package com.example.interview.deals.application;

import com.example.interview.deals.domain.DealStatus;

public class InvalidDealStatusException extends RuntimeException {

    public InvalidDealStatusException(Long dealId, DealStatus status) {
        super("Deal %d cannot be calculated from status %s".formatted(dealId, status));
    }
}
