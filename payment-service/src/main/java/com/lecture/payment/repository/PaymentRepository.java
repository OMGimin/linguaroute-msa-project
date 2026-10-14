package com.lecture.payment.repository;

import com.lecture.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCompanyIdOrderByRequestedAtDesc(Long companyId);

    Optional<Payment> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
}
