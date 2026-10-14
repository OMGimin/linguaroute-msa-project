package com.lecture.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitations", uniqueConstraints = @UniqueConstraint(
        name = "uk_invitations_code_hash", columnNames = "code_hash"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedBy;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Invitation unused(Company company, User createdBy, String codeHash, LocalDateTime expiresAt) {
        return Invitation.builder()
                .company(company)
                .createdBy(createdBy)
                .codeHash(codeHash)
                .status(Status.UNUSED)
                .expiresAt(expiresAt)
                .build();
    }

    public void expireIfDue(LocalDateTime now) {
        if (status == Status.UNUSED && !expiresAt.isAfter(now)) {
            status = Status.EXPIRED;
        }
    }

    public void revokeIfUnused() {
        if (status == Status.UNUSED) {
            status = Status.REVOKED;
        }
    }

    public void use(User user, LocalDateTime usedAt) {
        this.status = Status.USED;
        this.usedBy = user;
        this.usedAt = usedAt;
    }

    public enum Status {
        UNUSED,
        USED,
        EXPIRED,
        REVOKED
    }
}
