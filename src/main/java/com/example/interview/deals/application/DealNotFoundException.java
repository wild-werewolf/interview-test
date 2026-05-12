package com.example.interview.deals.application;

public class DealNotFoundException extends RuntimeException {

    public DealNotFoundException(Long dealId) {
        super("Deal %d was not found".formatted(dealId));
    }
}
