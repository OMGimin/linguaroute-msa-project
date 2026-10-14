package com.lecture.enrollment.service;

import com.lecture.enrollment.exception.EnrollmentException;
import com.lecture.enrollment.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.course-service.url}")
    private String courseServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public void validateEnrollable(Long courseId) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(courseServiceUrl + "/internal/courses/{id}/enrollment-validation", courseId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) {
                throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
            }

            boolean enrollable = Boolean.TRUE.equals(response.get("enrollable"));
            if (!enrollable) {
                throw new EnrollmentException(ErrorCode.COURSE_INACTIVE);
            }
        } catch (Exception e) {
            if (e instanceof EnrollmentException enrollmentException) {
                throw enrollmentException;
            }
            log.error("[CourseServiceClient] 강의 수강 가능 여부 확인 실패 - courseId: {}, error: {}",
                    courseId, e.getMessage());
            throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    public Map<String, Object> getCourse(Long courseId) {
        try {
            Map<String, Object> responseBody = webClientBuilder.build()
                    .get()
                    .uri(courseServiceUrl + "/internal/courses/{id}", courseId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (responseBody == null) {
                throw new RuntimeException("Course Service 응답 본문이 비어 있습니다.");
            }

            log.debug("[CourseServiceClient] 강의 상세 응답 - courseId: {}, body: {}", courseId, responseBody);
            Object data = responseBody.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> courseMap = (Map<String, Object>) dataMap;
                return courseMap;
            }

            return responseBody;
        } catch (Exception e) {
            log.error("[CourseServiceClient] 강의 상세 조회 실패 - courseId: {}, error: {}",
                    courseId, e.getMessage());
            throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    public List<LessonInfo> getLessons(Long courseId) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(courseServiceUrl + "/internal/courses/{id}/lessons", courseId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response == null || !(response.get("data") instanceof List<?> lessons)) {
                throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
            }
            return lessons.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .map(item -> new LessonInfo(
                            toLong(item.get("lessonId")),
                            Boolean.TRUE.equals(item.get("required"))))
                    .toList();
        } catch (EnrollmentException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("[CourseServiceClient] 차시 목록 조회 실패 - courseId: {}, error: {}",
                    courseId, exception.getMessage());
            throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public record LessonInfo(Long lessonId, boolean required) {
    }
}
