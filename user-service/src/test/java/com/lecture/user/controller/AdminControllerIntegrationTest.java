package com.lecture.user.controller;

import com.lecture.user.entity.Company;
import com.lecture.user.entity.User;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 플랫폼_관리자가_사용자를_검색하고_역할과_상태로_필터링한다() throws Exception {
        User platformAdmin = saveUser(null, "platform@linguaroute.local", "플랫폼 관리자",
                User.BusinessRole.PLATFORM_ADMIN, User.Status.ACTIVE);
        Company company = saveCompany("스칼라테크", "1234567890", Company.Status.ACTIVE);
        saveUser(company, "employee@scala.local", "이직원",
                User.BusinessRole.EMPLOYEE, User.Status.ACTIVE);
        saveUser(company, "inactive@scala.local", "휴직원",
                User.BusinessRole.EMPLOYEE, User.Status.INACTIVE);
        saveUser(company, "admin@scala.local", "김관리",
                User.BusinessRole.COMPANY_ADMIN, User.Status.ACTIVE);

        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "스칼라")
                        .param("businessRole", "EMPLOYEE")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "20")
                        .with(jwt().jwt(token -> token.subject(platformAdmin.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("employee@scala.local"))
                .andExpect(jsonPath("$.data.content[0].businessRole").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].companyId").value(company.getId()))
                .andExpect(jsonPath("$.data.content[0].companyName").value("스칼라테크"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void 플랫폼_관리자가_기업을_사업자번호와_상태로_검색한다() throws Exception {
        User platformAdmin = saveUser(null, "platform@linguaroute.local", "플랫폼 관리자",
                User.BusinessRole.PLATFORM_ADMIN, User.Status.ACTIVE);
        Company company = saveCompany("스칼라테크", "1234567890", Company.Status.ACTIVE);
        saveCompany("비활성기업", "9876543210", Company.Status.INACTIVE);

        mockMvc.perform(get("/api/admin/companies")
                        .param("keyword", "123-45-67890")
                        .param("status", "ACTIVE")
                        .with(jwt().jwt(token -> token.claim("userId", platformAdmin.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].companyId").value(company.getId()))
                .andExpect(jsonPath("$.data.content[0].name").value("스칼라테크"))
                .andExpect(jsonPath("$.data.content[0].businessNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void 페이지_번호와_크기를_안전한_범위로_보정한다() throws Exception {
        User platformAdmin = saveUser(null, "platform@linguaroute.local", "플랫폼 관리자",
                User.BusinessRole.PLATFORM_ADMIN, User.Status.ACTIVE);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "-1")
                        .param("size", "1000")
                        .with(jwt().jwt(token -> token.subject(platformAdmin.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void 기업_관리자는_플랫폼_운영_API를_조회할_수_없다() throws Exception {
        Company company = saveCompany("스칼라테크", "1234567890", Company.Status.ACTIVE);
        User companyAdmin = saveUser(company, "admin@scala.local", "김관리",
                User.BusinessRole.COMPANY_ADMIN, User.Status.ACTIVE);

        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().jwt(token -> token.subject(companyAdmin.getId().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLATFORM_ADMIN_REQUIRED"));
    }

    @Test
    void 비활성_플랫폼_관리자는_플랫폼_운영_API를_조회할_수_없다() throws Exception {
        User platformAdmin = saveUser(null, "platform@linguaroute.local", "플랫폼 관리자",
                User.BusinessRole.PLATFORM_ADMIN, User.Status.INACTIVE);

        mockMvc.perform(get("/api/admin/companies")
                        .with(jwt().jwt(token -> token.subject(platformAdmin.getId().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_INACTIVE"));
    }

    @Test
    void 인증되지_않은_요청은_플랫폼_운영_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 올바르지_않은_필터값은_400을_반환한다() throws Exception {
        User platformAdmin = saveUser(null, "platform@linguaroute.local", "플랫폼 관리자",
                User.BusinessRole.PLATFORM_ADMIN, User.Status.ACTIVE);

        mockMvc.perform(get("/api/admin/users")
                        .param("businessRole", "UNKNOWN")
                        .with(jwt().jwt(token -> token.subject(platformAdmin.getId().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("businessRole 값이 올바르지 않습니다"));
    }

    private Company saveCompany(String name, String businessNumber, Company.Status status) {
        return companyRepository.save(Company.builder()
                .name(name)
                .businessNumber(businessNumber)
                .status(status)
                .build());
    }

    private User saveUser(
            Company company,
            String email,
            String name,
            User.BusinessRole businessRole,
            User.Status status
    ) {
        User.AuthRole authRole = businessRole == User.BusinessRole.EMPLOYEE
                ? User.AuthRole.STUDENT
                : User.AuthRole.INSTRUCTOR;
        return userRepository.save(User.builder()
                .company(company)
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .name(name)
                .role(authRole)
                .businessRole(businessRole)
                .status(status)
                .build());
    }
}
