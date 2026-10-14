package com.lecture.user.repository;

import com.lecture.user.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {
    boolean existsByUserIdAndTermId(Long userId, Long termId);
}
