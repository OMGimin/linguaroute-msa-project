package com.lecture.enrollment.config;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.exception.EnrollmentException;
import com.lecture.enrollment.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [추가]
     * 수강 업무 예외 처리.
     *
     * ErrorCode에 정의한 HTTP 상태와 오류 코드를 반환한다.
     */
    @ExceptionHandler(EnrollmentException.class)
    public ResponseEntity<EnrollmentDto.ErrorResponse>
    handleEnrollmentException(
            EnrollmentException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        EnrollmentDto.ErrorResponse response =
                EnrollmentDto.ErrorResponse.of(
                        errorCode.name(),
                        exception.getMessage()
                );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * [변경]
     * @Valid 요청 DTO 검증 실패 처리.
     *
     * 예:
     * courseId 누락
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EnrollmentDto.ErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(", "));

        EnrollmentDto.ErrorResponse response =
                EnrollmentDto.ErrorResponse.of(
                        ErrorCode.INVALID_REQUEST.name(),
                        message
                );

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(response);
    }

    /**
     * [변경]
     * Entity 또는 Service의 잘못된 인자 처리.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EnrollmentDto.ErrorResponse>
    handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        EnrollmentDto.ErrorResponse response =
                EnrollmentDto.ErrorResponse.of(
                        ErrorCode.INVALID_REQUEST.name(),
                        exception.getMessage()
                );

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(response);
    }

    /**
     * [추가]
     * 현재 수강 상태에서 처리할 수 없는 요청.
     *
     * 예:
     * 완료된 수강을 다시 학습 상태로 변경
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<EnrollmentDto.ErrorResponse>
    handleIllegalStateException(
            IllegalStateException exception
    ) {
        EnrollmentDto.ErrorResponse response =
                EnrollmentDto.ErrorResponse.of(
                        ErrorCode.INVALID_ENROLLMENT_STATE.name(),
                        exception.getMessage()
                );

        return ResponseEntity
                .status(
                        ErrorCode.INVALID_ENROLLMENT_STATE
                                .getHttpStatus()
                )
                .body(response);
    }

    /**
     * [변경]
     * 예상하지 못한 서버 오류 처리.
     *
     * 오류 상세 내용은 로그에만 기록하고
     * API 응답에는 내부 예외 내용을 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<EnrollmentDto.ErrorResponse>
    handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "처리되지 않은 서버 오류가 발생했습니다.",
                exception
        );

        EnrollmentDto.ErrorResponse response =
                EnrollmentDto.ErrorResponse.of(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                );

        return ResponseEntity
                .status(
                        ErrorCode.INTERNAL_SERVER_ERROR
                                .getHttpStatus()
                )
                .body(response);
    }
}