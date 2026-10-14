package com.lecture.user.repository;

import com.lecture.user.entity.Invitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    boolean existsByCodeHash(String codeHash);

    List<Invitation> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invitation> findByCodeHash(String codeHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invitation> findByIdAndCompany_Id(Long invitationId, Long companyId);
}
