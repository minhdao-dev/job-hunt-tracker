package com.jobhunt.tracker.module.user.entity;

import com.jobhunt.tracker.common.entity.BaseEntity;
import com.jobhunt.tracker.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_settings", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 300)
    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean reminderEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer reminderAfterDays = 7;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(length = 10)
    @Builder.Default
    private String language = "vi";

    @Column(length = 100)
    private String targetRole;

    @Column
    private Integer targetSalaryMin;

    @Column
    private Integer targetSalaryMax;

    @Column(length = 200)
    private String preferredLocation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "work_type", columnDefinition = "app.work_type")
    private WorkType workType;
}