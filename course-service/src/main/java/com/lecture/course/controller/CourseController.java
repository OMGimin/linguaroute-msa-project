package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * GET /courses - 전체 강의 목록
     */
    @GetMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CoursePageResponse>> getAllCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Course.Language language,
            @RequestParam(required = false) Course.Situation situation,
            @RequestParam(required = false) Course.Level level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.searchCourses(
                        keyword, language, situation, level,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))))
        );
    }

    /**
     * GET /courses/{id} - 강의 상세
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> getCourse(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getCourse(id))
        );
    }

    @GetMapping("/{id}/lessons")
    public ResponseEntity<CourseDto.ApiResponse<java.util.List<CourseDto.LessonResponse>>> getLessons(
            @PathVariable Long id) {
        return ResponseEntity.ok(CourseDto.ApiResponse.success(courseService.getLessons(id)));
    }

}
