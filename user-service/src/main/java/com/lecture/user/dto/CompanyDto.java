package com.lecture.user.dto;

import com.lecture.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CompanyDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotNull(message = "기업 정보는 필수입니다")
        @Valid
        private CompanyRequest company;

        @NotNull(message = "관리자 정보는 필수입니다")
        @Valid
        private AdminRequest admin;

        @NotBlank(message = "이메일 인증 토큰은 필수입니다")
        private String emailVerificationToken;

        @NotEmpty(message = "약관 동의는 필수입니다")
        private List<@NotNull(message = "약관 ID는 필수입니다") Long> agreementIds;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyRequest {
        @NotBlank(message = "기업명은 필수입니다")
        @Size(max = 100, message = "기업명은 100자 이하여야 합니다")
        private String name;

        @NotBlank(message = "사업자번호는 필수입니다")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자번호 형식이 올바르지 않습니다")
        private String businessNumber;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResponse {
        private Long companyId;
        private Long userId;
        private User.BusinessRole role;
        private User.Status status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "기업명은 필수입니다")
        @Size(max = 100, message = "기업명은 100자 이하여야 합니다")
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String businessNumber;
        private com.lecture.user.entity.Company.Status status;

        public static Response from(com.lecture.user.entity.Company company) {
            return new Response(
                    company.getId(),
                    company.getName(),
                    company.getBusinessNumber(),
                    company.getStatus()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class EntitlementResponse {
        private Long companyId;
        private Long subscriptionId;
        private String subscriptionStatus;
        private Integer seatLimit;
        private LocalDateTime currentPeriodEnd;
        private Boolean autoRenew;
    }

    @Getter
    @AllArgsConstructor
    public static class EmployeeResponse {
        private Long userId;
        private String email;
        private String name;
        private User.Status status;
        private LocalDateTime joinedAt;

        public static EmployeeResponse from(User user) {
            return new EmployeeResponse(user.getId(), user.getEmail(), user.getName(), user.getStatus(), user.getCreatedAt());
        }
    }

    public enum EmployeeStatusAction {
        ACTIVE,
        INACTIVE,
        RELEASED
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEmployeeStatusRequest {
        @NotNull(message = "직원 상태는 필수입니다")
        private EmployeeStatusAction status;
    }

    @Getter
    @AllArgsConstructor
    public static class SeatResponse {
        private Integer purchased;
        private long used;
        private long remaining;
    }
}
