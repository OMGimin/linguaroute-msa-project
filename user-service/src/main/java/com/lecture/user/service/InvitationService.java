package com.lecture.user.service;

import com.lecture.user.dto.AuthDto;
import com.lecture.user.dto.InvitationDto;
import com.lecture.user.entity.CompanyEntitlement;
import com.lecture.user.entity.Invitation;
import com.lecture.user.entity.Term;
import com.lecture.user.entity.User;
import com.lecture.user.entity.UserAgreement;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.CompanyEntitlementRepository;
import com.lecture.user.repository.InvitationRepository;
import com.lecture.user.repository.UserAgreementRepository;
import com.lecture.user.repository.UserRepository;
import com.lecture.user.security.TokenHash;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int DEFAULT_EXPIRATION_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final CompanyEntitlementRepository companyEntitlementRepository;
    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final CompanyService companyService;
    private final TermService termService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public InvitationDto.Response create(Long userId, InvitationDto.CreateRequest request) {
        User admin = companyService.requireCompanyAdmin(userId);
        return issue(admin, request.getExpiresInDays());
    }

    @Transactional
    public List<InvitationDto.Response> getAll(Long userId) {
        User admin = companyService.requireCompanyAdmin(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        return invitationRepository.findByCompany_IdOrderByCreatedAtDesc(admin.getCompany().getId()).stream()
                .peek(invitation -> invitation.expireIfDue(now))
                .map(InvitationDto.Response::from)
                .toList();
    }

    @Transactional
    public void revoke(Long userId, Long invitationId) {
        User admin = companyService.requireCompanyAdmin(userId);
        Invitation invitation = findCompanyInvitation(invitationId, admin.getCompany().getId());
        invitation.expireIfDue(LocalDateTime.now(clock));
        invitation.revokeIfUnused();
    }

    @Transactional
    public InvitationDto.Response reissue(Long userId, Long invitationId) {
        User admin = companyService.requireCompanyAdmin(userId);
        Invitation invitation = findCompanyInvitation(invitationId, admin.getCompany().getId());
        invitation.expireIfDue(LocalDateTime.now(clock));
        invitation.revokeIfUnused();
        return issue(admin, DEFAULT_EXPIRATION_DAYS);
    }

    @Transactional
    public AuthDto.EmployeeSignupResponse signupEmployee(AuthDto.EmployeeSignupRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Invitation invitation = invitationRepository.findByCodeHash(TokenHash.sha256(normalizeCode(request.getInvitationCode())))
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
        validateInvitation(invitation, now);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
        List<Term> agreedTerms = termService.validateAgreementIds(request.getAgreementIds(), true);
        emailVerificationService.consumeSignupToken(email, request.getEmailVerificationToken());
        validateActiveEntitlementAndSeat(invitation.getCompany().getId(), now);

        try {
            User employee = userRepository.saveAndFlush(User.builder()
                    .company(invitation.getCompany())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName().trim())
                    .role(User.AuthRole.STUDENT)
                    .businessRole(User.BusinessRole.EMPLOYEE)
                    .status(User.Status.ACTIVE)
                    .build());
            userAgreementRepository.saveAll(agreedTerms.stream()
                    .map(term -> UserAgreement.builder()
                            .user(employee)
                            .term(term)
                            .agreed(true)
                            .agreedAt(now)
                            .build())
                    .toList());
            invitation.use(employee, now);
            return new AuthDto.EmployeeSignupResponse(
                    employee.getId(),
                    invitation.getCompany().getId(),
                    employee.getBusinessRole().name(),
                    employee.getStatus().name()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional
    public InvitationDto.ValidationResponse validateForSignup(String rawCode) {
        LocalDateTime now = LocalDateTime.now(clock);
        Invitation invitation = invitationRepository.findByCodeHash(TokenHash.sha256(normalizeCode(rawCode)))
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
        validateInvitation(invitation, now);
        validateActiveEntitlementAndSeat(invitation.getCompany().getId(), now);
        return new InvitationDto.ValidationResponse(true, invitation.getCompany().getId(), invitation.getExpiresAt());
    }

    private InvitationDto.Response issue(User admin, int expiresInDays) {
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(expiresInDays);
        for (int attempt = 0; attempt < 5; attempt++) {
            String rawCode = generateCode();
            String codeHash = TokenHash.sha256(rawCode);
            if (invitationRepository.existsByCodeHash(codeHash)) {
                continue;
            }
            Invitation invitation = invitationRepository.saveAndFlush(
                    Invitation.unused(admin.getCompany(), admin, codeHash, expiresAt));
            return InvitationDto.Response.issued(invitation, rawCode);
        }
        throw new IllegalStateException("초대코드를 생성할 수 없습니다.");
    }

    private Invitation findCompanyInvitation(Long invitationId, Long companyId) {
        return invitationRepository.findByIdAndCompany_Id(invitationId, companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private void validateInvitation(Invitation invitation, LocalDateTime now) {
        invitation.expireIfDue(now);
        switch (invitation.getStatus()) {
            case UNUSED -> {
            }
            case USED -> throw new ApiException(ErrorCode.INVITATION_ALREADY_USED);
            case EXPIRED -> throw new ApiException(ErrorCode.INVITATION_EXPIRED);
            case REVOKED -> throw new ApiException(ErrorCode.INVITATION_REVOKED);
        }
    }

    private void validateActiveEntitlementAndSeat(Long companyId, LocalDateTime now) {
        CompanyEntitlement entitlement = companyEntitlementRepository.findByCompanyIdForUpdate(companyId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUBSCRIPTION_INACTIVE));
        if (entitlement.getEntitlementStatus() != CompanyEntitlement.Status.ACTIVE
                || !entitlement.getCurrentPeriodEnd().isAfter(now)) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_INACTIVE);
        }
        long activeEmployeeCount = userRepository.countByCompany_IdAndBusinessRoleAndStatus(
                companyId, User.BusinessRole.EMPLOYEE, User.Status.ACTIVE);
        if (activeEmployeeCount >= entitlement.getSeatLimit()) {
            throw new ApiException(ErrorCode.SEAT_LIMIT_EXCEEDED);
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(9);
        for (int index = 0; index < 8; index++) {
            if (index == 4) {
                code.append('-');
            }
            code.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
