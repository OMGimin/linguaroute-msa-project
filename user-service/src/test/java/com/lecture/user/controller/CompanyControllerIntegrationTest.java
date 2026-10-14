package com.lecture.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.user.entity.Term;
import com.lecture.user.entity.User;
import com.lecture.user.entity.EmailVerification;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.EmailVerificationRepository;
import com.lecture.user.repository.TermRepository;
import com.lecture.user.repository.UserAgreementRepository;
import com.lecture.user.repository.UserRepository;
import com.lecture.user.security.TokenHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private UserAgreementRepository userAgreementRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Term serviceTerm;
    private Term privacyTerm;
    private static final String VERIFICATION_TOKEN = "verified-signup-token";

    @BeforeEach
    void setUp() {
        serviceTerm = termRepository.save(Term.builder()
                .type("SERVICE_TERMS")
                .version("1.0")
                .content("서비스 이용약관")
                .required(true)
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .build());
        privacyTerm = termRepository.save(Term.builder()
                .type("PRIVACY_COLLECTION")
                .version("1.0")
                .content("개인정보 수집 동의")
                .required(true)
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .build());
        emailVerificationRepository.save(EmailVerification.builder()
                .email("admin@company.com")
                .purpose(EmailVerification.Purpose.SIGNUP)
                .codeHash(TokenHash.sha256("123456"))
                .tokenHash(TokenHash.sha256(VERIFICATION_TOKEN))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .verifiedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void 활성_약관을_공개_조회한다() throws Exception {
        mockMvc.perform(get("/api/terms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void 기업과_관리자_프로필과_약관동의를_함께_생성한다() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(List.of(serviceTerm.getId(), privacyTerm.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyId").isNumber())
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.role").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(companyRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userAgreementRepository.count()).isEqualTo(2);

        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();
        assertThat(admin.getCompany().getBusinessNumber()).isEqualTo("1234567890");
        assertThat(admin.getRole()).isEqualTo(User.AuthRole.INSTRUCTOR);
        assertThat(admin.getBusinessRole()).isEqualTo(User.BusinessRole.COMPANY_ADMIN);
        assertThat(admin.getPassword()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", admin.getPassword())).isTrue();
        assertThat(emailVerificationRepository.findAll().getFirst().getUsedAt()).isNotNull();
    }

    @Test
    void 기업_대표계정_가입은_공개_API로_제공한다() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(List.of(serviceTerm.getId(), privacyTerm.getId()))))
                .andExpect(status().isCreated());
    }

    @Test
    void 내부_API_키로_최신_사용자_권한을_조회한다() throws Exception {
        registerCompany();
        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();

        mockMvc.perform(get("/internal/users/{id}/authorization-context", admin.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/internal/users/{id}/authorization-context", admin.getId())
                        .header("X-Internal-Api-Key", "local-internal-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(admin.getId()))
                .andExpect(jsonPath("$.data.companyId").value(admin.getCompany().getId()))
                .andExpect(jsonPath("$.data.businessRole").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void 중복_사업자번호는_409를_반환한다() throws Exception {
        String request = registerRequest(List.of(serviceTerm.getId(), privacyTerm.getId()));
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BUSINESS_NUMBER"));
    }

    @Test
    void 중복_이메일은_409를_반환한다() throws Exception {
        String firstRequest = registerRequest(List.of(serviceTerm.getId(), privacyTerm.getId()));
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        Map<String, Object> secondBody = Map.of(
                "company", Map.of(
                        "name", "다른기업",
                        "businessNumber", "987-65-43210"
                ),
                "admin", Map.of(
                        "email", "admin@company.com",
                        "password", "Password123!",
                        "name", "다른관리자"
                ),
                "emailVerificationToken", VERIFICATION_TOKEN,
                "agreementIds", List.of(serviceTerm.getId(), privacyTerm.getId())
        );

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void 필수_약관이_누락되면_422를_반환하고_저장하지_않는다() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(List.of(serviceTerm.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REQUIRED_AGREEMENT_MISSING"));

        assertThat(companyRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
        assertThat(userAgreementRepository.count()).isZero();
    }

    @Test
    void 이메일_인증_토큰이_유효하지_않으면_가입을_롤백한다() throws Exception {
        Map<String, Object> body = Map.of(
                "company", Map.of("name", "스칼라테크", "businessNumber", "123-45-67890"),
                "admin", Map.of(
                        "email", "admin@company.com",
                        "password", "Password123!",
                        "name", "김관리"
                ),
                "emailVerificationToken", "wrong-token",
                "agreementIds", List.of(serviceTerm.getId(), privacyTerm.getId())
        );

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION"));

        assertThat(companyRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
        assertThat(userAgreementRepository.count()).isZero();
    }

    @Test
    void 기업_관리자가_자신의_기업을_조회하고_수정한다() throws Exception {
        registerCompany();
        Long userId = userRepository.findByEmail("admin@company.com").orElseThrow().getId();

        mockMvc.perform(get("/api/companies/me")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("스칼라테크"))
                .andExpect(jsonPath("$.data.businessNumber").value("1234567890"));

        mockMvc.perform(patch("/api/companies/me")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"스칼라글로벌\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("스칼라글로벌"));

        mockMvc.perform(post("/api/companies/me")
                        .param("action", "update-company")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"스칼라브라우저\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("스칼라브라우저"));
    }

    @Test
    void 기업_관리자가_아니면_기업_조회가_거부된다() throws Exception {
        User employee = userRepository.save(User.builder()
                .email("employee@company.com")
                .password(passwordEncoder.encode("Password123!"))
                .name("이직원")
                .role(User.AuthRole.STUDENT)
                .businessRole(User.BusinessRole.EMPLOYEE)
                .status(User.Status.ACTIVE)
                .build());

        mockMvc.perform(get("/api/companies/me")
                        .with(jwt().jwt(token -> token.subject(employee.getId().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_ADMIN_REQUIRED"));
    }

    @Test
    void 인증되지_않은_사용자는_보호_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/companies/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 사용자가_JWT_사용자ID로_내_정보를_변경한다() throws Exception {
        registerCompany();
        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();

        mockMvc.perform(patch("/api/users/me")
                        .with(jwt().jwt(token -> token.claim("userId", admin.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김수정"));

        mockMvc.perform(post("/api/users/me")
                        .param("action", "update-profile")
                        .with(jwt().jwt(token -> token.claim("userId", admin.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김브라우저\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김브라우저"));
    }

    @Test
    void 사용자가_활성_약관에_추가로_동의한다() throws Exception {
        registerCompany();
        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();
        Term marketingTerm = termRepository.save(Term.builder()
                .type("MARKETING")
                .version("1.0")
                .content("마케팅 정보 수신 동의")
                .required(false)
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .build());

        String request = objectMapper.writeValueAsString(Map.of("agreementIds", List.of(marketingTerm.getId())));
        mockMvc.perform(post("/api/users/me/agreements")
                        .with(jwt().jwt(token -> token.subject(admin.getId().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreedTermIds[0]").value(marketingTerm.getId()));

        mockMvc.perform(post("/api/users/me/agreements")
                        .with(jwt().jwt(token -> token.subject(admin.getId().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        assertThat(userAgreementRepository.count()).isEqualTo(3);
    }

    @Test
    void 비활성_사용자는_약관_동의_API도_사용할_수_없다() throws Exception {
        registerCompany();
        User admin = userRepository.findByEmail("admin@company.com").orElseThrow();
        admin.changeStatus(User.Status.INACTIVE);
        userRepository.flush();

        mockMvc.perform(post("/api/users/me/agreements")
                        .with(jwt().jwt(token -> token.claim("user_id", admin.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agreementIds", List.of(serviceTerm.getId())
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_INACTIVE"));
    }

    private void registerCompany() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(List.of(serviceTerm.getId(), privacyTerm.getId()))))
                .andExpect(status().isCreated());
    }

    private String registerRequest(List<Long> agreementIds) throws Exception {
        Map<String, Object> body = Map.of(
                "company", Map.of(
                        "name", "스칼라테크",
                        "businessNumber", "123-45-67890"
                ),
                "admin", Map.of(
                        "email", "ADMIN@company.com",
                        "password", "Password123!",
                        "name", "김관리"
                ),
                "emailVerificationToken", VERIFICATION_TOKEN,
                "agreementIds", agreementIds
        );
        return objectMapper.writeValueAsString(body);
    }

}
