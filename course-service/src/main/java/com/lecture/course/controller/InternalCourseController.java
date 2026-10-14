package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.security.InternalApiKeyValidator;
import com.lecture.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class InternalCourseController {

    private final CourseService courseService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.CourseResponse> getCourse(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(courseService.getInternalCourse(id));
    }

    @GetMapping("/{id}/enrollment-validation")
    public ResponseEntity<CourseDto.EnrollmentValidationResponse> getEnrollmentValidation(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(courseService.getEnrollmentValidation(id));
    }

    @GetMapping("/{id}/lessons")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.LessonResponse>>> getLessons(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.getLessons(id)));
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<CourseDto.CourseResponse>> getRecommendCourses(
            @RequestParam Course.Language language,
            @RequestParam(defaultValue = "") List<Long> excludeIds,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(courseService.getRecommendCourses(language, excludeIds));
    }
}
