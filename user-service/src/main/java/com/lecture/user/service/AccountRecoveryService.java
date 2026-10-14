package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.entity.PasswordResetToken;
import com.lecture.user.entity.User;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.PasswordResetTokenRepository;
import com.lecture.user.repository.UserRepository;
import com.lecture.user.security.TokenHash;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Clock clock;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(AuthDto.PasswordResetRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail())).orElse(null);
        if (user == null || user.getStatus() != User.Status.ACTIVE) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        validateResetRequestLimit(user.getId(), now);
        String rawToken = generateToken();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHash.sha256(rawToken))
                .expiresAt(now.plusMinutes(15))
                .build());
        sendPasswordResetEmail(user.getEmail(), rawToken);
    }

    @Transactional
    public void confirmPasswordReset(AuthDto.PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHash.sha256(request.getResetToken()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!token.canUse(now)) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        token.getUser().changePassword(passwordEncoder.encode(request.getNewPassword()));
        token.markUsed(now);
    }

    @Transactional(readOnly = true)
    public void requestIdFind(AuthDto.IdFindRequest request) {
        String businessNumber = request.getBusinessNumber().replaceAll("[^0-9]", "");
        List<User> users = userRepository.findAllByNameAndCompany_BusinessNumberAndStatus(
                request.getName().trim(), businessNumber, User.Status.ACTIVE
        );
        users.forEach(this::sendIdFindEmail);
    }

    private void validateResetRequestLimit(Long userId, LocalDateTime now) {
        long requestsInLastMinute = passwordResetTokenRepository
                .countByUser_IdAndCreatedAtAfter(userId, now.minusMinutes(1));
        long requestsInLastHour = passwordResetTokenRepository
                .countByUser_IdAndCreatedAtAfter(userId, now.minusHours(1));
        if (requestsInLastMinute > 0 || requestsInLastHour >= 5) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_REQUEST_LIMIT);
        }
    }

    private void sendPasswordResetEmail(String email, String rawToken) {
        String resetUrl = UriComponentsBuilder.fromHttpUrl(frontendBaseUrl)
                .path("/account/reset-password")
                .queryParam("token", rawToken)
                .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[LinguaRoute] 비밀번호 재설정");
        message.setText("아래 링크에서 비밀번호를 재설정해 주세요.\n" + resetUrl + "\n유효 시간: 15분");
        mailSender.send(message);
    }

    private void sendIdFindEmail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("[LinguaRoute] 아이디 찾기 안내");
        message.setText("요청하신 계정의 로그인 이메일은 " + user.getEmail() + " 입니다.");
        mailSender.send(message);
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
