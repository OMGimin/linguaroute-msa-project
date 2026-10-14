package com.lecture.enrollment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [추가]
 * 직원의 차시별 학습 진행 상태를 저장하는 Entity.
 *
 * 차시 제목, 내용, 순서, 필수 여부는 course-service가 관리한다.
 * enrollment-service는 lessonId만 논리적으로 참조하여
 * 직원이 해당 차시를 시작·완료했는지 저장한다.
 */
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                // 같은 수강신청의 같은 차시는 하나의 진행 기록만 가질 수 있다.
                @UniqueConstraint(
                        name = "uk_lesson_progress_enrollment_lesson",
                        columnNames = {"enrollment_id", "lesson_id"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * [추가]
     * 같은 enrollment-service가 소유하는 Enrollment와의 관계.
     *
     * 하나의 Enrollment는 여러 LessonProgress를 가질 수 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "enrollment_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_lesson_progress_enrollment"
            )
    )
    private Enrollment enrollment;

    /**
     * [추가]
     * course-service가 소유하는 차시 ID의 논리 참조값.
     * course-service의 Lesson 테이블과 실제 외래키를 만들지 않는다.
     */
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    /**
     * [추가] 차시별 학습 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NOT_STARTED;

    /**
     * [추가] 차시 최초 시작 시각
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * [추가] 차시 최초 완료 시각
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * [추가] 차시 진행 상태가 마지막으로 변경된 시각
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * [추가]
     * 새로운 차시 진행 기록을 생성한다.
     *
     * 최초 상태:
     * status = NOT_STARTED
     * startedAt = null
     * completedAt = null
     */
    public static LessonProgress create(
            Enrollment enrollment,
            Long lessonId
    ) {
        validateRequiredValue(enrollment, "수강 정보");
        validateRequiredValue(lessonId, "차시 ID");

        return LessonProgress.builder()
                .enrollment(enrollment)
                .lessonId(lessonId)
                .status(Status.NOT_STARTED)
                .build();
    }

    /**
     * [추가]
     * 차시 학습을 시작한다.
     *
     * 같은 시작 요청이 반복되어도 최초 startedAt은 유지한다.
     * 이미 완료한 차시에는 상태와 시각을 다시 변경하지 않는다.
     */
    public void start(LocalDateTime startedAt) {
        validateRequiredValue(startedAt, "차시 시작 시각");

        if (status == Status.COMPLETED) {
            return;
        }

        if (this.startedAt == null) {
            this.startedAt = startedAt;
        }

        this.status = Status.LEARNING;
    }

    /**
     * [추가]
     * 차시 학습을 완료한다.
     *
     * 시작 API 호출 없이 완료 요청이 들어오면
     * 완료 시각을 시작 시각으로도 저장한다.
     *
     * 같은 완료 요청이 반복되어도 최초 완료 시각은 유지한다.
     */
    public void complete(LocalDateTime completedAt) {
        validateRequiredValue(completedAt, "차시 완료 시각");

        if (this.startedAt == null) {
            this.startedAt = completedAt;
        }

        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }

        this.status = Status.COMPLETED;
    }

    private static void validateRequiredValue(
            Object value,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + "는 필수입니다."
            );
        }
    }

    /**
     * [추가]
     * 차시별 학습 상태.
     *
     * NOT_STARTED: 아직 학습하지 않음
     * LEARNING: 학습 시작
     * COMPLETED: 학습 완료
     */
    public enum Status {
        NOT_STARTED,
        LEARNING,
        COMPLETED
    }
}