package com.lecture.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.payment.entity.OutboxEvent;
import com.lecture.payment.kafka.PaymentKafkaProducer;
import com.lecture.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentKafkaProducer paymentKafkaProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            String companyId = extractCompanyId(event.getPayload());
            paymentKafkaProducer.publishSubscriptionEvent(companyId, event.getPayload());
            event.markPublished(LocalDateTime.now());
            log.info("[Outbox] 구독 이벤트 발행 완료 - eventId: {}, eventType: {}, aggregateId: {}",
                    event.getEventId(), event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            log.error("[Outbox] 구독 이벤트 발행 실패 - eventId: {}, eventType: {}, error: {}",
                    event.getEventId(), event.getEventType(), e.getMessage(), e);
        }
    }

    private String extractCompanyId(String payload) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(payload);
        JsonNode companyId = jsonNode.get("companyId");
        if (companyId == null || companyId.isNull()) {
            throw new IllegalArgumentException("구독 이벤트 payload에 companyId가 없습니다.");
        }
        return companyId.asText();
    }
}
