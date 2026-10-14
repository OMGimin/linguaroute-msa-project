package com.lecture.user.repository;

import com.lecture.user.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findByEffectiveAtLessThanEqualOrderByTypeAscEffectiveAtDesc(LocalDateTime effectiveAt);
}
