package com.lecture.enrollment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // [추가] 수강신청 오류
    DUPLICATE_ENROLLMENT(
            HttpStatus.CONFLICT,
            "이미 신청한 강의입니다."
    ),

    ENROLLMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "수강 정보를 찾을 수 없습니다."
    ),

    // [추가] 강의·차시 검증 오류
    COURSE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "강의를 찾을 수 없습니다."
    ),

    COURSE_INACTIVE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "현재 수강할 수 없는 강의입니다."
    ),

    INVALID_INTERNAL_API_KEY(
            HttpStatus.UNAUTHORIZED,
            "내부 API 키가 올바르지 않습니다."
    ),

    LESSON_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "차시를 찾을 수 없습니다."
    ),

    LESSON_NOT_IN_COURSE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "해당 강의에 포함되지 않은 차시입니다."
    ),

    // [추가] 기업 구독 오류
    SUBSCRIPTION_INACTIVE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "활성 구독이 없는 기업입니다."
    ),

    // [추가] 권한·소유권 오류
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "해당 요청을 처리할 권한이 없습니다."
    ),

    // [추가] 내부 서비스 연동 오류
    INTERNAL_SERVICE_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "내부 서비스에 연결할 수 없습니다."
    ),

    // [추가] 요청 형식이나 필수값 오류
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "잘못된 요청입니다."
    ),

    // [추가] 현재 수강 상태에서 처리할 수 없는 요청
    INVALID_ENROLLMENT_STATE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "현재 수강 상태에서는 처리할 수 없습니다."
    ),

    // [추가] 예상하지 못한 서버 내부 오류
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String message;
}
