package com.example.interview.deals.integration.pricing;

import java.math.BigDecimal;

public interface PricingClient {

    PricingResult getPrice(BigDecimal amount, String currency);
}
