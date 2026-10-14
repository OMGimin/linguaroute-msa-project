package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.entity.EmailVerification;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.EmailVerificationRepository;
import com.lecture.user.security.TokenHash;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final Clock clock;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestSignupVerification(AuthDto.EmailVerificationRequest request) {
        if (request.getPurpose() != EmailVerification.Purpose.SIGNUP) {
            throw new IllegalArgumentException("지원하지 않는 이메일 인증 목적입니다");
        }

        String email = normalizeEmail(request.getEmail());
        LocalDateTime now = LocalDateTime.now(clock);
        validateRequestLimit(email, now);
        String verificationCode = generateVerificationCode();

        emailVerificationRepository.save(EmailVerification.builder()
                .email(email)
                .purpose(EmailVerification.Purpose.SIGNUP)
                .codeHash(TokenHash.sha256(verificationCode))
                .expiresAt(now.plusMinutes(15))
                .build());

        sendVerificationEmail(email, verificationCode);
    }

    @Transactional
    public AuthDto.EmailVerificationConfirmResponse confirmSignupVerification(
            AuthDto.EmailVerificationConfirmRequest request
    ) {
        String email = normalizeEmail(request.getEmail());
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailIgnoreCaseAndPurposeAndVerifiedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
                        email,
                        EmailVerification.Purpose.SIGNUP
                )
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_VERIFICATION_CODE));

        LocalDateTime now = LocalDateTime.now(clock);
        if (!verification.canConfirm(email, request.getVerificationCode(), now)) {
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        String rawToken = generateToken();
        verification.confirm(TokenHash.sha256(rawToken), now);
        return new AuthDto.EmailVerificationConfirmResponse(rawToken);
    }

    @Transactional
    public void consumeSignupToken(String email, String rawToken) {
        EmailVerification verification = emailVerificationRepository
                .findByTokenHash(TokenHash.sha256(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMAIL_VERIFICATION));

        LocalDateTime now = LocalDateTime.now(clock);
        if (!verification.canUseForSignup(email, now)) {
            throw new ApiException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }
        verification.markUsed(now);
    }

    private void validateRequestLimit(String email, LocalDateTime now) {
        long requestsInLastMinute = emailVerificationRepository
                .countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                        email,
                        EmailVerification.Purpose.SIGNUP,
                        now.minusMinutes(1)
                );
        long requestsInLastHour = emailVerificationRepository
                .countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                        email,
                        EmailVerification.Purpose.SIGNUP,
                        now.minusHours(1)
                );
        if (requestsInLastMinute > 0 || requestsInLastHour >= 5) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_REQUEST_LIMIT);
        }
    }

    private void sendVerificationEmail(String email, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[LinguaRoute] 이메일 인증 코드");
        message.setText("LinguaRoute 이메일 인증 코드: " + verificationCode + "\n유효 시간: 15분");
        mailSender.send(message);
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
