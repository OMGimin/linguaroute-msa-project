package com.lecture.user.dto;

import com.lecture.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    // 사용자 정보 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private User.AuthRole role;
        private User.BusinessRole businessRole;
        private Long companyId;
        private User.Status status;
        private LocalDateTime createdAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .businessRole(user.getBusinessRole())
                    .companyId(user.getCompany() == null ? null : user.getCompany().getId())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AuthorizationContextResponse {
        private Long userId;
        private Long companyId;
        private User.BusinessRole businessRole;
        private User.Status status;

        public static AuthorizationContextResponse from(User user) {
            return new AuthorizationContextResponse(
                    user.getId(),
                    user.getCompany() == null ? null : user.getCompany().getId(),
                    user.getBusinessRole(),
                    user.getStatus()
            );
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgreementRequest {
        @NotEmpty(message = "약관 동의는 필수입니다")
        private java.util.List<@jakarta.validation.constraints.NotNull(message = "약관 ID는 필수입니다") Long> agreementIds;
    }

    @Getter
    @AllArgsConstructor
    public static class AgreementResponse {
        private java.util.List<Long> agreedTermIds;
    }

}
