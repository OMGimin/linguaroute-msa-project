package com.lecture.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "token_hash", unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean canUseForSignup(String requestedEmail, LocalDateTime now) {
        return purpose == Purpose.SIGNUP
                && email.equalsIgnoreCase(requestedEmail)
                && verifiedAt != null
                && usedAt == null
                && expiresAt.isAfter(now);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public boolean canConfirm(String requestedEmail, String requestedCode, LocalDateTime now) {
        return purpose == Purpose.SIGNUP
                && email.equalsIgnoreCase(requestedEmail)
                && verifiedAt == null
                && usedAt == null
                && expiresAt.isAfter(now)
                && codeHash.equals(com.lecture.user.security.TokenHash.sha256(requestedCode));
    }

    public void confirm(String tokenHash, LocalDateTime confirmedAt) {
        this.tokenHash = tokenHash;
        this.verifiedAt = confirmedAt;
    }

    public enum Purpose {
        SIGNUP
    }
}
