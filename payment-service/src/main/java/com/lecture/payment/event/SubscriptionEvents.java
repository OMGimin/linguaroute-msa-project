package com.lecture.payment.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

public class SubscriptionEvents {

    private SubscriptionEvents() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentCompletedEvent(
            String eventId,
            SubscriptionEventType eventType,
            OffsetDateTime occurredAt,
            Long companyId,
            Long subscriptionId,
            Long paymentId,
            Long planPriceId,
            Integer seatLimit,
            OffsetDateTime currentPeriodEnd
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentFailedEvent(
            String eventId,
            SubscriptionEventType eventType,
            OffsetDateTime occurredAt,
            Long companyId,
            Long subscriptionId,
            Long paymentId,
            Long planPriceId,
            String failureCode,
            String failureReason
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionCanceledEvent(
            String eventId,
            SubscriptionEventType eventType,
            OffsetDateTime occurredAt,
            Long companyId,
            Long subscriptionId,
            OffsetDateTime canceledAt,
            OffsetDateTime effectiveAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionExpiredEvent(
            String eventId,
            SubscriptionEventType eventType,
            OffsetDateTime occurredAt,
            Long companyId,
            Long subscriptionId,
            OffsetDateTime expiredAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubscriptionRenewedEvent(
            String eventId,
            SubscriptionEventType eventType,
            OffsetDateTime occurredAt,
            Long companyId,
            Long subscriptionId,
            Long paymentId,
            Long planPriceId,
            Integer seatLimit,
            OffsetDateTime currentPeriodStart,
            OffsetDateTime currentPeriodEnd,
            OffsetDateTime nextBillingAt
    ) {
    }
}
