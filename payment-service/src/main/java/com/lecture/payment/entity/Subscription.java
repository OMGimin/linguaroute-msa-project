package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_price_id", nullable = false)
    private PlanPrice planPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private boolean autoRenew = true;

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Subscription active(Long companyId, PlanPrice planPrice, LocalDateTime start, LocalDateTime end) {
        return Subscription.builder()
                .companyId(companyId)
                .planPrice(planPrice)
                .status(Status.ACTIVE)
                .autoRenew(true)
                .currentPeriodStart(start)
                .currentPeriodEnd(end)
                .nextBillingAt(end)
                .build();
    }

    public static Subscription pending(Long companyId, PlanPrice planPrice, LocalDateTime requestedAt) {
        return Subscription.builder()
                .companyId(companyId)
                .planPrice(planPrice)
                .status(Status.PENDING)
                .autoRenew(false)
                .currentPeriodStart(requestedAt)
                .currentPeriodEnd(requestedAt)
                .build();
    }

    public void activate(LocalDateTime start, LocalDateTime end) {
        this.status = Status.ACTIVE;
        this.autoRenew = true;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
        this.nextBillingAt = end;
    }

    public void cancel(LocalDateTime canceledAt) {
        this.autoRenew = false;
        this.canceledAt = canceledAt;
        this.nextBillingAt = null;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = Status.EXPIRED;
        this.currentPeriodEnd = expiredAt;
        this.nextBillingAt = null;
    }

    public void renew(PlanPrice planPrice, LocalDateTime start, LocalDateTime end) {
        this.planPrice = planPrice;
        this.status = Status.ACTIVE;
        this.autoRenew = true;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
        this.nextBillingAt = end;
        this.canceledAt = null;
    }

    public enum Status {
        PENDING,
        ACTIVE,
        CANCELED,
        EXPIRED
    }
}
