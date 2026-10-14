package com.lecture.user.controller;

import com.lecture.user.entity.EmailVerification;
import com.lecture.user.repository.EmailVerificationRepository;
import com.lecture.user.security.TokenHash;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailVerificationControllerIntegrationTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void 이메일_인증_코드를_발송하고_가입용_일회성_토큰을_발급한다() throws Exception {
        mockMvc.perform(post("/api/users/register?action=request-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"New.Admin@company.com\",\"purpose\":\"SIGNUP\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.accepted").value(true));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        String verificationCode = extractVerificationCode(mailCaptor.getValue().getText());

        String response = mockMvc.perform(post("/api/users/register?action=confirm-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.admin@company.com\",\"verificationCode\":\"" + verificationCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailVerificationToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("emailVerificationToken").asText();
        EmailVerification verification = emailVerificationRepository.findAll().getFirst();
        assertThat(verification.getEmail()).isEqualTo("new.admin@company.com");
        assertThat(verification.getVerifiedAt()).isNotNull();
        assertThat(verification.getTokenHash()).isEqualTo(TokenHash.sha256(token));

        mockMvc.perform(post("/api/users/register?action=confirm-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.admin@company.com\",\"verificationCode\":\"" + verificationCode + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void 잘못된_인증_코드는_토큰을_발급하지_않는다() throws Exception {
        mockMvc.perform(post("/api/users/register?action=request-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong-code@company.com\",\"purpose\":\"SIGNUP\"}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/users/register?action=confirm-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong-code@company.com\",\"verificationCode\":\"000000\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void 같은_이메일의_인증_요청은_1분에_한번만_허용한다() throws Exception {
        String request = "{\"email\":\"limited@company.com\",\"purpose\":\"SIGNUP\"}";
        mockMvc.perform(post("/api/users/register?action=request-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/users/register?action=request-email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUEST_LIMIT"));
    }

    private String extractVerificationCode(String text) {
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
