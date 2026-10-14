package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.dto.UserDto;
import com.lecture.user.entity.User;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    /**
     * 사용자 단건 조회
     */
    public UserDto.UserResponse getUserById(Long id) {
        User user = findUser(id);
        validateActive(user);
        return UserDto.UserResponse.from(user);
    }

    public UserDto.AuthorizationContextResponse getAuthorizationContext(Long id) {
        return UserDto.AuthorizationContextResponse.from(findUser(id));
    }

    @Transactional
    public UserDto.UserResponse updateMe(Long userId, UserDto.UpdateRequest request) {
        User user = findUser(userId);
        validateActive(user);
        user.updateName(request.getName().trim());
        return UserDto.UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, AuthDto.PasswordChangeRequest request) {
        User user = findUser(userId);
        validateActive(user);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findUser(userId);
        validateActive(user);
        user.changePassword(passwordEncoder.encode(generateUnusablePassword()));
        user.changeStatus(User.Status.WITHDRAWN);
    }

    /**
     * 이메일로 사용자 조회 (서비스 간 내부 호출용)
     */
    public UserDto.UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return UserDto.UserResponse.from(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateActive(User user) {
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
    }

    private String generateUnusablePassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
