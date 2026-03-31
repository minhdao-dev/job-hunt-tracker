package com.jobhunt.tracker.module.job.entity;

import com.jobhunt.tracker.common.entity.BaseEntity;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "job_applications", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String position;

    @Column(length = 500)
    private String jobUrl;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate appliedDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source", columnDefinition = "app.job_source")
    @Builder.Default
    private JobSource source = JobSource.OTHER;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "app.job_status")
    @Builder.Default
    private JobStatus status = JobStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "priority", columnDefinition = "app.job_priority")
    @Builder.Default
    private JobPriority priority = JobPriority.MEDIUM;

    @Column
    private Long salaryMin;

    @Column
    private Long salaryMax;

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRemote = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}