package com.lecture.user.dto;

import com.lecture.user.entity.Invitation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class InvitationDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateRequest {
        @NotBlank(message = "초대코드는 필수입니다")
        private String invitationCode;
    }

    @Getter
    @AllArgsConstructor
    public static class ValidationResponse {
        private boolean valid;
        private Long companyId;
        private LocalDateTime expiresAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "초대코드 유효 기간은 필수입니다")
        @Min(value = 1, message = "초대코드 유효 기간은 1일 이상이어야 합니다")
        @Max(value = 30, message = "초대코드 유효 기간은 30일 이하여야 합니다")
        private Integer expiresInDays;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long invitationId;
        private String code;
        private String codeMasked;
        private Invitation.Status status;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime usedAt;

        public static Response from(Invitation invitation) {
            return new Response(invitation.getId(), null, "****-****", invitation.getStatus(),
                    invitation.getExpiresAt(), invitation.getCreatedAt(), invitation.getUsedAt());
        }

        public static Response issued(Invitation invitation, String rawCode) {
            return new Response(invitation.getId(), rawCode, "****-****", invitation.getStatus(),
                    invitation.getExpiresAt(), invitation.getCreatedAt(), invitation.getUsedAt());
        }
    }
}
