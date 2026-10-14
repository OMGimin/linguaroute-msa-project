package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_entitlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CompanyEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_status", nullable = false, length = 20)
    private Status entitlementStatus;

    @Column(name = "seat_limit", nullable = false)
    private Integer seatLimit;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CompanyEntitlement active(
            Company company,
            Long subscriptionId,
            Integer seatLimit,
            LocalDateTime currentPeriodEnd,
            boolean autoRenew
    ) {
        return CompanyEntitlement.builder()
                .company(company)
                .subscriptionId(subscriptionId)
                .entitlementStatus(Status.ACTIVE)
                .seatLimit(seatLimit)
                .currentPeriodEnd(currentPeriodEnd)
                .autoRenew(autoRenew)
                .build();
    }

    public void activate(
            Long subscriptionId,
            Integer seatLimit,
            LocalDateTime currentPeriodEnd,
            boolean autoRenew
    ) {
        this.subscriptionId = subscriptionId;
        this.entitlementStatus = Status.ACTIVE;
        this.seatLimit = seatLimit;
        this.currentPeriodEnd = currentPeriodEnd;
        this.autoRenew = autoRenew;
    }

    public void cancelAutoRenew(LocalDateTime effectiveAt) {
        this.autoRenew = false;
        if (effectiveAt != null) {
            this.currentPeriodEnd = effectiveAt;
        }
    }

    public void expire(LocalDateTime expiredAt) {
        this.entitlementStatus = Status.EXPIRED;
        this.autoRenew = false;
        if (expiredAt != null) {
            this.currentPeriodEnd = expiredAt;
        }
    }

    public enum Status {
        ACTIVE,
        EXPIRED
    }
}
