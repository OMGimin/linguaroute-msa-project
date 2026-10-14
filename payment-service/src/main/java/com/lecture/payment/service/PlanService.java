package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.PlanPrice;
import com.lecture.payment.repository.PlanPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanPriceRepository planPriceRepository;

    public List<PaymentDto.PlanPriceResponse> getActivePlans() {
        return planPriceRepository.findByStatusOrderByIdAsc(PlanPrice.Status.ACTIVE)
                .stream()
                .map(PaymentDto.PlanPriceResponse::from)
                .toList();
    }
}
