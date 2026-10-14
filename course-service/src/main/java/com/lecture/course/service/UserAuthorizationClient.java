package com.lecture.course.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lecture.course.exception.CourseErrorCode;
import com.lecture.course.exception.CourseException;
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

    public void requirePlatformAdmin(Long userId) {
        try {
            AuthorizationResponse response = restClient.get()
                    .uri("/internal/users/{userId}/authorization-context", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .body(AuthorizationResponse.class);

            if (response == null || response.data() == null
                    || !"ACTIVE".equals(response.data().status())
                    || !"PLATFORM_ADMIN".equals(response.data().businessRole())) {
                throw new CourseException(CourseErrorCode.PLATFORM_ADMIN_REQUIRED);
            }
        } catch (CourseException e) {
            throw e;
        } catch (RestClientException e) {
            throw new CourseException(CourseErrorCode.USER_AUTHORIZATION_UNAVAILABLE);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizationResponse(AuthorizationData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorizationData(Long userId, String businessRole, String status) {
    }
}
