package com.jobhunt.tracker.module.company.dto;

import com.jobhunt.tracker.module.company.entity.CompanySize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(

        @NotBlank(message = "Company name is required")
        @Size(max = 255, message = "Company name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Website must not exceed 500 characters")
        String website,

        @Size(max = 100, message = "Industry must not exceed 100 characters")
        String industry,

        CompanySize size,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,

        Boolean isOutsource,

        String notes
) {
}