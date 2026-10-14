package com.lecture.user.repository;

import com.lecture.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByNameAndCompany_BusinessNumberAndStatus(String name, String businessNumber, User.Status status);
    List<User> findAllByCompany_IdAndBusinessRoleOrderByCreatedAtDesc(Long companyId, User.BusinessRole businessRole);
    Optional<User> findByIdAndCompany_IdAndBusinessRole(Long id, Long companyId, User.BusinessRole businessRole);
    long countByCompany_IdAndBusinessRoleAndStatus(Long companyId, User.BusinessRole businessRole, User.Status status);

    @Query(
            value = """
                    select user from User user
                    left join fetch user.company company
                    where (:keyword is null
                        or lower(user.name) like :keyword
                        or lower(user.email) like :keyword
                        or lower(company.name) like :keyword)
                      and (:businessRole is null or user.businessRole = :businessRole)
                      and (:status is null or user.status = :status)
                    """,
            countQuery = """
                    select count(user) from User user
                    left join user.company company
                    where (:keyword is null
                        or lower(user.name) like :keyword
                        or lower(user.email) like :keyword
                        or lower(company.name) like :keyword)
                      and (:businessRole is null or user.businessRole = :businessRole)
                      and (:status is null or user.status = :status)
                    """
    )
    Page<User> searchForPlatformAdmin(
            @Param("keyword") String keyword,
            @Param("businessRole") User.BusinessRole businessRole,
            @Param("status") User.Status status,
            Pageable pageable
    );
}
