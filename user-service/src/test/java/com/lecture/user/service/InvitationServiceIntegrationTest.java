package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.dto.InvitationDto;
import com.lecture.user.entity.Company;
import com.lecture.user.entity.CompanyEntitlement;
import com.lecture.user.entity.EmailVerification;
import com.lecture.user.entity.Invitation;
import com.lecture.user.entity.Term;
import com.lecture.user.entity.User;
import com.lecture.user.error.ApiException;
import com.lecture.user.repository.CompanyEntitlementRepository;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.EmailVerificationRepository;
import com.lecture.user.repository.InvitationRepository;
import com.lecture.user.repository.TermRepository;
import com.lecture.user.repository.UserAgreementRepository;
import com.lecture.user.repository.UserRepository;
import com.lecture.user.security.TokenHash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InvitationServiceIntegrationTest {

    @Autowired private InvitationService invitationService;
    @Autowired private EmployeeManagementService employeeManagementService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyEntitlementRepository companyEntitlementRepository;
    @Autowired private InvitationRepository invitationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserAgreementRepository userAgreementRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private TermRepository termRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Company company;
    private User admin;
    private Term serviceTerm;
    private Term privacyTerm;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        company = companyRepository.save(Company.builder()
                .name("초대 검증 기업")
                .businessNumber(suffix.substring(0, 10))
                .status(Company.Status.ACTIVE)
                .build());
        admin = userRepository.save(User.builder()
                .company(company)
                .email("admin-" + suffix.substring(0, 12) + "@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .name("기업 관리자")
                .role(User.AuthRole.INSTRUCTOR)
                .businessRole(User.BusinessRole.COMPANY_ADMIN)
                .status(User.Status.ACTIVE)
                .build());
        serviceTerm = termRepository.save(term("SERVICE_TERMS", suffix.substring(0, 8)));
        privacyTerm = termRepository.save(term("PRIVACY_COLLECTION", suffix.substring(8, 16)));
        companyEntitlementRepository.save(CompanyEntitlement.active(
                company, 1L, 2, LocalDateTime.now().plusDays(30), true));
    }

    @AfterEach
    void tearDown() {
        userAgreementRepository.deleteAll();
        invitationRepository.deleteAll();
        emailVerificationRepository.deleteAll();
        companyEntitlementRepository.deleteAll();
        userRepository.deleteAll();
        termRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void 초대코드는_원문을_생성응답에만_노출하고_폐기와_재발급을_지원한다() {
        InvitationDto.Response created = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));

        assertThat(created.getCode()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
        assertThat(invitationRepository.findAll().getFirst().getCodeHash())
                .isEqualTo(TokenHash.sha256(created.getCode()));
        assertThat(invitationService.getAll(admin.getId()).getFirst().getCode()).isNull();

        invitationService.revoke(admin.getId(), created.getInvitationId());
        assertThat(invitationService.getAll(admin.getId()).getFirst().getStatus())
                .isEqualTo(Invitation.Status.REVOKED);

        InvitationDto.Response reissued = invitationService.reissue(admin.getId(), created.getInvitationId());
        assertThat(reissued.getCode()).isNotEqualTo(created.getCode());
        assertThat(invitationService.getAll(admin.getId())).hasSize(2);
    }

    @Test
    void 직원가입은_초대코드를_한번만_사용하고_좌석을_배정한다() {
        InvitationDto.Response invitation = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        String token = verifiedSignupToken("employee@example.com");

        AuthDto.EmployeeSignupResponse response = invitationService.signupEmployee(employeeRequest(
                invitation.getCode(), "employee@example.com", token));

        assertThat(response.getCompanyId()).isEqualTo(company.getId());
        assertThat(response.getBusinessRole()).isEqualTo("EMPLOYEE");
        assertThat(invitationRepository.findById(invitation.getInvitationId()).orElseThrow().getStatus())
                .isEqualTo(Invitation.Status.USED);
        assertThat(userRepository.countByCompany_IdAndBusinessRoleAndStatus(
                company.getId(), User.BusinessRole.EMPLOYEE, User.Status.ACTIVE)).isEqualTo(1);

        assertThatThrownBy(() -> invitationService.signupEmployee(employeeRequest(
                invitation.getCode(), "another@example.com", verifiedSignupToken("another@example.com"))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(com.lecture.user.error.ErrorCode.INVITATION_ALREADY_USED);
    }

    @Test
    void 만료된_초대코드와_좌석초과와_비활성구독을_거부한다() {
        Invitation expired = invitationRepository.save(Invitation.unused(
                company, admin, TokenHash.sha256("EXPR-IRED"), LocalDateTime.now().minusSeconds(1)));
        assertThatThrownBy(() -> invitationService.signupEmployee(employeeRequest(
                "EXPR-IRED", "expired@example.com", verifiedSignupToken("expired@example.com"))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(com.lecture.user.error.ErrorCode.INVITATION_EXPIRED);
        invitationService.getAll(admin.getId());
        assertThat(invitationRepository.findById(expired.getId()).orElseThrow().getStatus())
                .isEqualTo(Invitation.Status.EXPIRED);

        InvitationDto.Response first = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        invitationService.signupEmployee(employeeRequest(first.getCode(), "one@example.com", verifiedSignupToken("one@example.com")));
        InvitationDto.Response second = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        invitationService.signupEmployee(employeeRequest(second.getCode(), "two@example.com", verifiedSignupToken("two@example.com")));
        InvitationDto.Response third = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        assertThatThrownBy(() -> invitationService.signupEmployee(employeeRequest(
                third.getCode(), "three@example.com", verifiedSignupToken("three@example.com"))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(com.lecture.user.error.ErrorCode.SEAT_LIMIT_EXCEEDED);
    }

    @Test
    void 활성_상태가_아닌_구독은_직원가입을_허용하지_않는다() {
        CompanyEntitlement entitlement = companyEntitlementRepository.findByCompany_Id(company.getId()).orElseThrow();
        entitlement.expire(LocalDateTime.now());
        companyEntitlementRepository.save(entitlement);
        InvitationDto.Response invitation = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));

        assertThatThrownBy(() -> invitationService.signupEmployee(employeeRequest(
                invitation.getCode(), "inactive@example.com", verifiedSignupToken("inactive@example.com"))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(com.lecture.user.error.ErrorCode.SUBSCRIPTION_INACTIVE);
    }

    @Test
    void 기업관리자는_직원상태와_좌석을_함께관리한다() {
        InvitationDto.Response first = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        invitationService.signupEmployee(employeeRequest(first.getCode(), "first@example.com", verifiedSignupToken("first@example.com")));
        InvitationDto.Response second = invitationService.create(admin.getId(), new InvitationDto.CreateRequest(7));
        invitationService.signupEmployee(employeeRequest(second.getCode(), "second@example.com", verifiedSignupToken("second@example.com")));
        User firstEmployee = userRepository.findByEmail("first@example.com").orElseThrow();

        assertThat(employeeManagementService.getEmployees(admin.getId())).hasSize(2);
        assertThat(employeeManagementService.getSeats(admin.getId()).getUsed()).isEqualTo(2);
        assertThat(employeeManagementService.getSeats(admin.getId()).getRemaining()).isZero();

        employeeManagementService.updateEmployeeStatus(admin.getId(), firstEmployee.getId(),
                new com.lecture.user.dto.CompanyDto.UpdateEmployeeStatusRequest(
                        com.lecture.user.dto.CompanyDto.EmployeeStatusAction.INACTIVE));
        assertThat(employeeManagementService.getSeats(admin.getId()).getUsed()).isEqualTo(1);

        employeeManagementService.updateEmployeeStatus(admin.getId(), firstEmployee.getId(),
                new com.lecture.user.dto.CompanyDto.UpdateEmployeeStatusRequest(
                        com.lecture.user.dto.CompanyDto.EmployeeStatusAction.RELEASED));
        assertThat(userRepository.findById(firstEmployee.getId()).orElseThrow().getCompany()).isNull();
        assertThat(employeeManagementService.getEmployees(admin.getId())).hasSize(1);
    }

    private AuthDto.EmployeeSignupRequest employeeRequest(String code, String email, String token) {
        return new AuthDto.EmployeeSignupRequest(code, email, "Password123!", "직원", token,
                List.of(serviceTerm.getId(), privacyTerm.getId()));
    }

    private String verifiedSignupToken(String email) {
        String token = UUID.randomUUID().toString();
        emailVerificationRepository.save(EmailVerification.builder()
                .email(email)
                .purpose(EmailVerification.Purpose.SIGNUP)
                .codeHash(TokenHash.sha256("123456"))
                .tokenHash(TokenHash.sha256(token))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verifiedAt(LocalDateTime.now())
                .build());
        return token;
    }

    private Term term(String type, String version) {
        return Term.builder()
                .type(type)
                .version(version)
                .content(type)
                .required(true)
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
