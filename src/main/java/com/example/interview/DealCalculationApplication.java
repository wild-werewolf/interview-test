package com.example.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DealCalculationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealCalculationApplication.class, args);
    }
}
