package com.lecture.payment.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProcessor {

    public PaymentProcessResult process(String paymentMethodToken) {
        return switch (paymentMethodToken) {
            case "mock-success" -> PaymentProcessResult.success("mock-" + UUID.randomUUID());
            case "mock-failure" -> PaymentProcessResult.failure("CARD_DECLINED", "결제가 승인되지 않았습니다.");
            default -> PaymentProcessResult.invalid();
        };
    }

    public record PaymentProcessResult(
            boolean success,
            boolean valid,
            String providerPaymentId,
            String failureCode,
            String failureReason
    ) {
        static PaymentProcessResult success(String providerPaymentId) {
            return new PaymentProcessResult(true, true, providerPaymentId, null, null);
        }

        static PaymentProcessResult failure(String failureCode, String failureReason) {
            return new PaymentProcessResult(false, true, null, failureCode, failureReason);
        }

        static PaymentProcessResult invalid() {
            return new PaymentProcessResult(false, false, null, "INVALID_PAYMENT_METHOD", "지원하지 않는 결제 수단 토큰입니다.");
        }
    }
}
