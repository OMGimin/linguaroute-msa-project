package com.lecture.course.repository;

import com.lecture.course.entity.Course;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CourseSpecification {

    private CourseSpecification() {
    }

    public static Specification<Course> catalogSearch(
            String keyword,
            Course.Language language,
            Course.Situation situation,
            Course.Level level) {

        Specification<Course> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("status"), Course.Status.ACTIVE);

        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
        }
        if (language != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("language"), language));
        }
        if (situation != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("situation"), situation));
        }
        if (level != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("level"), level));
        }
        return specification;
    }

    public static Specification<Course> adminSearch(
            String keyword,
            Course.Language language,
            Course.Situation situation,
            Course.Level level,
            Course.Status status) {
        Specification<Course> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
        }
        if (language != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("language"), language));
        }
        if (situation != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("situation"), situation));
        }
        if (level != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("level"), level));
        }
        return specification;
    }
}
