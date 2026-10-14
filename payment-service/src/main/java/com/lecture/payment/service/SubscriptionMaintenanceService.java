package com.lecture.payment.service;

import com.lecture.payment.entity.Subscription;
import com.lecture.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionMaintenanceService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Scheduled(fixedDelayString = "${app.subscription.maintenance-delay-ms:60000}")
    public void processDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        subscriptionRepository
                .findByStatusAndAutoRenewAndNextBillingAtLessThanEqual(Subscription.Status.ACTIVE, true, now)
                .forEach(subscription -> {
                    log.info("[SubscriptionMaintenance] 구독 갱신 처리 - subscriptionId: {}", subscription.getId());
                    subscriptionService.renewSubscription(subscription, now);
                });

        subscriptionRepository
                .findByStatusAndAutoRenewAndCurrentPeriodEndLessThanEqual(Subscription.Status.ACTIVE, false, now)
                .forEach(subscription -> {
                    log.info("[SubscriptionMaintenance] 구독 만료 처리 - subscriptionId: {}", subscription.getId());
                    subscriptionService.expireSubscription(subscription, now);
                });
    }
}
