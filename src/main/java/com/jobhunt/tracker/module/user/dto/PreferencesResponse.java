package com.jobhunt.tracker.module.user.dto;

public record PreferencesResponse(
        String targetRole,
        Integer targetSalaryMin,
        Integer targetSalaryMax,
        String preferredLocation,
        com.jobhunt.tracker.module.user.entity.WorkType workType
) {
}