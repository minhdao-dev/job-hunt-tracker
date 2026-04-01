package com.jobhunt.tracker.module.offer.entity;

import com.jobhunt.tracker.common.entity.BaseEntity;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "offers", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private JobApplication job;

    @Column
    private Long salary;

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String benefits;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate expiredAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "decision", columnDefinition = "app.offer_decision")
    @Builder.Default
    private OfferDecision decision = OfferDecision.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;
}