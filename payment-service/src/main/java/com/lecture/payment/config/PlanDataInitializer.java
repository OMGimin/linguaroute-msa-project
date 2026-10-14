package com.lecture.payment.config;

import com.lecture.payment.entity.Plan;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlanDataInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (planRepository.existsByName("BUSINESS_50")) {
            return;
        }

        Plan plan = Plan.builder()
                .name("BUSINESS_50")
                .description("50 seats business language training plan")
                .status(Plan.Status.ACTIVE)
                .build();

        plan.addPrice(PlanPrice.builder()
                .billingCycle(PlanPrice.BillingCycle.MONTHLY)
                .price(299000L)
                .currency("KRW")
                .seatLimit(50)
                .status(PlanPrice.Status.ACTIVE)
                .build());

        plan.addPrice(PlanPrice.builder()
                .billingCycle(PlanPrice.BillingCycle.YEARLY)
                .price(2990000L)
                .currency("KRW")
                .seatLimit(50)
                .status(PlanPrice.Status.ACTIVE)
                .build());

        planRepository.save(plan);
    }
}
