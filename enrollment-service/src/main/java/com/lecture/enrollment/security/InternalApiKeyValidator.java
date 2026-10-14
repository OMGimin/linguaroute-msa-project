package com.lecture.enrollment.security;

import com.lecture.enrollment.exception.EnrollmentException;
import com.lecture.enrollment.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InternalApiKeyValidator {

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public void validate(String requestApiKey) {
        if (!StringUtils.hasText(requestApiKey) || !requestApiKey.equals(internalApiKey)) {
            throw new EnrollmentException(ErrorCode.INVALID_INTERNAL_API_KEY);
        }
    }
}
