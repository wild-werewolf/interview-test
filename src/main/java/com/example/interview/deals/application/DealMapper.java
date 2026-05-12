package com.example.interview.deals.application;

import com.example.interview.deals.generated.model.DealResponse;
import com.example.interview.deals.persistence.DealEntity;
import org.springframework.stereotype.Component;

@Component
public class DealMapper {

    public DealResponse toResponse(DealEntity deal) {
        return new DealResponse()
                .id(deal.getId())
                .userId(deal.getUserId())
                .amount(deal.getAmount())
                .currency(deal.getCurrency())
                .price(deal.getPrice())
                .status(com.example.interview.deals.generated.model.DealStatus.fromValue(deal.getStatus().name()));
    }
}
