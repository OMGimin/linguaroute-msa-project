package com.lecture.user.kafka;

import com.lecture.user.service.CompanyEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final CompanyEntitlementService companyEntitlementService;

    @KafkaListener(
            topics = "${kafka.topic.subscription-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleSubscriptionEvent(Map<String, Object> event) {
        log.info("[SubscriptionEventConsumer] 구독 이벤트 수신 - eventType: {}, eventId: {}",
                event.get("eventType"), event.get("eventId"));
        companyEntitlementService.processSubscriptionEvent(event);
    }
}
