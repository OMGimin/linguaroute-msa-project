package com.lecture.user.dto;

import com.lecture.user.entity.EmailVerification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerificationRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotNull(message = "인증 목적은 필수입니다")
        private EmailVerification.Purpose purpose;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerificationConfirmRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "인증 코드는 필수입니다")
        private String verificationCode;
    }

    @Getter
    @AllArgsConstructor
    public static class AcceptedResponse {
        private boolean accepted;
    }

    @Getter
    @AllArgsConstructor
    public static class EmailVerificationConfirmResponse {
        private String emailVerificationToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetConfirmRequest {
        @NotBlank(message = "재설정 토큰은 필수입니다")
        private String resetToken;

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
        private String newPassword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdFindRequest {
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        private String name;

        @NotBlank(message = "사업자등록번호는 필수입니다")
        private String businessNumber;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeRequest {
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
        private String newPassword;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthCodeExchangeRequest {
        @NotBlank(message = "Authorization Code는 필수입니다")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSignupRequest {
        @NotBlank(message = "초대코드는 필수입니다")
        private String invitationCode;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        private String name;

        @NotBlank(message = "이메일 인증 토큰은 필수입니다")
        private String emailVerificationToken;

        @NotEmpty(message = "약관 동의는 필수입니다")
        private java.util.List<Long> agreementIds;
    }

    @Getter
    @AllArgsConstructor
    public static class EmployeeSignupResponse {
        private Long userId;
        private Long companyId;
        private String businessRole;
        private String status;
    }

    @Getter
    @AllArgsConstructor
    public static class OAuthTokenResponse {
        private String accessToken;
        private String tokenType;
        private long expiresIn;
    }

    @Getter
    @NoArgsConstructor
    public static class AuthorizationServerTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private long expiresIn;
    }
}
