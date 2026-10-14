package com.lecture.enrollment.exception;

import lombok.Getter;

@Getter
public class EnrollmentException extends RuntimeException {

    private final ErrorCode errorCode;

    public EnrollmentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 기본 메시지 대신 더 구체적인 메시지가 필요한 경우 사용한다.
     */
    public EnrollmentException(
            ErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }
}