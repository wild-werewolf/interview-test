package com.example.interview.deals.integration.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
public class PricingWebClient implements PricingClient {

    private final WebClient webClient;

    public PricingWebClient(WebClient.Builder webClientBuilder, @Value("${pricing.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public PricingResult getPrice(BigDecimal amount, String currency) {
        // TODO candidate: implement WebClient call.
        // Requirements:
        // - GET /prices with amount and currency query params;
        // - map successful JSON response to PricingResult;
        // - convert 404 to PriceNotFoundException;
        // - convert 5xx, timeout and connection failures to PricingServiceUnavailableException;
        // - do not retry 404.
        throw new UnsupportedOperationException("TODO implement pricing WebClient");
    }
}
