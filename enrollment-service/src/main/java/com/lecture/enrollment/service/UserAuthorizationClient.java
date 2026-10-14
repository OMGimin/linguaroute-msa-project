package com.lecture.enrollment.service;

import com.lecture.enrollment.exception.EnrollmentException;
import com.lecture.enrollment.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthorizationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.user-service.url}")
    private String userServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public AuthorizationContext getAuthorizationContext(Long userId) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/internal/users/{id}/authorization-context", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            Map<String, Object> data = extractData(response);
            return new AuthorizationContext(
                    toLong(data.get("userId")),
                    toLong(data.get("companyId")),
                    stringValue(data.get("businessRole")),
                    stringValue(data.get("status"))
            );
        } catch (Exception e) {
            log.error("[UserAuthorizationClient] 사용자 권한 조회 실패 - userId: {}, error: {}",
                    userId, e.getMessage());
            throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new EnrollmentException(ErrorCode.INTERNAL_SERVICE_UNAVAILABLE);
        }
        return (Map<String, Object>) data;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    @Getter
    @RequiredArgsConstructor
    public static class AuthorizationContext {
        private final Long userId;
        private final Long companyId;
        private final String businessRole;
        private final String status;

        public boolean isActiveEmployee() {
            return "ACTIVE".equals(status)
                    && "EMPLOYEE".equals(businessRole)
                    && companyId != null;
        }

        public boolean isActiveCompanyAdmin() {
            return "ACTIVE".equals(status)
                    && "COMPANY_ADMIN".equals(businessRole)
                    && companyId != null;
        }

        public boolean isActivePlatformAdmin() {
            return "ACTIVE".equals(status) && "PLATFORM_ADMIN".equals(businessRole);
        }
    }
}
