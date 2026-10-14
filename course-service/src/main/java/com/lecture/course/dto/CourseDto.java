package com.lecture.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lecture.course.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public class CourseDto {
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    @Getter @NoArgsConstructor @AllArgsConstructor public static class LessonRequest { @NotBlank private String title; @NotBlank private String contentUrl; @NotNull private Integer sequence; @NotNull private Boolean required; @NotNull private Integer durationSeconds; }
    @Getter @AllArgsConstructor public static class LessonResponse { private Long lessonId; private String title; private String contentUrl; private Integer sequence; private boolean required; private Integer durationSeconds; public static LessonResponse from(com.lecture.course.entity.Lesson l){return new LessonResponse(l.getId(),l.getTitle(),l.getContentUrl(),l.getSequence(),l.isRequired(),l.getDurationSeconds());} }

    // 강의 등록 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "강의 제목은 필수입니다")
        @Size(max = 255, message = "강의 제목은 255자 이하여야 합니다")
        private String title;

        private String description;

        @NotNull(message = "언어는 필수입니다")
        private Course.Language language;

        @NotNull(message = "상황은 필수입니다")
        private Course.Situation situation;

        @NotNull(message = "난이도는 필수입니다")
        private Course.Level level;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @NotBlank(message = "강의 제목은 필수입니다")
        @Size(max = 255, message = "강의 제목은 255자 이하여야 합니다")
        private String title;

        private String description;

        @NotNull(message = "언어는 필수입니다")
        private Course.Language language;

        @NotNull(message = "상황은 필수입니다")
        private Course.Situation situation;

        @NotNull(message = "난이도는 필수입니다")
        private Course.Level level;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusRequest {

        @NotNull(message = "강의 상태는 필수입니다")
        private Course.Status status;
    }

    // 강의 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseResponse {
        private Long id;
        private String title;
        private String description;
        private Course.Language language;
        private Course.Situation situation;
        private Course.Level level;
        private Course.Status status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static CourseResponse from(Course course) {
            return CourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .language(course.getLanguage())
                    .situation(course.getSituation())
                    .level(course.getLevel())
                    .status(course.getStatus())
                    .createdAt(course.getCreatedAt())
                    .updatedAt(course.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseSummaryResponse {
        private Long courseId;
        private String title;
        private Course.Language language;
        private Course.Situation situation;
        private Course.Level level;
        private Course.Status status;

        public static CourseSummaryResponse from(Course course) {
            return CourseSummaryResponse.builder()
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .language(course.getLanguage())
                    .situation(course.getSituation())
                    .level(course.getLevel())
                    .status(course.getStatus())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoursePageResponse {
        private List<CourseSummaryResponse> content;
        private int page;
        private int size;
        private long totalElements;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentValidationResponse {
        private Long courseId;
        private Course.Status status;
        private boolean enrollable;
    }

    // 공통 API 응답 래퍼
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private T data;
        private String code;
        private String message;
        private OffsetDateTime timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .data(data)
                    .timestamp(OffsetDateTime.now(SERVICE_ZONE_ID))
                    .build();
        }

        public static <T> ApiResponse<T> error(String code, String message) {
            return ApiResponse.<T>builder()
                    .code(code)
                    .message(message)
                    .timestamp(OffsetDateTime.now(SERVICE_ZONE_ID))
                    .build();
        }
    }

    // 추천 서비스용 응답 (언어 기반 강의 목록)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendResponse {
        private List<CourseResponse> courses;
        private Course.Language language;
    }
}
