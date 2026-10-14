package com.lecture.enrollment.service;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.LessonProgress;
import com.lecture.enrollment.kafka.EnrollmentKafkaProducer;
import com.lecture.enrollment.repository.EnrollmentRepository;
import com.lecture.enrollment.repository.LessonProgressRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnrollmentServiceEventTest {

    @Test
    void emitsCompletionEventOnlyOnFirstCompletionTransition() {
        EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
        CourseServiceClient courseServiceClient = mock(CourseServiceClient.class);
        UserAuthorizationClient authorizationClient = mock(UserAuthorizationClient.class);
        CompanyEntitlementClient entitlementClient = mock(CompanyEntitlementClient.class);
        EnrollmentKafkaProducer producer = mock(EnrollmentKafkaProducer.class);
        EnrollmentWriteService writeService = mock(EnrollmentWriteService.class);
        LessonProgressRepository lessonProgressRepository = mock(LessonProgressRepository.class);

        Enrollment enrollment = Enrollment.builder()
                .id(9001L).companyId(10L).userId(101L).courseId(9101L)
                .status(Enrollment.Status.ENROLLED).progressRate(BigDecimal.ZERO)
                .enrolledAt(LocalDateTime.now()).build();
        LessonProgress lessonProgress = LessonProgress.builder()
                .enrollment(enrollment).lessonId(910101L)
                .status(LessonProgress.Status.NOT_STARTED).build();

        when(authorizationClient.getAuthorizationContext(101L)).thenReturn(
                new UserAuthorizationClient.AuthorizationContext(101L, 10L, "EMPLOYEE", "ACTIVE"));
        when(enrollmentRepository.findByIdAndUserIdAndCompanyId(9001L, 101L, 10L))
                .thenReturn(Optional.of(enrollment));
        when(courseServiceClient.getLessons(9101L)).thenReturn(List.of(
                new CourseServiceClient.LessonInfo(910101L, true)));
        when(lessonProgressRepository.findByEnrollment_IdAndLessonId(9001L, 910101L))
                .thenReturn(Optional.of(lessonProgress));
        when(lessonProgressRepository.countByEnrollment_IdAndLessonIdInAndStatus(anyLong(), any(), any()))
                .thenReturn(1L);

        EnrollmentService service = new EnrollmentService(
                enrollmentRepository, courseServiceClient, authorizationClient, entitlementClient,
                producer, writeService, lessonProgressRepository, new ProgressCalculator());

        service.completeLesson(101L, 9001L, 910101L);
        service.completeLesson(101L, 9001L, 910101L);

        verify(producer, times(1)).publishEnrollmentCompleted(any());
    }
}
