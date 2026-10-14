package com.lecture.course.repository;
import com.lecture.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LessonRepository extends JpaRepository<Lesson, Long> { List<Lesson> findByCourse_IdOrderBySequenceAsc(Long courseId); Optional<Lesson> findByIdAndCourse_Id(Long id, Long courseId); }
