package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.security.UserAuthorizationClient;
import com.lecture.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {
    private final PaymentService paymentService;
    private final UserAuthorizationClient userAuthorizationClient;

    @GetMapping
    public ResponseEntity<PaymentDto.ApiResponse<List<PaymentDto.PaymentResponse>>> getPayments(
            @RequestHeader("X-User-Id") Long userId) {
        userAuthorizationClient.requirePlatformAdmin(userId);
        return ResponseEntity.ok(PaymentDto.ApiResponse.success(paymentService.getAllPayments()));
    }
}
