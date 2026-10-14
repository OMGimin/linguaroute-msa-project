package com.lecture.payment.dto;

import com.lecture.payment.entity.Payment;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.entity.Subscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public class PaymentDto {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private PaymentDto() {
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSubscriptionRequest {
        @NotNull(message = "요금제 가격 ID는 필수입니다")
        private Long planPriceId;

        @NotBlank(message = "결제 수단 토큰은 필수입니다")
        private String paymentMethodToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelSubscriptionRequest {
        private String reason;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PlanPriceResponse {
        private Long planPriceId;
        private String planName;
        private PlanPrice.BillingCycle billingCycle;
        private Integer seatLimit;
        private Long price;
        private String currency;

        public static PlanPriceResponse from(PlanPrice planPrice) {
            return PlanPriceResponse.builder()
                    .planPriceId(planPrice.getId())
                    .planName(planPrice.getPlan().getName())
                    .billingCycle(planPrice.getBillingCycle())
                    .seatLimit(planPrice.getSeatLimit())
                    .price(planPrice.getPrice())
                    .currency(planPrice.getCurrency())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class SubscriptionResponse {
        private Long subscriptionId;
        private Long paymentId;
        private Subscription.Status status;
        private OffsetDateTime currentPeriodStart;
        private OffsetDateTime currentPeriodEnd;
        private OffsetDateTime nextBillingAt;
        private Boolean autoRenew;

        public static SubscriptionResponse from(Subscription subscription, Long paymentId) {
            return SubscriptionResponse.builder()
                    .subscriptionId(subscription.getId())
                    .paymentId(paymentId)
                    .status(subscription.getStatus())
                    .currentPeriodStart(toOffsetDateTime(subscription.getCurrentPeriodStart()))
                    .currentPeriodEnd(toOffsetDateTime(subscription.getCurrentPeriodEnd()))
                    .nextBillingAt(toOffsetDateTime(subscription.getNextBillingAt()))
                    .autoRenew(subscription.isAutoRenew())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private Long paymentId;
        private Long subscriptionId;
        private Long companyId;
        private Long amount;
        private String currency;
        private Payment.Status status;
        private String providerPaymentId;
        private String failureReason;
        private OffsetDateTime requestedAt;
        private OffsetDateTime paidAt;
        private OffsetDateTime failedAt;

        public static PaymentResponse from(Payment payment) {
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .subscriptionId(payment.getSubscription() == null ? null : payment.getSubscription().getId())
                    .companyId(payment.getCompanyId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus())
                    .providerPaymentId(payment.getProviderPaymentId())
                    .failureReason(payment.getFailureReason())
                    .requestedAt(toOffsetDateTime(payment.getRequestedAt()))
                    .paidAt(toOffsetDateTime(payment.getPaidAt()))
                    .failedAt(toOffsetDateTime(payment.getFailedAt()))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private T data;
        private OffsetDateTime timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .data(data)
                    .timestamp(OffsetDateTime.now(SERVICE_ZONE_ID))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String code;
        private String message;
        private OffsetDateTime timestamp;

        public static ErrorResponse of(String code, String message) {
            return ErrorResponse.builder()
                    .code(code)
                    .message(message)
                    .timestamp(OffsetDateTime.now(SERVICE_ZONE_ID))
                    .build();
        }
    }

    public static class PaymentResponses {
        private PaymentResponses() {
        }

        public static List<PaymentResponse> from(List<Payment> payments) {
            return payments.stream()
                    .map(PaymentResponse::from)
                    .toList();
        }
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(SERVICE_ZONE_ID).toOffsetDateTime();
    }
}
