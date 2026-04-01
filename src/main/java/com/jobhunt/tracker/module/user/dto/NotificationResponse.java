package com.jobhunt.tracker.module.user.dto;

public record NotificationResponse(
        Boolean reminderEnabled,
        Integer reminderAfterDays,
        Boolean emailNotifications,
        String timezone,
        String language
) {
}