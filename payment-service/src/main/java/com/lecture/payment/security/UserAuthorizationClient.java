package com.lecture.payment.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lecture.payment.exception.PaymentErrorCode;
import com.lecture.payment.exception.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserAuthorizationClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public UserAuthorizationClient(
            RestClient.Builder builder,
            @Value("${service.user-service.url}") String userServiceUrl,
            @Value("${app.security.internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(userServiceUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public Long requireCompanyAdminCompanyId(Long userId) {
        try {
            AuthorizationResponse response = restClient.get()
                    .uri("/internal/users/{userId}/authorization-context", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(AuthorizationResponse.class);

            if (response == null || response.data() == null
                    || !"ACTIVE".equals(response.data().status())
                    || !"COMPANY_ADMIN".equals(response.data().businessRole())
                    || response.data().companyId() == null) {
                throw new PaymentException(PaymentErrorCode.COMPANY_ADMIN_REQUIRED);
            }
            return response.data().companyId();
        } catch (PaymentException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PaymentException(PaymentErrorCode.USER_AUTHORIZATION_UNAVAILABLE);
        }
    }

    public void requirePlatformAdmin(Long userId) {
        AuthorizationData data = getAuthorizationData(userId);
        if (!"ACTIVE".equals(data.status()) || !"PLATFORM_ADMIN".equals(data.businessRole())) {
            throw new PaymentException(PaymentErrorCode.PLATFORM_ADMIN_REQUIRED);
        }
    }

    private AuthorizationData getAuthorizationData(Long userId) {
        try {
            AuthorizationResponse response = restClient.get()
                    .uri("/internal/users/{userId}/authorization-context", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(AuthorizationResponse.class);
            if (response == null || response.data() == null) {
                throw new PaymentException(PaymentErrorCode.USER_AUTHORIZATION_UNAVAILABLE);
            }
            return response.data();
        } catch (PaymentException e) {
            throw e;
        } catch (RestClientException e) {
            throw new PaymentException(PaymentErrorCode.USER_AUTHORIZATION_UNAVAILABLE);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizationResponse(AuthorizationData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizationData(Long userId, Long companyId, String businessRole, String status) {
    }
}
