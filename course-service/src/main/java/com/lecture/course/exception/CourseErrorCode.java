package com.lecture.course.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode {
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "강의를 찾을 수 없습니다"),
    COURSE_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "COURSE_INACTIVE", "비활성화된 강의입니다"),
    INVALID_INTERNAL_API_KEY(HttpStatus.UNAUTHORIZED, "INVALID_INTERNAL_API_KEY", "내부 API 키가 올바르지 않습니다"),
    PLATFORM_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "PLATFORM_ADMIN_REQUIRED", "플랫폼 관리자 권한이 필요합니다"),
    USER_AUTHORIZATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "USER_AUTHORIZATION_UNAVAILABLE", "사용자 권한을 확인할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
