package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.OutboxEvent;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.entity.Subscription;
import com.lecture.payment.exception.PaymentErrorCode;
import com.lecture.payment.exception.PaymentException;
import com.lecture.payment.repository.OutboxEventRepository;
import com.lecture.payment.repository.PaymentRepository;
import com.lecture.payment.repository.PlanPriceRepository;
import com.lecture.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionServiceTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PlanPriceRepository planPriceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    void mock_success_구독결제는_구독과_결제를_성공처리하고_outbox를_저장한다() {
        PlanPrice planPrice = firstPlanPrice();

        PaymentDto.SubscriptionResponse response = subscriptionService.createSubscription(
                10L,
                "idem-success-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success")
        );

        assertThat(response.getSubscriptionId()).isNotNull();
        assertThat(response.getPaymentId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Subscription.Status.ACTIVE);

        Payment payment = paymentRepository.findById(response.getPaymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCESS);
        assertThat(payment.getAmount()).isEqualTo(planPrice.getPrice());

        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        assertThat(event.getEventType()).isEqualTo("PaymentCompleted");
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
    }

    @Test
    void 같은_멱등성키는_새_결제를_생성하지_않고_기존_결과를_반환한다() {
        PlanPrice planPrice = firstPlanPrice();
        PaymentDto.CreateSubscriptionRequest request =
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success");

        PaymentDto.SubscriptionResponse first =
                subscriptionService.createSubscription(10L, "idem-repeat-1", request);
        PaymentDto.SubscriptionResponse second =
                subscriptionService.createSubscription(10L, "idem-repeat-1", request);

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(second.getSubscriptionId()).isEqualTo(first.getSubscriptionId());
        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    @Test
    void 멱등성키는_기업별로_분리된다() {
        PlanPrice planPrice = firstPlanPrice();
        PaymentDto.CreateSubscriptionRequest request =
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success");

        PaymentDto.SubscriptionResponse first =
                subscriptionService.createSubscription(10L, "idem-company-scope", request);
        PaymentDto.SubscriptionResponse second =
                subscriptionService.createSubscription(20L, "idem-company-scope", request);

        assertThat(second.getPaymentId()).isNotEqualTo(first.getPaymentId());
        assertThat(second.getSubscriptionId()).isNotEqualTo(first.getSubscriptionId());
        assertThat(paymentRepository.findAll()).hasSize(2);
    }

    @Test
    void mock_failure는_실패결제와_paymentFailed_outbox를_남긴다() {
        PlanPrice planPrice = firstPlanPrice();

        assertThatThrownBy(() -> subscriptionService.createSubscription(
                10L,
                "idem-failure-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-failure")
        )).isInstanceOf(PaymentException.class);

        Payment payment = paymentRepository.findByCompanyIdAndIdempotencyKey(10L, "idem-failure-1").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("결제가 승인되지 않았습니다.");

        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        assertThat(event.getEventType()).isEqualTo("PaymentFailed");
    }

    @Test
    void 잘못된_결제수단은_결제와_outbox를_생성하지_않는다() {
        PlanPrice planPrice = firstPlanPrice();

        assertThatThrownBy(() -> subscriptionService.createSubscription(
                10L,
                "idem-invalid-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "invalid-token")
        ))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_METHOD);

        assertThat(subscriptionRepository.findAll()).isEmpty();
        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void 구독해지는_자동갱신을_끄고_subscriptionCanceled_outbox를_저장한다() {
        PlanPrice planPrice = firstPlanPrice();
        subscriptionService.createSubscription(
                10L,
                "idem-cancel-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success")
        );
        outboxEventRepository.deleteAll();

        PaymentDto.SubscriptionResponse response = subscriptionService.cancelSubscription(
                10L,
                new PaymentDto.CancelSubscriptionRequest("교육 인원 감소")
        );

        assertThat(response.getStatus()).isEqualTo(Subscription.Status.ACTIVE);
        assertThat(response.getAutoRenew()).isFalse();

        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        assertThat(event.getEventType()).isEqualTo("SubscriptionCanceled");
    }

    @Test
    void 구독갱신은_조회된_구독을_다시_관리상태로_가져와_저장한다() {
        PlanPrice planPrice = firstPlanPrice();
        PaymentDto.SubscriptionResponse created = subscriptionService.createSubscription(
                10L,
                "idem-renew-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success")
        );
        Subscription subscription = subscriptionRepository.findById(created.getSubscriptionId()).orElseThrow();
        LocalDateTime renewalAt = subscription.getCurrentPeriodEnd();
        outboxEventRepository.deleteAll();

        subscriptionService.renewSubscription(subscription, renewalAt);

        Subscription renewed = subscriptionRepository.findById(created.getSubscriptionId()).orElseThrow();
        assertThat(renewed.getStatus()).isEqualTo(Subscription.Status.ACTIVE);
        assertThat(renewed.getCurrentPeriodStart()).isEqualTo(renewalAt);
        assertThat(renewed.getNextBillingAt()).isEqualTo(renewed.getCurrentPeriodEnd());
        assertThat(outboxEventRepository.findAll().getFirst().getEventType()).isEqualTo("SubscriptionRenewed");
    }

    @Test
    void 구독만료는_조회된_구독을_다시_관리상태로_가져와_저장한다() {
        PlanPrice planPrice = firstPlanPrice();
        PaymentDto.SubscriptionResponse created = subscriptionService.createSubscription(
                10L,
                "idem-expire-1",
                new PaymentDto.CreateSubscriptionRequest(planPrice.getId(), "mock-success")
        );
        Subscription subscription = subscriptionRepository.findById(created.getSubscriptionId()).orElseThrow();
        LocalDateTime expiredAt = subscription.getCurrentPeriodEnd();
        outboxEventRepository.deleteAll();

        subscriptionService.expireSubscription(subscription, expiredAt);

        Subscription expired = subscriptionRepository.findById(created.getSubscriptionId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(Subscription.Status.EXPIRED);
        assertThat(expired.getNextBillingAt()).isNull();
        assertThat(outboxEventRepository.findAll().getFirst().getEventType()).isEqualTo("SubscriptionExpired");
    }

    private PlanPrice firstPlanPrice() {
        return planPriceRepository.findByStatusOrderByIdAsc(PlanPrice.Status.ACTIVE)
                .getFirst();
    }
}
