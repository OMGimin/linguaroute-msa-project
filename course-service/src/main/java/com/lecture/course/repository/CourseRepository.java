package com.lecture.course.repository;

import com.lecture.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // 언어별 강의 조회 (추천 서비스 사용)
    List<Course> findByLanguageAndStatus(Course.Language language, Course.Status status);

    // 활성 강의 전체 조회
    List<Course> findByStatus(Course.Status status);

    boolean existsByIdAndStatus(Long id, Course.Status status);

    // 언어별 + 특정 ID 제외 조회 (추천 서비스: 이미 수강한 강의 제외)
    List<Course> findByLanguageAndStatusAndIdNotIn(
            Course.Language language,
            Course.Status status,
            List<Long> excludeIds
    );
}
