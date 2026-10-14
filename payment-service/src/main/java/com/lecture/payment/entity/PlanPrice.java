package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "plan_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plan_price_cycle",
                columnNames = {"plan_id", "billing_cycle"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PlanPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "seat_limit", nullable = false)
    private Integer seatLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    void assignPlan(Plan plan) {
        this.plan = plan;
    }

    public enum BillingCycle {
        MONTHLY,
        YEARLY
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
