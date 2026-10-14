package com.lecture.enrollment.service;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentWriteService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * 반드시 독립 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Enrollment createEnrollment(Long companyId, Long userId, Long courseId) {

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.create(companyId, userId, courseId, LocalDateTime.now())
        );

        log.info("[EnrollmentWriteService] enrollment 생성 완료 - enrollmentId: {}, companyId: {}, userId: {}, courseId: {}",
                enrollment.getId(), companyId, userId, courseId);

        return enrollment;
    }
}
