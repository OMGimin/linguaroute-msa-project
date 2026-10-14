package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.exception.EnrollmentException;
import com.lecture.enrollment.exception.ErrorCode;
import com.lecture.enrollment.kafka.EnrollmentKafkaProducer;
import com.lecture.enrollment.kafka.KafkaEvent;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.lecture.enrollment.entity.LessonProgress;
import com.lecture.enrollment.repository.LessonProgressRepository;
import com.lecture.enrollment.service.CourseServiceClient.LessonInfo;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final UserAuthorizationClient userAuthorizationClient;
    private final CompanyEntitlementClient companyEntitlementClient;
    private final EnrollmentKafkaProducer enrollmentKafkaProducer;
    private final EnrollmentWriteService enrollmentWriteService;
    private final LessonProgressRepository lessonProgressRepository;
    private final ProgressCalculator progressCalculator;

    @Transactional
    public EnrollmentDto.EnrollResponse enroll(Long userId, Long courseId) {
        UserAuthorizationClient.AuthorizationContext authorization =
                userAuthorizationClient.getAuthorizationContext(userId);

        if (!authorization.isActiveEmployee()) {
            throw new EnrollmentException(ErrorCode.FORBIDDEN);
        }

        CompanyEntitlementClient.Entitlement entitlement =
                companyEntitlementClient.getEntitlement(authorization.getCompanyId());

        if (!entitlement.isActive()) {
            throw new EnrollmentException(ErrorCode.SUBSCRIPTION_INACTIVE);
        }

        courseServiceClient.validateEnrollable(courseId);

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new EnrollmentException(ErrorCode.DUPLICATE_ENROLLMENT);
        }

        Enrollment enrollment = enrollmentWriteService.createEnrollment(
                authorization.getCompanyId(),
                userId,
                courseId
        );

        log.info("[EnrollmentService] 수강신청 완료 - enrollmentId: {}", enrollment.getId());
        return EnrollmentDto.EnrollResponse.from(enrollment);
    }

    public List<EnrollmentDto.EnrollmentSummaryResponse> getEnrollmentsByUser(Long userId) {
        requireActiveEmployee(userId);
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId)
                .stream()
                .map(EnrollmentDto.EnrollmentSummaryResponse::from)
                .toList();
    }

    public EnrollmentDto.EnrollmentDetailResponse getEnrollmentDetail(Long userId, Long enrollmentId) {
        Enrollment enrollment = findActiveEmployeeEnrollment(userId, enrollmentId);
        return EnrollmentDto.EnrollmentDetailResponse.from(enrollment,
                lessonProgressRepository.findByEnrollment_IdOrderByLessonIdAsc(enrollmentId));
    }

    @Transactional
    public EnrollmentDto.LessonActionResponse startLesson(Long userId, Long enrollmentId, Long lessonId) {
        Enrollment enrollment = findActiveEmployeeEnrollment(userId, enrollmentId);
        LessonProgress progress = findOrCreateLessonProgress(enrollment, lessonId);
        progress.start(java.time.LocalDateTime.now());
        enrollment.startLearning(java.time.LocalDateTime.now());
        updateProgress(enrollment);
        return EnrollmentDto.LessonActionResponse.from(enrollment, progress);
    }

    @Transactional
    public EnrollmentDto.LessonActionResponse completeLesson(Long userId, Long enrollmentId, Long lessonId) {
        Enrollment enrollment = findActiveEmployeeEnrollment(userId, enrollmentId);
        boolean wasCompleted = enrollment.getStatus() == Enrollment.Status.COMPLETED;
        LessonProgress progress = findOrCreateLessonProgress(enrollment, lessonId);
        progress.complete(java.time.LocalDateTime.now());
        updateProgress(enrollment);
        if (!wasCompleted && enrollment.getStatus() == Enrollment.Status.COMPLETED) {
            enrollmentKafkaProducer.publishEnrollmentCompleted(
                    KafkaEvent.EnrollmentCompletedEvent.builder()
                            .eventId(java.util.UUID.randomUUID().toString())
                            .eventType("EnrollmentCompleted")
                            .occurredAt(java.time.LocalDateTime.now())
                            .enrollmentId(enrollment.getId())
                            .userId(enrollment.getUserId())
                            .courseId(enrollment.getCourseId())
                            .build()
            );
        }
        return EnrollmentDto.LessonActionResponse.from(enrollment, progress);
    }

    public EnrollmentDto.PageResponse<EnrollmentDto.ManagementEnrollmentResponse> getCompanyEnrollments(
            Long adminId, int page, int size) {
        UserAuthorizationClient.AuthorizationContext authorization = userAuthorizationClient.getAuthorizationContext(adminId);
        if (!authorization.isActiveCompanyAdmin()) {
            throw new EnrollmentException(ErrorCode.FORBIDDEN);
        }
        return managementPage(authorization.getCompanyId(), page, size);
    }

    public EnrollmentDto.PageResponse<EnrollmentDto.ManagementEnrollmentResponse> getAllForPlatformAdmin(
            Long adminId, int page, int size) {
        UserAuthorizationClient.AuthorizationContext authorization = userAuthorizationClient.getAuthorizationContext(adminId);
        if (!authorization.isActivePlatformAdmin()) {
            throw new EnrollmentException(ErrorCode.FORBIDDEN);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return EnrollmentDto.PageResponse.from(enrollmentRepository.findAll(pageable)
                .map(EnrollmentDto.ManagementEnrollmentResponse::from));
    }

    private EnrollmentDto.PageResponse<EnrollmentDto.ManagementEnrollmentResponse> managementPage(
            Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<EnrollmentDto.ManagementEnrollmentResponse> result = enrollmentRepository.findByCompanyId(companyId, pageable)
                .map(EnrollmentDto.ManagementEnrollmentResponse::from);
        return EnrollmentDto.PageResponse.from(result);
    }

    private void requireActiveEmployee(Long userId) {
        if (!userAuthorizationClient.getAuthorizationContext(userId).isActiveEmployee()) {
            throw new EnrollmentException(ErrorCode.FORBIDDEN);
        }
    }

    private Enrollment findActiveEmployeeEnrollment(Long userId, Long enrollmentId) {
        UserAuthorizationClient.AuthorizationContext authorization = userAuthorizationClient.getAuthorizationContext(userId);
        if (!authorization.isActiveEmployee()) {
            throw new EnrollmentException(ErrorCode.FORBIDDEN);
        }
        return enrollmentRepository.findByIdAndUserIdAndCompanyId(enrollmentId, userId, authorization.getCompanyId())
                .orElseThrow(() -> new EnrollmentException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    private LessonProgress findOrCreateLessonProgress(Enrollment enrollment, Long lessonId) {
        List<LessonInfo> lessons = courseServiceClient.getLessons(enrollment.getCourseId());
        boolean lessonInCourse = lessons.stream().anyMatch(lesson -> lesson.lessonId().equals(lessonId));
        if (!lessonInCourse) {
            throw new EnrollmentException(ErrorCode.LESSON_NOT_IN_COURSE);
        }
        return lessonProgressRepository.findByEnrollment_IdAndLessonId(enrollment.getId(), lessonId)
                .orElseGet(() -> lessonProgressRepository.save(LessonProgress.create(enrollment, lessonId)));
    }

    private void updateProgress(Enrollment enrollment) {
        List<LessonInfo> lessons = courseServiceClient.getLessons(enrollment.getCourseId());
        Set<Long> requiredLessonIds = lessons.stream().filter(LessonInfo::required)
                .map(LessonInfo::lessonId).collect(Collectors.toSet());
        long completed = requiredLessonIds.isEmpty() ? 0 : lessonProgressRepository
                .countByEnrollment_IdAndLessonIdInAndStatus(enrollment.getId(), requiredLessonIds, LessonProgress.Status.COMPLETED);
        enrollment.updateProgress(progressCalculator.calculate(completed, requiredLessonIds.size()));
        if (progressCalculator.isCompleted(completed, requiredLessonIds.size())) {
            enrollment.complete(java.time.LocalDateTime.now());
        } else if (enrollment.getStatus() == Enrollment.Status.ENROLLED) {
            enrollment.startLearning(java.time.LocalDateTime.now());
        }
    }

    public EnrollmentDto.EnrollmentHistoryResponse getEnrollmentHistory(Long userId) {
        List<Long> activeCourseIds = enrollmentRepository
                .findByUserIdAndStatusIn(
                        userId,
                        List.of(
                                Enrollment.Status.ENROLLED,
                                Enrollment.Status.LEARNING,
                                Enrollment.Status.COMPLETED
                        )
                )
                .stream()
                .map(Enrollment::getCourseId)
                .toList();

        return EnrollmentDto.EnrollmentHistoryResponse.builder()
                .userId(userId)
                .activeCourseIds(activeCourseIds)
                .build();
    }
}
