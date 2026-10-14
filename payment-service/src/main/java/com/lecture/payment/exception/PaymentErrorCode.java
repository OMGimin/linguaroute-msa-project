package com.lecture.payment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {
    MISSING_COMPANY_CONTEXT(HttpStatus.BAD_REQUEST, "MISSING_COMPANY_CONTEXT", "기업 식별 정보가 필요합니다"),
    INVALID_USER_CONTEXT(HttpStatus.UNAUTHORIZED, "INVALID_USER_CONTEXT", "인증 사용자 정보가 올바르지 않습니다"),
    COMPANY_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "COMPANY_ADMIN_REQUIRED", "기업 관리자 권한이 필요합니다"),
    PLATFORM_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "PLATFORM_ADMIN_REQUIRED", "플랫폼 관리자 권한이 필요합니다"),
    USER_AUTHORIZATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "USER_AUTHORIZATION_UNAVAILABLE", "사용자 권한 정보를 확인할 수 없습니다"),
    MISSING_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key 헤더가 필요합니다"),
    PLAN_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_PRICE_NOT_FOUND", "요금제를 찾을 수 없습니다"),
    ACTIVE_SUBSCRIPTION_EXISTS(HttpStatus.CONFLICT, "ACTIVE_SUBSCRIPTION_EXISTS", "이미 활성 구독이 있습니다"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다"),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD", "지원하지 않는 결제 수단 토큰입니다"),
    PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_FAILED", "결제가 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
