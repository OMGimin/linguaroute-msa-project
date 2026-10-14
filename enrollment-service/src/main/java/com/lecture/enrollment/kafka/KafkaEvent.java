package com.lecture.enrollment.kafka;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Kafka 이벤트 메시지 DTO
 */
public class KafkaEvent {

    /**
     * Enrollment Service → Recommend Service
     * 모든 필수 차시를 완료해 수강 상태가 COMPLETED가 된 이벤트
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentCompletedEvent {
        private String eventId;
        private String eventType;
        private LocalDateTime occurredAt;
        private Long enrollmentId;
        private Long userId;
        private Long courseId;
    }
}
