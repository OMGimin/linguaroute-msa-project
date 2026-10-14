package com.lecture.enrollment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                // [변경] 사용자별 동일 강의 중복 신청 방지
                @UniqueConstraint(
                        name = "uk_enrollment_user_course",
                        columnNames = {"user_id", "course_id"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [추가] user-service 기업 ID의 논리 참조값
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // user-service 사용자 ID의 논리 참조값
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // course-service 강의 ID의 논리 참조값
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // [변경] 결제 상태가 아닌 수강·학습 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ENROLLED;

    // [추가] 서버에서 계산한 전체 필수 차시 진도율
    @Column(
            name = "progress_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    @Builder.Default
    private BigDecimal progressRate = BigDecimal.ZERO.setScale(2);

    // [추가] 수강신청 시각
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    // [추가] 최초 학습 시작 시각
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    // [추가] 수강 완료 시각
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // [추가] 수강신청 생성
    public static Enrollment create(
            Long companyId,
            Long userId,
            Long courseId,
            LocalDateTime enrolledAt
    ) {
        validateRequiredValue(companyId, "기업 ID");
        validateRequiredValue(userId, "사용자 ID");
        validateRequiredValue(courseId, "강의 ID");
        validateRequiredValue(enrolledAt, "수강신청 시각");

        return Enrollment.builder()
                .companyId(companyId)
                .userId(userId)
                .courseId(courseId)
                .status(Status.ENROLLED)
                .progressRate(BigDecimal.ZERO.setScale(2))
                .enrolledAt(enrolledAt)
                .build();
    }

    // [추가] 최초 차시 학습 시작
    public void startLearning(LocalDateTime startedAt) {
        validateRequiredValue(startedAt, "학습 시작 시각");

        if (status == Status.COMPLETED) {
            throw new IllegalStateException("이미 완료된 수강입니다.");
        }

        if (this.startedAt == null) {
            this.startedAt = startedAt;
        }

        this.status = Status.LEARNING;
    }

    // [추가] 서버에서 계산한 진도율 저장
    public void updateProgress(BigDecimal progressRate) {
        if (progressRate == null) {
            throw new IllegalArgumentException("진도율은 필수입니다.");
        }

        if (progressRate.compareTo(BigDecimal.ZERO) < 0
                || progressRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "진도율은 0 이상 100 이하이어야 합니다."
            );
        }

        this.progressRate = progressRate.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    // [추가] 모든 필수 차시 완료
    public void complete(LocalDateTime completedAt) {
        validateRequiredValue(completedAt, "수강 완료 시각");

        // 완료 요청이 바로 들어와도 학습 시작 시각은 남긴다.
        if (this.startedAt == null) {
            this.startedAt = completedAt;
        }

        this.status = Status.COMPLETED;
        this.progressRate = BigDecimal.valueOf(100)
                .setScale(2, RoundingMode.HALF_UP);

        // 반복 완료 요청 시 최초 완료 시각 유지
        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }
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

    // [변경] API와 ERD에 정의된 수강 상태
    public enum Status {
        ENROLLED,
        LEARNING,
        COMPLETED
    }
}