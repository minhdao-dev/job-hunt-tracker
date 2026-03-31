package com.jobhunt.tracker.module.reminder.service;

import com.jobhunt.tracker.module.reminder.dto.*;

import java.util.List;
import java.util.UUID;

public interface ReminderService {

    ReminderResponse create(UUID userId, UUID jobId, CreateReminderRequest request);

    List<ReminderResponse> getAll(UUID userId, UUID jobId);

    ReminderResponse update(UUID userId, UUID jobId, UUID reminderId, UpdateReminderRequest request);

    void delete(UUID userId, UUID jobId, UUID reminderId);
}