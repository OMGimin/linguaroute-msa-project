package com.lecture.enrollment.repository;

import com.lecture.enrollment.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository
        extends JpaRepository<LessonProgress, Long> {

    /**
     * [추가] 특정 수강신청의 특정 차시 진행 기록 조회
     *
     * POST /api/enrollments/{enrollmentId}/lessons/{lessonId}/start
     * POST /api/enrollments/{enrollmentId}/lessons/{lessonId}/complete
     */
    Optional<LessonProgress> findByEnrollment_IdAndLessonId(
            Long enrollmentId,
            Long lessonId
    );

    /**
     * [추가] 특정 수강신청의 전체 차시 진행 기록 조회
     */
    List<LessonProgress> findByEnrollment_IdOrderByLessonIdAsc(
            Long enrollmentId
    );

    /**
     * [추가]
     * course-service에서 받은 필수 차시 ID 중
     * 완료된 차시 개수를 계산한다.
     *
     * 진도율 계산에 사용한다.
     */
    long countByEnrollment_IdAndLessonIdInAndStatus(
            Long enrollmentId,
            Collection<Long> lessonIds,
            LessonProgress.Status status
    );

    /**
     * [추가]
     * 지정한 차시 ID 목록의 진행 기록을 조회한다.
     */
    List<LessonProgress> findByEnrollment_IdAndLessonIdIn(
            Long enrollmentId,
            Collection<Long> lessonIds
    );
}