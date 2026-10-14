package com.lecture.user.security;

import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyValidator {

    private final byte[] expectedApiKey;

    public InternalApiKeyValidator(@Value("${app.security.internal-api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey.getBytes(StandardCharsets.UTF_8);
    }

    public void validate(String providedApiKey) {
        if (providedApiKey == null || !MessageDigest.isEqual(
                expectedApiKey,
                providedApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(ErrorCode.INVALID_INTERNAL_API_KEY);
        }
    }
}
