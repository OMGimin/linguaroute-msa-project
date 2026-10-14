package com.lecture.course.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.course.entity.Course;
import com.lecture.course.repository.CourseRepository;
import com.lecture.course.service.UserAuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAuthorizationClient userAuthorizationClient;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        courseRepository.save(Course.builder()
                .title("해외 고객 미팅 영어")
                .description("비즈니스 미팅 과정")
                .language(Course.Language.ENGLISH)
                .situation(Course.Situation.CUSTOMER_MEETING)
                .level(Course.Level.INTERMEDIATE)
                .status(Course.Status.ACTIVE)
                .build());
        courseRepository.save(Course.builder()
                .title("비활성 일본어")
                .description("비공개 과정")
                .language(Course.Language.JAPANESE)
                .situation(Course.Situation.DAILY_CONVERSATION)
                .level(Course.Level.BEGINNER)
                .status(Course.Status.INACTIVE)
                .build());
    }

    @Test
    void 목록검색필터는_조건에_맞는_활성강의만_반환한다() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .with(jwt())
                        .param("keyword", "미팅")
                        .param("language", "ENGLISH")
                        .param("situation", "CUSTOMER_MEETING")
                        .param("level", "INTERMEDIATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("해외 고객 미팅 영어"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void 비활성강의_상세는_조회할수없다() throws Exception {
        Long inactiveId = courseRepository.findByStatus(Course.Status.INACTIVE).getFirst().getId();

        mockMvc.perform(get("/api/courses/{courseId}", inactiveId).with(jwt()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COURSE_INACTIVE"));
    }

    @Test
    void 플랫폼관리자는_비활성강의_상세를_조회할수있다() throws Exception {
        Long inactiveId = courseRepository.findByStatus(Course.Status.INACTIVE).getFirst().getId();

        mockMvc.perform(get("/api/admin/courses/{courseId}", inactiveId)
                        .with(jwt())
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(inactiveId))
                .andExpect(jsonPath("$.data.title").value("비활성 일본어"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(userAuthorizationClient).requirePlatformAdmin(1L);
    }

    @Test
    void 플랫폼관리자는_강의를_등록수정비활성화한다() throws Exception {
        String createBody = """
                {"title":"출장 중국어","description":"출장 회화","language":"CHINESE",
                 "situation":"BUSINESS_TRIP","level":"ELEMENTARY"}
                """;

        String response = mockMvc.perform(post("/api/admin/courses")
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        Long courseId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/admin/courses/{courseId}", courseId)
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"출장 중국어 실전","description":"수정됨","language":"CHINESE",
                                 "situation":"BUSINESS_TRIP","level":"INTERMEDIATE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("출장 중국어 실전"));

        mockMvc.perform(post("/api/admin/courses/{courseId}", courseId)
                        .param("action", "update-course")
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"출장 중국어 브라우저","description":"브라우저 수정","language":"CHINESE",
                                 "situation":"BUSINESS_TRIP","level":"INTERMEDIATE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("출장 중국어 브라우저"));

        mockMvc.perform(patch("/api/admin/courses/{courseId}/status", courseId)
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get("/api/admin/courses")
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("출장 중국어 브라우저"))
                .andExpect(jsonPath("$.data.content[0].status").value("INACTIVE"));

        mockMvc.perform(post("/api/admin/courses/{courseId}/status", courseId)
                        .param("action", "update-status")
                        .with(jwt())
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(userAuthorizationClient, times(6)).requirePlatformAdmin(1L);
    }
}
