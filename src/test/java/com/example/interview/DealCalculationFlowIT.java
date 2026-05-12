package com.example.interview;

import com.example.interview.deals.domain.DealStatus;
import com.example.interview.deals.domain.TaskStatus;
import com.example.interview.deals.generated.model.CreateDealRequest;
import com.example.interview.deals.generated.model.DealResponse;
import com.example.interview.deals.generated.model.TaskProcessingResponse;
import com.example.interview.deals.generated.model.TaskProcessingStatus;
import com.example.interview.deals.persistence.DealCalculationTaskEntity;
import com.example.interview.deals.persistence.DealCalculationTaskRepository;
import com.example.interview.deals.persistence.DealEntity;
import com.example.interview.deals.persistence.DealRepository;
import com.example.interview.deals.worker.AcquiredDealCalculationTask;
import com.example.interview.deals.worker.DealCalculationTaskTransactionService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "deal-calculation.scheduler.enabled=false"
)
class DealCalculationFlowIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("deal_calculation")
            .withUsername("deal")
            .withPassword("deal");

    static final WireMockServer pricingService = new WireMockServer(options().dynamicPort());

    static {
        pricingService.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("pricing.base-url", pricingService::baseUrl);
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    DealRepository dealRepository;

    @Autowired
    DealCalculationTaskRepository taskRepository;

    @Autowired
    DealCalculationTaskTransactionService transactionService;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        dealRepository.deleteAll();
        pricingService.resetAll();
        clock.setInstant(Instant.parse("2026-05-05T10:00:00Z"));
    }

    @AfterAll
    static void stopWireMock() {
        pricingService.stop();
    }

    @Test
    void createDeal_shouldCreateDealInNewStatus() {
        DealResponse deal = createDeal();

        assertThat(deal.getId()).isNotNull();
        assertThat(deal.getUserId()).isEqualTo(100L);
        assertThat(deal.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(deal.getCurrency()).isEqualTo("USD");
        assertThat(deal.getPrice()).isNull();
        assertThat(deal.getStatus()).isEqualTo(com.example.interview.deals.generated.model.DealStatus.NEW);
    }

    @Test
    void requestCalculation_shouldCreateTaskAndMoveDealToCalculationRequested() {
        DealResponse deal = createDeal();

        ResponseEntity<DealResponse> response = rest.postForEntity(
                "/deals/%d/calculate".formatted(deal.getId()),
                null,
                DealResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus())
                .isEqualTo(com.example.interview.deals.generated.model.DealStatus.CALCULATION_REQUESTED);

        List<DealCalculationTaskEntity> tasks = taskRepository.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getDealId()).isEqualTo(deal.getId());
        assertThat(tasks.get(0).getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void requestCalculation_shouldBeIdempotent() {
        DealResponse deal = createDeal();

        rest.postForEntity("/deals/%d/calculate".formatted(deal.getId()), null, DealResponse.class);
        ResponseEntity<DealResponse> second = rest.postForEntity(
                "/deals/%d/calculate".formatted(deal.getId()),
                null,
                DealResponse.class
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(taskRepository.findAll()).hasSize(1);
    }

    @Test
    void processNext_shouldCalculateDealSuccessfully() {
        DealResponse deal = createDealAndRequestCalculation();
        stubSuccessfulPrice();

        TaskProcessingResponse processing = processNext();

        assertThat(processing.getStatus()).isEqualTo(TaskProcessingStatus.SUCCESS);
        DealEntity storedDeal = dealRepository.findById(deal.getId()).orElseThrow();
        assertThat(storedDeal.getStatus()).isEqualTo(DealStatus.CALCULATED);
        assertThat(storedDeal.getPrice()).isEqualByComparingTo("123.45");
        assertThat(taskRepository.findAll().get(0).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void processNext_whenPricingReturns404_shouldMoveDealToPriceNotFound() {
        DealResponse deal = createDealAndRequestCalculation();
        pricingService.stubFor(get(urlPathEqualTo("/prices"))
                .willReturn(aResponse().withStatus(404)));

        TaskProcessingResponse processing = processNext();

        assertThat(processing.getStatus()).isEqualTo(TaskProcessingStatus.PRICE_NOT_FOUND);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.PRICE_NOT_FOUND);
        assertThat(taskRepository.findAll().get(0).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void processNext_whenPricingReturns500_shouldRetryTask() {
        DealResponse deal = createDealAndRequestCalculation();
        pricingService.stubFor(get(urlPathEqualTo("/prices"))
                .willReturn(aResponse().withStatus(500).withBody("temporary failure")));

        TaskProcessingResponse processing = processNext();

        assertThat(processing.getStatus()).isEqualTo(TaskProcessingStatus.RETRY_SCHEDULED);
        DealCalculationTaskEntity task = taskRepository.findAll().get(0);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(task.getAttempt()).isEqualTo(1);
        assertThat(task.getAvailableAt()).isEqualTo(Instant.parse("2026-05-05T10:00:10Z"));
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.CALCULATION_REQUESTED);
    }

    @Test
    void processNext_whenMaxAttemptsExceeded_shouldFailDeal() {
        DealResponse deal = createDealAndRequestCalculation();
        DealCalculationTaskEntity task = taskRepository.findAll().get(0);
        task.setAttempt(2);
        taskRepository.save(task);
        pricingService.stubFor(get(urlPathEqualTo("/prices"))
                .willReturn(aResponse().withStatus(500)));

        TaskProcessingResponse processing = processNext();

        assertThat(processing.getStatus()).isEqualTo(TaskProcessingStatus.FAILED);
        assertThat(taskRepository.findAll().get(0).getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.FAILED);
    }

    @Test
    void processNext_shouldNotCompleteWithWrongProcessingToken() {
        DealResponse deal = createDealAndRequestCalculation();
        AcquiredDealCalculationTask acquired = transactionService.acquireTask().orElseThrow();

        TaskProcessingStatus status = transactionService.completeSuccess(
                acquired.taskId(),
                UUID.randomUUID(),
                new BigDecimal("999.99")
        );

        assertThat(status).isEqualTo(TaskProcessingStatus.STALE_ATTEMPT);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.PROCESSING);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getPrice()).isNull();
        assertThat(taskRepository.findById(acquired.taskId()).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void processNext_shouldNotCreateDuplicateActiveTasks() throws Exception {
        DealResponse deal = createDeal();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ResponseEntity<DealResponse>> first = executor.submit(() -> calculateWhenReleased(deal.getId(), start));
            Future<ResponseEntity<DealResponse>> second = executor.submit(() -> calculateWhenReleased(deal.getId(), start));

            start.countDown();

            assertThat(first.get().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(second.get().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(taskRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void processNext_shouldRecoverExpiredProcessingTask() {
        DealResponse deal = createDealAndRequestCalculation();
        Optional<AcquiredDealCalculationTask> acquired = transactionService.acquireTask();
        assertThat(acquired).isPresent();
        clock.advanceSeconds(31);
        stubSuccessfulPrice();

        TaskProcessingResponse processing = processNext();

        assertThat(processing.getStatus()).isEqualTo(TaskProcessingStatus.SUCCESS);
        assertThat(dealRepository.findById(deal.getId()).orElseThrow().getStatus())
                .isEqualTo(DealStatus.CALCULATED);
        assertThat(taskRepository.findAll().get(0).getAttempt()).isEqualTo(2);
    }

    private ResponseEntity<DealResponse> calculateWhenReleased(Long dealId, CountDownLatch start) throws InterruptedException {
        start.await();
        return rest.postForEntity("/deals/%d/calculate".formatted(dealId), null, DealResponse.class);
    }

    private DealResponse createDealAndRequestCalculation() {
        DealResponse deal = createDeal();
        ResponseEntity<DealResponse> response = rest.postForEntity(
                "/deals/%d/calculate".formatted(deal.getId()),
                null,
                DealResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return deal;
    }

    private DealResponse createDeal() {
        CreateDealRequest request = new CreateDealRequest()
                .userId(100L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD");

        ResponseEntity<DealResponse> response = rest.postForEntity("/deals", request, DealResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TaskProcessingResponse processNext() {
        ResponseEntity<TaskProcessingResponse> response = rest.postForEntity(
                "/internal/tasks/deal-calculation/process-next",
                null,
                TaskProcessingResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void stubSuccessfulPrice() {
        pricingService.stubFor(get(urlPathEqualTo("/prices"))
                .willReturn(okJson("{\"price\":123.45}")));
    }

    @TestConfiguration
    static class ClockTestConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-05-05T10:00:00Z");

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
