package com.lecture.payment.repository;

import com.lecture.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(
            Long companyId,
            Subscription.Status status
    );

    boolean existsByCompanyIdAndStatus(Long companyId, Subscription.Status status);

    List<Subscription> findByStatusAndAutoRenewAndNextBillingAtLessThanEqual(
            Subscription.Status status,
            boolean autoRenew,
            LocalDateTime nextBillingAt
    );

    List<Subscription> findByStatusAndAutoRenewAndCurrentPeriodEndLessThanEqual(
            Subscription.Status status,
            boolean autoRenew,
            LocalDateTime currentPeriodEnd
    );
}
