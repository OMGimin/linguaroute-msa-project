package com.lecture.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.subscription-events}")
    private String subscriptionEventsTopic;

    public SendResult<String, Object> publishSubscriptionEvent(String companyId, String payload) {
        try {
            Object event = objectMapper.readValue(payload, Object.class);
            SendResult<String, Object> result = kafkaTemplate
                    .send(subscriptionEventsTopic, companyId, event)
                    .get(10, TimeUnit.SECONDS);

            log.info("[Kafka Producer] 구독 이벤트 발행 성공 - topic: {}, key: {}, partition: {}, offset: {}",
                    subscriptionEventsTopic,
                    companyId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return result;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("구독 이벤트 payload JSON 형식이 올바르지 않습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("구독 이벤트 Kafka 발행에 실패했습니다.", e);
        }
    }
}
