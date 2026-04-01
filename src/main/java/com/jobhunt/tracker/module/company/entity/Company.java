package com.jobhunt.tracker.module.company.entity;

import com.jobhunt.tracker.common.entity.BaseEntity;
import com.jobhunt.tracker.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "companies", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String website;

    @Column(length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "size", columnDefinition = "app.company_size")
    @Builder.Default
    private CompanySize size = CompanySize.UNKNOWN;

    @Column
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isOutsource = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}