package com.example.interview.common.error;

import com.example.interview.deals.application.DealNotFoundException;
import com.example.interview.deals.application.InvalidDealStatusException;
import com.example.interview.deals.generated.model.ErrorResponse;
import com.example.interview.deals.integration.pricing.PriceNotFoundException;
import com.example.interview.deals.integration.pricing.PricingServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DealNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(DealNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("DEAL_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidDealStatusException.class)
    ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidDealStatusException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("INVALID_DEAL_STATUS", exception.getMessage()));
    }

    @ExceptionHandler(PriceNotFoundException.class)
    ResponseEntity<ErrorResponse> handlePriceNotFound(PriceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("PRICE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(PricingServiceUnavailableException.class)
    ResponseEntity<ErrorResponse> handlePricingUnavailable(PricingServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(error("PRICING_SERVICE_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_FAILED", "Request validation failed"));
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse()
                .code(code)
                .message(message);
    }
}
