package com.jobhunt.tracker.module.offer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateOfferRequest(

        @Min(value = 0, message = "Salary must be non-negative")
        Long salary,

        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        String benefits,

        LocalDate startDate,

        LocalDate expiredAt,

        String note
) {
}