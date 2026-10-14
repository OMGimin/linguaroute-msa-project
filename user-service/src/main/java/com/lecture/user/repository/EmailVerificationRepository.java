package com.lecture.user.repository;

import com.lecture.user.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.time.LocalDateTime;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findTopByEmailIgnoreCaseAndPurposeAndVerifiedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
            String email,
            EmailVerification.Purpose purpose
    );

    long countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
            String email,
            EmailVerification.Purpose purpose,
            LocalDateTime createdAt
    );
}
