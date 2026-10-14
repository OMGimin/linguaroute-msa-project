package com.lecture.course.exception;

import lombok.Getter;

@Getter
public class CourseException extends RuntimeException {

    private final CourseErrorCode errorCode;

    public CourseException(CourseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
