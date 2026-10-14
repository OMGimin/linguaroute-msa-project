package com.lecture.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.payment.entity.OutboxEvent;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.entity.Subscription;
import com.lecture.payment.event.SubscriptionEventType;
import com.lecture.payment.event.SubscriptionEvents;
import com.lecture.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void savePaymentCompleted(Subscription subscription, Payment payment) {
        String eventId = UUID.randomUUID().toString();
        var payload = new SubscriptionEvents.PaymentCompletedEvent(
                eventId,
                SubscriptionEventType.PaymentCompleted,
                now(),
                subscription.getCompanyId(),
                subscription.getId(),
                payment.getId(),
                subscription.getPlanPrice().getId(),
                subscription.getPlanPrice().getSeatLimit(),
                toOffset(subscription.getCurrentPeriodEnd())
        );
        save(eventId, subscription.getId(), SubscriptionEventType.PaymentCompleted, payload);
    }

    public void savePaymentFailed(Long companyId, Long subscriptionId, Payment payment, Long planPriceId,
                                  String failureCode, String failureReason) {
        String eventId = UUID.randomUUID().toString();
        var payload = new SubscriptionEvents.PaymentFailedEvent(
                eventId,
                SubscriptionEventType.PaymentFailed,
                now(),
                companyId,
                subscriptionId,
                payment.getId(),
                planPriceId,
                failureCode,
                failureReason
        );
        save(eventId, subscriptionId == null ? payment.getId() : subscriptionId, SubscriptionEventType.PaymentFailed, payload);
    }

    public void saveSubscriptionCanceled(Subscription subscription, LocalDateTime canceledAt) {
        String eventId = UUID.randomUUID().toString();
        var payload = new SubscriptionEvents.SubscriptionCanceledEvent(
                eventId,
                SubscriptionEventType.SubscriptionCanceled,
                now(),
                subscription.getCompanyId(),
                subscription.getId(),
                toOffset(canceledAt),
                toOffset(subscription.getCurrentPeriodEnd())
        );
        save(eventId, subscription.getId(), SubscriptionEventType.SubscriptionCanceled, payload);
    }

    public void saveSubscriptionExpired(Subscription subscription, LocalDateTime expiredAt) {
        String eventId = UUID.randomUUID().toString();
        var payload = new SubscriptionEvents.SubscriptionExpiredEvent(
                eventId,
                SubscriptionEventType.SubscriptionExpired,
                now(),
                subscription.getCompanyId(),
                subscription.getId(),
                toOffset(expiredAt)
        );
        save(eventId, subscription.getId(), SubscriptionEventType.SubscriptionExpired, payload);
    }

    public void saveSubscriptionRenewed(Subscription subscription, Payment payment) {
        String eventId = UUID.randomUUID().toString();
        var payload = new SubscriptionEvents.SubscriptionRenewedEvent(
                eventId,
                SubscriptionEventType.SubscriptionRenewed,
                now(),
                subscription.getCompanyId(),
                subscription.getId(),
                payment.getId(),
                subscription.getPlanPrice().getId(),
                subscription.getPlanPrice().getSeatLimit(),
                toOffset(subscription.getCurrentPeriodStart()),
                toOffset(subscription.getCurrentPeriodEnd()),
                toOffset(subscription.getNextBillingAt())
        );
        save(eventId, subscription.getId(), SubscriptionEventType.SubscriptionRenewed, payload);
    }

    private void save(String eventId, Long aggregateId, SubscriptionEventType eventType, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.pending(
                    eventId,
                    aggregateId,
                    eventType.name(),
                    objectMapper.writeValueAsString(payload)
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("구독 이벤트 payload 직렬화에 실패했습니다.", e);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(SERVICE_ZONE_ID);
    }

    private OffsetDateTime toOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(SERVICE_ZONE_ID).toOffsetDateTime();
    }
}
