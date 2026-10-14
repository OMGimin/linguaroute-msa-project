package com.lecture.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Situation situation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Language {
        ENGLISH, JAPANESE, CHINESE
    }

    public enum Situation {
        CUSTOMER_MEETING, PRESENTATION, EMAIL, BUSINESS_TRIP, DAILY_CONVERSATION
    }

    public enum Level {
        BEGINNER, ELEMENTARY, INTERMEDIATE, ADVANCED
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    public void update(
            String title,
            String description,
            Language language,
            Situation situation,
            Level level) {
        this.title = title;
        this.description = description;
        this.language = language;
        this.situation = situation;
        this.level = level;
    }

    public void changeStatus(Status status) {
        this.status = status;
    }

}
