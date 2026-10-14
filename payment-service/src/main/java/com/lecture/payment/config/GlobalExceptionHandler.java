package com.lecture.payment.config;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.lecture.payment.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<PaymentDto.ErrorResponse> handlePaymentException(PaymentException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(PaymentDto.ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentDto.ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(PaymentDto.ErrorResponse.of("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PaymentDto.ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PaymentDto.ErrorResponse.of("INVALID_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentDto.ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentDto.ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"));
    }
}
