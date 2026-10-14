package com.lecture.user.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자번호입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰의 사용자 정보가 올바르지 않습니다"),
    INVALID_INTERNAL_API_KEY(HttpStatus.FORBIDDEN, "내부 API 키가 올바르지 않습니다"),
    INVALID_EMAIL_VERIFICATION(HttpStatus.UNPROCESSABLE_ENTITY, "이메일 인증 토큰이 유효하지 않습니다"),
    INVALID_VERIFICATION_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "이메일 인증 코드가 올바르지 않거나 만료되었습니다"),
    EMAIL_VERIFICATION_REQUEST_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "이메일 인증 요청 횟수를 초과했습니다"),
    PASSWORD_RESET_REQUEST_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "비밀번호 재설정 요청 횟수를 초과했습니다"),
    OAUTH_TOKEN_EXCHANGE_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "로그인 인증 코드를 교환할 수 없습니다"),
    INVALID_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "현재 비밀번호가 올바르지 않습니다"),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.UNPROCESSABLE_ENTITY, "비밀번호 재설정 링크가 올바르지 않거나 만료되었습니다"),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "기업을 찾을 수 없습니다"),
    COMPANY_ENTITLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "기업 구독 권한 정보를 찾을 수 없습니다"),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대코드를 찾을 수 없습니다"),
    INVITATION_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용한 초대코드입니다"),
    INVITATION_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "만료된 초대코드입니다"),
    INVITATION_REVOKED(HttpStatus.UNPROCESSABLE_ENTITY, "폐기된 초대코드입니다"),
    SEAT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "사용 가능한 좌석이 없습니다"),
    SUBSCRIPTION_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "활성 구독이 없는 기업입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "활성 상태의 사용자만 요청할 수 있습니다"),
    COMPANY_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "기업 관리자 권한이 필요합니다"),
    PLATFORM_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "플랫폼 관리자 권한이 필요합니다"),
    COMPANY_MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "소속 기업 정보가 없습니다"),
    INVALID_AGREEMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 약관 동의입니다"),
    REQUIRED_AGREEMENT_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "필수 약관에 동의해야 합니다");

    private final HttpStatus status;
    private final String message;
}
