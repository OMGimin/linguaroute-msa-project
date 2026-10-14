package com.lecture.course.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons", uniqueConstraints = @UniqueConstraint(name = "uk_lesson_course_sequence", columnNames = {"course_id", "sequence_no"}))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id") private Course course;
    @Column(nullable = false) private String title;
    @Column(name = "content_url", nullable = false) private String contentUrl;
    @Column(name = "sequence_no", nullable = false) private Integer sequence;
    @Column(nullable = false) @Builder.Default private boolean required = true;
    @Column(name = "duration_seconds", nullable = false) private Integer durationSeconds;
}
