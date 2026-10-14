package com.lecture.course.config;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.exception.CourseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleCourseException(CourseException e) {
        var error = e.getErrorCode();
        return ResponseEntity.status(error.getStatus())
                .body(CourseDto.ApiResponse.error(error.getCode(), error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CourseDto.ApiResponse.error("INVALID_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CourseDto.ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"));
    }
}
