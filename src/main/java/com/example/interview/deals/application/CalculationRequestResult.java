package com.example.interview.deals.application;

import com.example.interview.deals.persistence.DealEntity;
import org.springframework.http.HttpStatus;

public record CalculationRequestResult(DealEntity deal, HttpStatus httpStatus) {
}
