package com.lecture.user.security;

import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    public Long userId(Jwt jwt) {
        try {
            Object userId = jwt.getClaim("userId");
            if (userId == null) {
                userId = jwt.getClaim("user_id");
            }
            return userId == null ? Long.valueOf(jwt.getSubject()) : Long.valueOf(userId.toString());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
