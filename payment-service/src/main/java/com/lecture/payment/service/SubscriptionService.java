package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.entity.Subscription;
import com.lecture.payment.exception.PaymentErrorCode;
import com.lecture.payment.exception.PaymentException;
import com.lecture.payment.repository.PaymentRepository;
import com.lecture.payment.repository.PlanPriceRepository;
import com.lecture.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final PlanPriceRepository planPriceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final MockPaymentProcessor mockPaymentProcessor;
    private final OutboxEventService outboxEventService;

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentDto.SubscriptionResponse createSubscription(
            Long companyId,
            String idempotencyKey,
            PaymentDto.CreateSubscriptionRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);

        var existingPayment = paymentRepository.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (payment.getStatus() == Payment.Status.SUCCESS && payment.getSubscription() != null) {
                return PaymentDto.SubscriptionResponse.from(payment.getSubscription(), payment.getId());
            }
            throw new PaymentException(PaymentErrorCode.PAYMENT_FAILED, payment.getFailureReason());
        }

        if (subscriptionRepository.existsByCompanyIdAndStatus(companyId, Subscription.Status.ACTIVE)) {
            throw new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
        }

        PlanPrice planPrice = planPriceRepository.findById(request.getPlanPriceId())
                .filter(price -> price.getStatus() == PlanPrice.Status.ACTIVE)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PLAN_PRICE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        MockPaymentProcessor.PaymentProcessResult result =
                mockPaymentProcessor.process(request.getPaymentMethodToken());

        if (!result.valid()) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_METHOD);
        }

        Subscription subscription = subscriptionRepository.save(Subscription.pending(companyId, planPrice, now));
        Payment payment = paymentRepository.save(Payment.pending(
                companyId,
                idempotencyKey,
                planPrice.getPrice(),
                planPrice.getCurrency(),
                now
        ));

        if (!result.success()) {
            payment.fail(result.failureReason(), now);
            outboxEventService.savePaymentFailed(
                    companyId,
                    subscription.getId(),
                    payment,
                    planPrice.getId(),
                    result.failureCode(),
                    result.failureReason()
            );
            throw new PaymentException(PaymentErrorCode.PAYMENT_FAILED, result.failureReason());
        }

        LocalDateTime periodEnd = calculatePeriodEnd(now, planPrice.getBillingCycle());
        subscription.activate(now, periodEnd);
        payment.succeed(subscription, result.providerPaymentId(), now);
        outboxEventService.savePaymentCompleted(subscription, payment);

        return PaymentDto.SubscriptionResponse.from(subscription, payment.getId());
    }

    public PaymentDto.SubscriptionResponse getMySubscription(Long companyId) {
        Subscription subscription = subscriptionRepository
                .findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, Subscription.Status.ACTIVE)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        return PaymentDto.SubscriptionResponse.from(subscription, null);
    }

    @Transactional
    public PaymentDto.SubscriptionResponse cancelSubscription(Long companyId, PaymentDto.CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository
                .findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, Subscription.Status.ACTIVE)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));

        LocalDateTime canceledAt = LocalDateTime.now();
        subscription.cancel(canceledAt);
        outboxEventService.saveSubscriptionCanceled(subscription, canceledAt);

        return PaymentDto.SubscriptionResponse.from(subscription, null);
    }

    @Transactional
    public void renewSubscription(Subscription subscription, LocalDateTime renewalAt) {
        Subscription renewingSubscription = subscriptionRepository.findById(subscription.getId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        PlanPrice planPrice = renewingSubscription.getPlanPrice();
        Payment payment = paymentRepository.save(Payment.pending(
                renewingSubscription.getCompanyId(),
                "renewal-" + renewingSubscription.getId() + "-" + renewalAt,
                planPrice.getPrice(),
                planPrice.getCurrency(),
                renewalAt
        ));
        String providerPaymentId = "mock-renewal-" + UUID.randomUUID();
        LocalDateTime periodEnd = calculatePeriodEnd(renewalAt, planPrice.getBillingCycle());
        renewingSubscription.renew(planPrice, renewalAt, periodEnd);
        payment.succeed(renewingSubscription, providerPaymentId, renewalAt);
        outboxEventService.saveSubscriptionRenewed(renewingSubscription, payment);
    }

    @Transactional
    public void expireSubscription(Subscription subscription, LocalDateTime expiredAt) {
        Subscription expiringSubscription = subscriptionRepository.findById(subscription.getId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        expiringSubscription.expire(expiredAt);
        outboxEventService.saveSubscriptionExpired(expiringSubscription, expiredAt);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new PaymentException(PaymentErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
    }

    private LocalDateTime calculatePeriodEnd(LocalDateTime start, PlanPrice.BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> start.plusMonths(1);
            case YEARLY -> start.plusYears(1);
        };
    }
}
