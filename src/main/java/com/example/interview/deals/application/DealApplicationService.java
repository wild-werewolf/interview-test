package com.example.interview.deals.application;

import com.example.interview.deals.domain.DealStatus;
import com.example.interview.deals.generated.model.CreateDealRequest;
import com.example.interview.deals.persistence.DealEntity;
import com.example.interview.deals.persistence.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DealApplicationService {

    private final DealRepository dealRepository;
    private final Clock clock;

    @Transactional
    public DealEntity createDeal(CreateDealRequest request) {
        Instant now = clock.instant();
        DealEntity deal = DealEntity.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(DealStatus.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return dealRepository.save(deal);
    }

    @Transactional(readOnly = true)
    public DealEntity getDeal(Long dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new DealNotFoundException(dealId));
    }
}
