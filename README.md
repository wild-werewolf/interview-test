# Deal Calculation Candidate Starter

Это самостоятельный Gradle-проект для практического интервью. Его можно отправить кандидату отдельным архивом.

## Цель

Вам нужно реализовать надежный расчет сделки через внешний `pricing-service`. Внешний вызов может падать, сервис может выключиться между этапами обработки, один и тот же запрос может прийти несколько раз. Нужно сделать так, чтобы сделка не рассчиталась дважды, задача не потерялась, а технические ошибки ретраились.

## Что уже готово

- Spring Boot project на Java 17;
- OpenAPI spec в `src/main/resources/openapi/deal-calculation-api.yaml`;
- generated DTO и `Api` interfaces через Gradle task `openApiGenerate`;
- Liquibase changelog;
- JPA entity;
- repositories;
- controllers, которые реализуют generated interfaces;
- базовый create/get deal flow;
- WebClient configuration;
- WireMock/Testcontainers integration tests;
- Docker Compose с PostgreSQL.

Generated-код лежит в `build/generated/openapi` и не редактируется руками.

## Что нужно реализовать

Обязательные TODO:

- `DealCalculationApplicationService.requestCalculation(...)`
- `DealCalculationTaskProcessor.processNext()`
- `DealCalculationTaskTransactionService.acquireTask(...)`
- `DealCalculationTaskTransactionService.completeSuccess(...)`
- `DealCalculationTaskTransactionService.handlePriceNotFound(...)`
- `DealCalculationTaskTransactionService.handleTechnicalFailure(...)`
- `PricingWebClient.getPrice(...)`

Важно:

- не держите DB-транзакцию во время вызова `pricing-service`;
- используйте короткие транзакции;
- проверяйте `processingToken` при завершении task;
- не ретрайте `404`;
- ретрайте только технические ошибки;
- не создавайте две активные task-записи для одной deal.

## Запуск тестов

```bash
./gradlew test
```

Если после распаковки архива на Linux/macOS у скрипта потерялся executable bit, выполните `bash gradlew test` или `chmod +x gradlew`.

Если на Windows с Docker Desktop Testcontainers не видит Docker daemon, проверьте активный context `docker context ls`. Для context `desktop-linux` обычно помогает:

```powershell
$env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
```

На Windows:

```bat
gradlew.bat test
```

Часть тестов должна падать до реализации TODO. Начните с:

1. `requestCalculation_shouldCreateTaskAndMoveDealToCalculationRequested`
2. `requestCalculation_shouldBeIdempotent`
3. `processNext_shouldCalculateDealSuccessfully`
4. `processNext_whenPricingReturns404_shouldMoveDealToPriceNotFound`
5. `processNext_whenPricingReturns500_shouldRetryTask`
6. `processNext_whenMaxAttemptsExceeded_shouldFailDeal`
7. `processNext_shouldNotCompleteWithWrongProcessingToken`
8. `processNext_shouldNotCreateDuplicateActiveTasks`
9. `processNext_shouldRecoverExpiredProcessingTask`

## Ручной запуск

```bash
docker compose up -d
./gradlew bootRun
```

Endpoints:

- `POST /deals`
- `GET /deals/{id}`
- `POST /deals/{id}/calculate`
- `POST /internal/tasks/deal-calculation/process-next`
