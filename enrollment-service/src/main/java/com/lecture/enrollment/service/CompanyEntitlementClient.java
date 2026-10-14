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
public class CompanyEntitlementClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.user-service.url}")
    private String userServiceUrl;

    @Value("${app.security.internal-api-key}")
    private String internalApiKey;

    public Entitlement getEntitlement(Long companyId) {
        try {
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/internal/companies/{companyId}/entitlement", companyId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            Map<String, Object> data = extractData(response);
            return new Entitlement(
                    toLong(data.get("companyId")),
                    stringValue(data.get("subscriptionStatus"))
            );
        } catch (Exception e) {
            log.error("[CompanyEntitlementClient] 기업 구독 권한 조회 실패 - companyId: {}, error: {}",
                    companyId, e.getMessage());
            throw new EnrollmentException(ErrorCode.SUBSCRIPTION_INACTIVE);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new EnrollmentException(ErrorCode.SUBSCRIPTION_INACTIVE);
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
    public static class Entitlement {
        private final Long companyId;
        private final String subscriptionStatus;

        public boolean isActive() {
            return "ACTIVE".equals(subscriptionStatus);
        }
    }
}
