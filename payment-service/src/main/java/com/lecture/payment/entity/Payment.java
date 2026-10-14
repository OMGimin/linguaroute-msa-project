package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_company_idempotency_key",
                columnNames = {"company_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "provider_payment_id", unique = true)
    private String providerPaymentId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    public static Payment pending(Long companyId, String idempotencyKey, Long amount, String currency, LocalDateTime requestedAt) {
        return Payment.builder()
                .companyId(companyId)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .status(Status.PENDING)
                .requestedAt(requestedAt)
                .build();
    }

    public void succeed(Subscription subscription, String providerPaymentId, LocalDateTime paidAt) {
        this.subscription = subscription;
        this.status = Status.SUCCESS;
        this.providerPaymentId = providerPaymentId;
        this.paidAt = paidAt;
    }

    public void fail(String failureReason, LocalDateTime failedAt) {
        this.status = Status.FAILED;
        this.failureReason = failureReason;
        this.failedAt = failedAt;
    }
}
