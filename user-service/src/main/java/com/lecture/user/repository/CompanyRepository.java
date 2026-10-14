package com.lecture.user.repository;

import com.lecture.user.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByBusinessNumber(String businessNumber);

    @Query("""
            select company from Company company
            where (:keyword is null or lower(company.name) like :keyword
                or company.businessNumber like :businessNumber)
              and (:status is null or company.status = :status)
            """)
    Page<Company> searchForPlatformAdmin(
            @Param("keyword") String keyword,
            @Param("businessNumber") String businessNumber,
            @Param("status") Company.Status status,
            Pageable pageable
    );
}
