package com.lecture.enrollment.repository;

import com.lecture.enrollment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long> {

    /**
     * [변경] 로그인한 직원의 수강 목록을 최근 신청순으로 조회
     *
     * GET /api/enrollments/me
     */
    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(
            Long userId
    );

    /**
     * [추가] 수강 ID와 사용자 ID를 함께 확인하여 상세 조회
     *
     * GET /api/enrollments/{enrollmentId}
     */
    Optional<Enrollment> findByIdAndUserId(
            Long enrollmentId,
            Long userId
    );

    /**
     * [추가] 수강 ID, 사용자 ID, 기업 ID를 함께 확인
     *
     * 직원의 수강 소유권과 기업 소속을 함께 검증할 때 사용한다.
     */
    Optional<Enrollment> findByIdAndUserIdAndCompanyId(
            Long enrollmentId,
            Long userId,
            Long companyId
    );

    /**
     * [유지] 동일 사용자의 동일 강의 중복 신청 확인
     *
     * POST /api/enrollments
     */
    boolean existsByUserIdAndCourseId(
            Long userId,
            Long courseId
    );

    /**
     * [유지] 사용자와 강의로 기존 수강신청 조회
     */
    Optional<Enrollment> findByUserIdAndCourseId(
            Long userId,
            Long courseId
    );

    /**
     * [추가] 기업별 직원 수강 상태 페이지 조회
     *
     * GET /api/companies/me/enrollments
     * GET /api/companies/me/enrollments/progress
     */
    Page<Enrollment> findByCompanyId(
            Long companyId,
            Pageable pageable
    );

    /**
     * [추가] 기업 내 특정 직원의 수강 상태 페이지 조회
     */
    Page<Enrollment> findByCompanyIdAndUserId(
            Long companyId,
            Long userId,
            Pageable pageable
    );

    /**
     * [변경] 특정 사용자의 특정 수강 상태 목록 조회
     *
     * 기존 ACTIVE 상태는 제거됐으므로
     * ENROLLED, LEARNING, COMPLETED 중 하나를 사용한다.
     */
    List<Enrollment> findByUserIdAndStatus(
            Long userId,
            Enrollment.Status status
    );

    /**
     * [통합 확인 필요]
     * 추천 서비스가 기존 수강 이력 API를 사용하는지 확인한다.
     *
     * 이미 신청한 강의를 추천에서 제외해야 한다면
     * ENROLLED, LEARNING, COMPLETED 상태를 함께 조회한다.
     */
    List<Enrollment> findByUserIdAndStatusIn(
            Long userId,
            List<Enrollment.Status> statuses
    );
}