package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.security.PaymentRequestContext;
import com.lecture.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRequestContext paymentRequestContext;

    @GetMapping
    public ResponseEntity<PaymentDto.ApiResponse<List<PaymentDto.PaymentResponse>>> getPayments(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long companyId = paymentRequestContext.companyId(userIdHeader);
        return ResponseEntity.ok(PaymentDto.ApiResponse.success(paymentService.getPayments(companyId)));
    }
}
