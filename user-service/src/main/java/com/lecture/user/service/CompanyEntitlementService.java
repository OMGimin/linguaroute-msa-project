package com.lecture.user.service;

import com.lecture.user.dto.CompanyDto;
import com.lecture.user.entity.Company;
import com.lecture.user.entity.CompanyEntitlement;
import com.lecture.user.entity.ProcessedEvent;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.CompanyEntitlementRepository;
import com.lecture.user.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyEntitlementService {

    private final CompanyRepository companyRepository;
    private final CompanyEntitlementRepository companyEntitlementRepository;
    private final ProcessedEventRepository processedEventRepository;

    public CompanyDto.EntitlementResponse getEntitlement(Long companyId) {
        CompanyEntitlement entitlement = companyEntitlementRepository.findByCompany_Id(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_ENTITLEMENT_NOT_FOUND));

        return new CompanyDto.EntitlementResponse(
                entitlement.getCompany().getId(),
                entitlement.getSubscriptionId(),
                entitlement.getEntitlementStatus().name(),
                entitlement.getSeatLimit(),
                entitlement.getCurrentPeriodEnd(),
                entitlement.isAutoRenew()
        );
    }

    @Transactional
    public void processSubscriptionEvent(Map<String, Object> event) {
        String eventId = requiredString(event, "eventId");
        String eventType = requiredString(event, "eventType");

        if (processedEventRepository.existsByEventId(eventId)) {
            return;
        }

        switch (eventType) {
            case "PaymentCompleted" -> activateEntitlement(event, true);
            case "SubscriptionRenewed" -> activateEntitlement(event, true);
            case "SubscriptionCanceled" -> cancelEntitlement(event);
            case "SubscriptionExpired" -> expireEntitlement(event);
            case "PaymentFailed" -> {
            }
            default -> throw new IllegalArgumentException("지원하지 않는 구독 이벤트입니다: " + eventType);
        }

        processedEventRepository.save(ProcessedEvent.create(
                eventId,
                eventType,
                toLocalDateTime(event.get("occurredAt")),
                LocalDateTime.now()
        ));
    }

    private void activateEntitlement(Map<String, Object> event, boolean autoRenew) {
        Long companyId = requiredLong(event, "companyId");
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMPANY_NOT_FOUND));

        Long subscriptionId = requiredLong(event, "subscriptionId");
        Integer seatLimit = requiredInteger(event, "seatLimit");
        LocalDateTime currentPeriodEnd = requiredDateTime(event, "currentPeriodEnd");

        CompanyEntitlement entitlement = companyEntitlementRepository.findByCompany_Id(companyId)
                .orElseGet(() -> CompanyEntitlement.active(
                        company,
                        subscriptionId,
                        seatLimit,
                        currentPeriodEnd,
                        autoRenew
                ));

        entitlement.activate(subscriptionId, seatLimit, currentPeriodEnd, autoRenew);
        companyEntitlementRepository.save(entitlement);
    }

    private void cancelEntitlement(Map<String, Object> event) {
        companyEntitlementRepository.findByCompany_Id(requiredLong(event, "companyId"))
                .ifPresent(entitlement -> entitlement.cancelAutoRenew(toLocalDateTime(event.get("effectiveAt"))));
    }

    private void expireEntitlement(Map<String, Object> event) {
        companyEntitlementRepository.findByCompany_Id(requiredLong(event, "companyId"))
                .ifPresent(entitlement -> entitlement.expire(toLocalDateTime(event.get("expiredAt"))));
    }

    private String requiredString(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("구독 이벤트에 " + key + " 값이 없습니다.");
        }
        return value.toString();
    }

    private Long requiredLong(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        throw new IllegalArgumentException("구독 이벤트에 " + key + " 값이 없습니다.");
    }

    private Integer requiredInteger(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        throw new IllegalArgumentException("구독 이벤트에 " + key + " 값이 없습니다.");
    }

    private LocalDateTime requiredDateTime(Map<String, Object> event, String key) {
        LocalDateTime value = toLocalDateTime(event.get(key));
        if (value == null) {
            throw new IllegalArgumentException("구독 이벤트에 " + key + " 값이 없습니다.");
        }
        return value;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        String text = value.toString();
        if (text.endsWith("Z") || text.contains("+")) {
            return OffsetDateTime.parse(text).toLocalDateTime();
        }
        return LocalDateTime.parse(text);
    }
}
