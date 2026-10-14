package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static OutboxEvent pending(String eventId, Long aggregateId, String eventType, String payload) {
        return OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("Subscription")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(Status.PENDING)
                .build();
    }

    public void markPublished(LocalDateTime publishedAt) {
        this.status = Status.PUBLISHED;
        this.publishedAt = publishedAt;
    }

    public enum Status {
        PENDING,
        PUBLISHED
    }
}
