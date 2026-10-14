package com.lecture.payment.service;

import com.lecture.payment.entity.OutboxEvent;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.entity.Subscription;
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

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionMaintenanceServiceTest {

    @Autowired
    private SubscriptionMaintenanceService subscriptionMaintenanceService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlanPriceRepository planPriceRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    void 자동갱신_대상은_갱신하고_해지후_종료일이_지난_대상은_만료한다() {
        PlanPrice planPrice = planPriceRepository.findByStatusOrderByIdAsc(PlanPrice.Status.ACTIVE).getFirst();
        LocalDateTime dueAt = LocalDateTime.now().minusMinutes(1);

        Subscription renewing = subscriptionRepository.save(
                Subscription.active(99001L, planPrice, dueAt.minusMonths(1), dueAt));
        Subscription expiring = Subscription.active(99002L, planPrice, dueAt.minusMonths(1), dueAt);
        expiring.cancel(dueAt.minusSeconds(1));
        expiring = subscriptionRepository.save(expiring);

        subscriptionMaintenanceService.processDueSubscriptions();

        assertThat(subscriptionRepository.findById(renewing.getId()).orElseThrow().getStatus())
                .isEqualTo(Subscription.Status.ACTIVE);
        assertThat(subscriptionRepository.findById(renewing.getId()).orElseThrow().getNextBillingAt())
                .isAfter(LocalDateTime.now().minusSeconds(5));
        assertThat(subscriptionRepository.findById(expiring.getId()).orElseThrow().getStatus())
                .isEqualTo(Subscription.Status.EXPIRED);
        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("SubscriptionRenewed", "SubscriptionExpired");
    }
}
