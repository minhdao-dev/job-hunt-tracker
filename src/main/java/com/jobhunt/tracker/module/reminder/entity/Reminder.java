package com.jobhunt.tracker.module.reminder.entity;

import com.jobhunt.tracker.common.entity.BaseEntity;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminders", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobApplication job;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime remindAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSent = false;
}