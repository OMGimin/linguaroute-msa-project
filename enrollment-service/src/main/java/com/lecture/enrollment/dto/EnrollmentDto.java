package com.lecture.enrollment.dto;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.LessonProgress;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public class EnrollmentDto {

    /**
     * API 날짜 형식에 사용할 서비스 시간대.
     */
    private static final ZoneId SERVICE_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private EnrollmentDto() {
    }

    /**
     * [유지] 수강신청 요청
     *
     * POST /api/enrollments
     *
     * 요청:
     * {
     *   "courseId": 12
     * }
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollRequest {

        @NotNull(message = "강의 ID는 필수입니다.")
        private Long courseId;
    }

    /**
     * [변경] 수강신청 생성 응답
     *
     * 응답 필드는 API 명세의 ENROLL-01과 일치한다.
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class EnrollResponse {

        private Long enrollmentId;
        private Long courseId;
        private Enrollment.Status status;
        private BigDecimal progressRate;

        public static EnrollResponse from(
                Enrollment enrollment
        ) {
            return EnrollResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .courseId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .progressRate(enrollment.getProgressRate())
                    .build();
        }
    }

    /**
     * [추가] 직원의 수강 목록 응답
     *
     * GET /api/enrollments/me
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class EnrollmentSummaryResponse {

        private Long enrollmentId;
        private Long courseId;
        private Enrollment.Status status;
        private BigDecimal progressRate;
        private OffsetDateTime enrolledAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;

        public static EnrollmentSummaryResponse from(
                Enrollment enrollment
        ) {
            return EnrollmentSummaryResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .courseId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .progressRate(enrollment.getProgressRate())
                    .enrolledAt(toOffsetDateTime(
                            enrollment.getEnrolledAt()
                    ))
                    .startedAt(toOffsetDateTime(
                            enrollment.getStartedAt()
                    ))
                    .completedAt(toOffsetDateTime(
                            enrollment.getCompletedAt()
                    ))
                    .build();
        }
    }

    /**
     * [추가] 차시별 진행 상태 응답
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class LessonProgressResponse {

        private Long lessonId;
        private LessonProgress.Status status;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;

        public static LessonProgressResponse from(
                LessonProgress lessonProgress
        ) {
            return LessonProgressResponse.builder()
                    .lessonId(lessonProgress.getLessonId())
                    .status(lessonProgress.getStatus())
                    .startedAt(toOffsetDateTime(
                            lessonProgress.getStartedAt()
                    ))
                    .completedAt(toOffsetDateTime(
                            lessonProgress.getCompletedAt()
                    ))
                    .build();
        }
    }

    /**
     * [추가] 수강 상세·진도율 응답
     *
     * GET /api/enrollments/{enrollmentId}
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class EnrollmentDetailResponse {

        private Long enrollmentId;
        private Long courseId;
        private Enrollment.Status status;
        private BigDecimal progressRate;
        private OffsetDateTime enrolledAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private List<LessonProgressResponse> lessons;

        public static EnrollmentDetailResponse from(
                Enrollment enrollment,
                List<LessonProgress> lessonProgresses
        ) {
            List<LessonProgressResponse> lessons =
                    lessonProgresses.stream()
                            .map(LessonProgressResponse::from)
                            .toList();

            return EnrollmentDetailResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .courseId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .progressRate(enrollment.getProgressRate())
                    .enrolledAt(toOffsetDateTime(
                            enrollment.getEnrolledAt()
                    ))
                    .startedAt(toOffsetDateTime(
                            enrollment.getStartedAt()
                    ))
                    .completedAt(toOffsetDateTime(
                            enrollment.getCompletedAt()
                    ))
                    .lessons(lessons)
                    .build();
        }
    }

    /**
     * [추가] 차시 시작·완료 응답
     *
     * POST /api/enrollments/{enrollmentId}/lessons/{lessonId}/start
     * POST /api/enrollments/{enrollmentId}/lessons/{lessonId}/complete
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class LessonActionResponse {

        private Long enrollmentId;
        private Long lessonId;
        private LessonProgress.Status lessonStatus;
        private BigDecimal progressRate;
        private Enrollment.Status enrollmentStatus;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;

        public static LessonActionResponse from(
                Enrollment enrollment,
                LessonProgress lessonProgress
        ) {
            return LessonActionResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .lessonId(lessonProgress.getLessonId())
                    .lessonStatus(lessonProgress.getStatus())
                    .progressRate(enrollment.getProgressRate())
                    .enrollmentStatus(enrollment.getStatus())
                    .startedAt(toOffsetDateTime(
                            lessonProgress.getStartedAt()
                    ))
                    .completedAt(toOffsetDateTime(
                            lessonProgress.getCompletedAt()
                    ))
                    .build();
        }
    }

    /**
     * [추가] 기업 관리자·플랫폼 관리자 수강 조회 응답
     *
     * GET /api/companies/me/enrollments
     * GET /api/companies/me/enrollments/progress
     * GET /api/admin/enrollments
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ManagementEnrollmentResponse {

        private Long enrollmentId;
        private Long companyId;
        private Long userId;
        private Long courseId;
        private Enrollment.Status status;
        private BigDecimal progressRate;
        private OffsetDateTime enrolledAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;

        public static ManagementEnrollmentResponse from(
                Enrollment enrollment
        ) {
            return ManagementEnrollmentResponse.builder()
                    .enrollmentId(enrollment.getId())
                    .companyId(enrollment.getCompanyId())
                    .userId(enrollment.getUserId())
                    .courseId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .progressRate(enrollment.getProgressRate())
                    .enrolledAt(toOffsetDateTime(
                            enrollment.getEnrolledAt()
                    ))
                    .startedAt(toOffsetDateTime(
                            enrollment.getStartedAt()
                    ))
                    .completedAt(toOffsetDateTime(
                            enrollment.getCompletedAt()
                    ))
                    .build();
        }
    }

    /**
     * [추가] 공통 페이지 응답
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> {

        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public static <T> PageResponse<T> from(
                Page<T> result
        ) {
            return PageResponse.<T>builder()
                    .content(result.getContent())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        }
    }

    /**
     * [유지·변경]
     * 추천 서비스용 수강 이력 응답.
     *
     * [통합 확인 필요]
     * 추천 서비스가 기존 내부 API를 사용하는지 확인한다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentHistoryResponse {

        private Long userId;

        /**
         * 기존 activeCourseIds 이름은 현재 상태 모델과 맞지 않지만,
         * 추천 서비스 호환 여부가 확인될 때까지 유지한다.
         */
        private List<Long> activeCourseIds;
    }

    /**
     * [변경] 공통 성공 응답
     *
     * 기존 success와 message 필드를 제거하고
     * API 명세의 data와 timestamp만 반환한다. (성공 대신 시각으로 변환 => 이미 신청한 강의인지 판단 가능)
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {

        private T data;
        private OffsetDateTime timestamp;

        public static <T> ApiResponse<T> success(
                T data
        ) {
            return ApiResponse.<T>builder()
                    .data(data)
                    .timestamp(OffsetDateTime.now(
                            SERVICE_ZONE_ID
                    ))
                    .build();
        }
    }

    /**
     * [추가] 공통 오류 응답
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {

        private String code;
        private String message;
        private OffsetDateTime timestamp;

        public static ErrorResponse of(
                String code,
                String message
        ) {
            return ErrorResponse.builder()
                    .code(code)
                    .message(message)
                    .timestamp(OffsetDateTime.now(
                            SERVICE_ZONE_ID
                    ))
                    .build();
        }
    }

    /**
     * Entity의 LocalDateTime을
     * API의 ISO 8601 OffsetDateTime으로 변환한다.
     */
    private static OffsetDateTime toOffsetDateTime(
            LocalDateTime dateTime
    ) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZone(SERVICE_ZONE_ID)
                .toOffsetDateTime();
    }
}