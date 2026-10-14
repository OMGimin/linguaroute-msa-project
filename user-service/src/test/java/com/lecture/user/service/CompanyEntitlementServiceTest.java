package com.lecture.user.service;

import com.lecture.user.entity.Company;
import com.lecture.user.entity.CompanyEntitlement;
import com.lecture.user.repository.CompanyEntitlementRepository;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.ProcessedEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CompanyEntitlementServiceTest {

    @Autowired
    private CompanyEntitlementService companyEntitlementService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyEntitlementRepository companyEntitlementRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("이벤트 검증 기업")
                .businessNumber(UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .status(Company.Status.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        companyEntitlementRepository.findByCompany_Id(company.getId())
                .ifPresent(companyEntitlementRepository::delete);
        companyRepository.deleteById(company.getId());
        processedEventRepository.deleteAll();
    }

    @Test
    void 결제실패_이벤트는_초기_이용권한을_생성하지_않고_중복수신도_한번만_기록한다() {
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "PaymentFailed",
                "occurredAt", OffsetDateTime.now(ZoneOffset.UTC).toString(),
                "companyId", company.getId(),
                "subscriptionId", 101L,
                "paymentId", 201L,
                "planPriceId", 1L,
                "failureCode", "CARD_DECLINED",
                "failureReason", "결제가 승인되지 않았습니다."
        );

        companyEntitlementService.processSubscriptionEvent(event);
        companyEntitlementService.processSubscriptionEvent(event);

        assertThat(companyEntitlementRepository.findByCompany_Id(company.getId())).isEmpty();
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void 갱신과_만료_이벤트는_권한상태와_이용기간을_순서대로_반영한다() {
        companyEntitlementService.processSubscriptionEvent(paymentCompletedEvent());
        companyEntitlementService.processSubscriptionEvent(subscriptionRenewedEvent());

        CompanyEntitlement renewed = companyEntitlementRepository.findByCompany_Id(company.getId()).orElseThrow();
        assertThat(renewed.getEntitlementStatus()).isEqualTo(CompanyEntitlement.Status.ACTIVE);
        assertThat(renewed.getSubscriptionId()).isEqualTo(102L);
        assertThat(renewed.getSeatLimit()).isEqualTo(30);
        assertThat(renewed.isAutoRenew()).isTrue();

        companyEntitlementService.processSubscriptionEvent(subscriptionExpiredEvent());

        CompanyEntitlement expired = companyEntitlementRepository.findByCompany_Id(company.getId()).orElseThrow();
        assertThat(expired.getEntitlementStatus()).isEqualTo(CompanyEntitlement.Status.EXPIRED);
        assertThat(expired.isAutoRenew()).isFalse();
        assertThat(processedEventRepository.count()).isEqualTo(3);
    }

    @Test
    void 구독해지_이벤트는_현재_이용기간을_유지하면서_자동갱신을_중지한다() {
        companyEntitlementService.processSubscriptionEvent(paymentCompletedEvent());
        companyEntitlementService.processSubscriptionEvent(Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "SubscriptionCanceled",
                "occurredAt", "2026-08-20T09:00:00+09:00",
                "companyId", company.getId(),
                "subscriptionId", 101L,
                "canceledAt", "2026-08-20T09:00:00+09:00",
                "effectiveAt", "2026-09-11T09:00:00+09:00"
        ));

        CompanyEntitlement canceled = companyEntitlementRepository.findByCompany_Id(company.getId()).orElseThrow();
        assertThat(canceled.getEntitlementStatus()).isEqualTo(CompanyEntitlement.Status.ACTIVE);
        assertThat(canceled.isAutoRenew()).isFalse();
        assertThat(canceled.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0));
    }

    private Map<String, Object> paymentCompletedEvent() {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "PaymentCompleted",
                "occurredAt", "2026-08-11T09:00:00+09:00",
                "companyId", company.getId(),
                "subscriptionId", 101L,
                "paymentId", 201L,
                "planPriceId", 1L,
                "seatLimit", 10,
                "currentPeriodEnd", "2026-09-11T09:00:00+09:00"
        );
    }

    private Map<String, Object> subscriptionRenewedEvent() {
        return Map.ofEntries(
                Map.entry("eventId", UUID.randomUUID().toString()),
                Map.entry("eventType", "SubscriptionRenewed"),
                Map.entry("occurredAt", "2026-09-11T09:00:00+09:00"),
                Map.entry("companyId", company.getId()),
                Map.entry("subscriptionId", 102L),
                Map.entry("paymentId", 202L),
                Map.entry("planPriceId", 2L),
                Map.entry("seatLimit", 30),
                Map.entry("currentPeriodStart", "2026-09-11T09:00:00+09:00"),
                Map.entry("currentPeriodEnd", "2026-10-11T09:00:00+09:00"),
                Map.entry("nextBillingAt", "2026-10-11T09:00:00+09:00")
        );
    }

    private Map<String, Object> subscriptionExpiredEvent() {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "SubscriptionExpired",
                "occurredAt", "2026-10-11T09:00:00+09:00",
                "companyId", company.getId(),
                "subscriptionId", 102L,
                "expiredAt", "2026-10-11T09:00:00+09:00"
        );
    }
}
