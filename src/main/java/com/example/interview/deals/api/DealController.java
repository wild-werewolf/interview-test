package com.example.interview.deals.api;

import com.example.interview.deals.application.CalculationRequestResult;
import com.example.interview.deals.application.DealApplicationService;
import com.example.interview.deals.application.DealCalculationApplicationService;
import com.example.interview.deals.application.DealMapper;
import com.example.interview.deals.generated.api.DealsApi;
import com.example.interview.deals.generated.model.CreateDealRequest;
import com.example.interview.deals.generated.model.DealResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DealController implements DealsApi {

    private final DealApplicationService dealApplicationService;
    private final DealCalculationApplicationService calculationApplicationService;
    private final DealMapper dealMapper;

    @Override
    public ResponseEntity<DealResponse> createDeal(CreateDealRequest createDealRequest) {
        DealResponse response = dealMapper.toResponse(dealApplicationService.createDeal(createDealRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<DealResponse> getDeal(Long id) {
        return ResponseEntity.ok(dealMapper.toResponse(dealApplicationService.getDeal(id)));
    }

    @Override
    public ResponseEntity<DealResponse> requestDealCalculation(Long id) {
        CalculationRequestResult result = calculationApplicationService.requestCalculation(id);
        return ResponseEntity.status(result.httpStatus()).body(dealMapper.toResponse(result.deal()));
    }
}
