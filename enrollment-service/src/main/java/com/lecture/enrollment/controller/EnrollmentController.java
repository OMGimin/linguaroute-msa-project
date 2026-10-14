package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * POST /enrollments - 수강신청
     * Gateway에서 X-User-Id 헤더로 사용자 ID 전달
     */
    @PostMapping
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollResponse>> enroll(
            @Valid @RequestBody EnrollmentDto.EnrollRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        EnrollmentDto.EnrollResponse response =
                enrollmentService.enroll(userId, request.getCourseId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/me - 내 수강 목록 조회
     * Gateway가 전달한 X-User-Id 헤더를 사용
     */
    @GetMapping("/me")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.EnrollmentSummaryResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId) {

        List<EnrollmentDto.EnrollmentSummaryResponse> response =
                enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentDetailResponse>> getEnrollment(
            @PathVariable Long enrollmentId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getEnrollmentDetail(userId, enrollmentId)));
    }

    @PostMapping("/{enrollmentId}/lessons/{lessonId}/start")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.LessonActionResponse>> startLesson(
            @PathVariable Long enrollmentId, @PathVariable Long lessonId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.startLesson(userId, enrollmentId, lessonId)));
    }

    @PostMapping("/{enrollmentId}/lessons/{lessonId}/complete")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.LessonActionResponse>> completeLesson(
            @PathVariable Long enrollmentId, @PathVariable Long lessonId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.completeLesson(userId, enrollmentId, lessonId)));
    }

}
