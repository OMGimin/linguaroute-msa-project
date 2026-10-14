package com.lecture.payment.security;

import com.lecture.payment.exception.PaymentErrorCode;
import com.lecture.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PaymentRequestContext {

    private final UserAuthorizationClient userAuthorizationClient;

    public Long companyId(String userIdHeader) {
        Long userId = userId(userIdHeader);
        return userAuthorizationClient.requireCompanyAdminCompanyId(userId);
    }

    private Long userId(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            throw new PaymentException(PaymentErrorCode.INVALID_USER_CONTEXT);
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new PaymentException(PaymentErrorCode.INVALID_USER_CONTEXT);
        }
    }
}
