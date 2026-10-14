package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.security.InternalApiKeyValidator;
import com.lecture.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/enrollments")
@RequiredArgsConstructor
public class InternalEnrollmentController {

    private final EnrollmentService enrollmentService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @GetMapping("/history/{userId}")
    public ResponseEntity<EnrollmentDto.EnrollmentHistoryResponse> getEnrollmentHistory(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(enrollmentService.getEnrollmentHistory(userId));
    }
}
