package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.service.CourseService;
import com.lecture.course.service.UserAuthorizationClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final UserAuthorizationClient userAuthorizationClient;

    @GetMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CoursePageResponse>> getCourses(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.lecture.course.entity.Course.Language language,
            @RequestParam(required = false) com.lecture.course.entity.Course.Situation situation,
            @RequestParam(required = false) com.lecture.course.entity.Course.Level level,
            @RequestParam(required = false) com.lecture.course.entity.Course.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.searchAdminCourses(
                keyword, language, situation, level, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> getCourse(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.getInternalCourse(courseId)));
    }

    @GetMapping("/{courseId}/lessons")
    public ResponseEntity<CourseDto.ApiResponse<java.util.List<CourseDto.LessonResponse>>> getLessons(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.getLessons(courseId)));
    }

    @PostMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> createCourse(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CourseDto.CreateRequest request) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseDto.ApiResponse.success(courseService.createCourse(request)));
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> updateCourse(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDto.UpdateRequest request) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.updateCourse(courseId, request)));
    }

    @PostMapping(value = "/{courseId}", params = "action=update-course")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> updateCourseFromBrowser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDto.UpdateRequest request) {
        return updateCourse(userId, courseId, request);
    }

    @PatchMapping("/{courseId}/status")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> changeCourseStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDto.StatusRequest request) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.changeCourseStatus(courseId, request)));
    }


    @PostMapping(value = "/{courseId}/status", params = "action=update-status")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> changeCourseStatusFromBrowser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDto.StatusRequest request) {
        return changeCourseStatus(userId, courseId, request);
    }

    @PostMapping("/{courseId}/lessons")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.LessonResponse>> addLesson(@RequestHeader("X-User-Id") Long userId,@PathVariable Long courseId,@Valid @RequestBody CourseDto.LessonRequest request){userAuthorizationClient.requirePlatformAdmin(userId);return ResponseEntity.status(HttpStatus.CREATED).body(CourseDto.ApiResponse.success(courseService.addLesson(courseId,request)));}
}
