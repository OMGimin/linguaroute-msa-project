package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public List<PaymentDto.PaymentResponse> getPayments(Long companyId) {
        return PaymentDto.PaymentResponses.from(
                paymentRepository.findByCompanyIdOrderByRequestedAtDesc(companyId)
        );
    }

    public List<PaymentDto.PaymentResponse> getAllPayments() {
        return PaymentDto.PaymentResponses.from(paymentRepository.findAll());
    }
}
