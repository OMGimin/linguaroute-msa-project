package com.lecture.user.controller;

import com.lecture.user.entity.Company;
import com.lecture.user.entity.User;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountRecoveryControllerIntegrationTest {

    private static final Pattern RESET_TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private JavaMailSender mailSender;

    private User user;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.save(Company.builder()
                .name("테스트기업").businessNumber("1234567890").status(Company.Status.ACTIVE).build());
        user = userRepository.save(User.builder()
                .email("member@test.com").password(passwordEncoder.encode("Password123!"))
                .name("테스트사용자").role(User.AuthRole.STUDENT).businessRole(User.BusinessRole.EMPLOYEE)
                .company(company).status(User.Status.ACTIVE).build());
    }

    @Test
    void Gateway_공개_별칭으로_활성_약관을_조회한다() throws Exception {
        mockMvc.perform(get("/api/users/register?action=active-terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void Gateway_공개_별칭의_직원가입은_인증없이_컨트롤러에_도달한다() throws Exception {
        mockMvc.perform(post("/api/users/register?action=employee-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비밀번호_재설정_링크는_한번만_사용할_수_있다() throws Exception {
        mockMvc.perform(post("/api/users/register?action=request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"member@test.com\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.accepted").value(true));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        String token = extractResetToken(captor.getValue().getText());

        String request = "{\"resetToken\":\"" + token + "\",\"newPassword\":\"NewPassword123!\"}";
        mockMvc.perform(post("/api/users/register?action=confirm-password-reset")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("NewPassword123!", userRepository.findById(user.getId()).orElseThrow().getPassword())).isTrue();

        mockMvc.perform(post("/api/users/register?action=confirm-password-reset")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"));
    }

    @Test
    void 아이디_찾기는_일치_여부를_노출하지_않고_등록된_이메일로만_보낸다() throws Exception {
        mockMvc.perform(post("/api/users/register?action=request-id-find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트사용자\",\"businessNumber\":\"123-45-67890\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.accepted").value(true));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("member@test.com");
    }

    @Test
    void 보호된_내정보_비밀번호변경과_탈퇴를_처리한다() throws Exception {
        mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(user.getId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("member@test.com"));

        mockMvc.perform(put("/api/users/me/password")
                        .with(jwt().jwt(token -> token.subject(user.getId().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"Changed123!\"}"))
                .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("Changed123!", userRepository.findById(user.getId()).orElseThrow().getPassword())).isTrue();

        mockMvc.perform(delete("/api/users/me").with(jwt().jwt(token -> token.subject(user.getId().toString()))))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(User.Status.WITHDRAWN);

        mockMvc.perform(get("/api/users/me").with(jwt().jwt(token -> token.subject(user.getId().toString()))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_INACTIVE"));
    }

    private String extractResetToken(String text) {
        Matcher matcher = RESET_TOKEN_PATTERN.matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
