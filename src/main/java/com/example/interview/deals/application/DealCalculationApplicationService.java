package com.example.interview.deals.application;

import com.example.interview.deals.persistence.DealCalculationTaskRepository;
import com.example.interview.deals.persistence.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DealCalculationApplicationService {

    private final DealRepository dealRepository;
    private final DealCalculationTaskRepository taskRepository;

    @Transactional
    public CalculationRequestResult requestCalculation(Long dealId) {
        // TODO candidate: implement idempotent calculation request.
        // Requirements:
        // - lock the deal in a short transaction;
        // - create one active task only when deal is NEW;
        // - return 202 for existing active calculation;
        // - return 200 for already CALCULATED;
        // - return 409 for FAILED or PRICE_NOT_FOUND.
        return dealRepository.findById(dealId)
                .map(deal -> new CalculationRequestResult(deal, HttpStatus.ACCEPTED))
                .orElseThrow(() -> new DealNotFoundException(dealId));
    }
}
