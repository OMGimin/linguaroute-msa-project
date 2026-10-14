package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/me/enrollments")
@RequiredArgsConstructor
public class CompanyEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.PageResponse<EnrollmentDto.ManagementEnrollmentResponse>>> getEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getCompanyEnrollments(userId, page, size)));
    }

    @GetMapping("/progress")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.PageResponse<EnrollmentDto.ManagementEnrollmentResponse>>> getProgress(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getCompanyEnrollments(userId, page, size)));
    }
}
