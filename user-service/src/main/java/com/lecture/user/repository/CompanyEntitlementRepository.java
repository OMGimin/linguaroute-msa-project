package com.lecture.user.repository;

import com.lecture.user.entity.CompanyEntitlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyEntitlementRepository extends JpaRepository<CompanyEntitlement, Long> {

    Optional<CompanyEntitlement> findByCompany_Id(Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entitlement from CompanyEntitlement entitlement where entitlement.company.id = :companyId")
    Optional<CompanyEntitlement> findByCompanyIdForUpdate(@Param("companyId") Long companyId);
}
