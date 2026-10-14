package com.lecture.payment.repository;

import com.lecture.payment.entity.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, Long> {

    List<PlanPrice> findByStatusOrderByIdAsc(PlanPrice.Status status);
}
