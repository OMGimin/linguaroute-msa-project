package com.lecture.course.security;

import com.lecture.course.exception.CourseErrorCode;
import com.lecture.course.exception.CourseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class InternalApiKeyValidator {

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public void validate(String requestApiKey) {
        if (!StringUtils.hasText(requestApiKey) || !requestApiKey.equals(internalApiKey)) {
            throw new CourseException(CourseErrorCode.INVALID_INTERNAL_API_KEY);
        }
    }
}
