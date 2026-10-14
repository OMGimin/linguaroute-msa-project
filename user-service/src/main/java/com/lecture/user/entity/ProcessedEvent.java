package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_events_event_id", columnNames = "event_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ProcessedEvent create(
            String eventId,
            String eventType,
            LocalDateTime occurredAt,
            LocalDateTime processedAt
    ) {
        return new ProcessedEvent(null, eventId, eventType, occurredAt, processedAt, null);
    }
}
