package com.lecture.user.service;

import com.lecture.user.dto.CompanyDto;
import com.lecture.user.entity.Company;
import com.lecture.user.entity.Term;
import com.lecture.user.entity.User;
import com.lecture.user.entity.UserAgreement;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.UserAgreementRepository;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final TermService termService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public CompanyDto.RegisterResponse register(CompanyDto.RegisterRequest request) {
        String businessNumber = normalizeBusinessNumber(request.getCompany().getBusinessNumber());
        String email = request.getAdmin().getEmail().trim().toLowerCase();

        validateDuplicates(businessNumber, email);
        List<Term> agreedTerms = termService.validateAgreementIds(request.getAgreementIds(), true);
        emailVerificationService.consumeSignupToken(email, request.getEmailVerificationToken());

        try {
            Company company = companyRepository.saveAndFlush(Company.builder()
                    .name(request.getCompany().getName().trim())
                    .businessNumber(businessNumber)
                    .status(Company.Status.ACTIVE)
                    .build());

            User admin = userRepository.saveAndFlush(User.builder()
                    .company(company)
                    .email(email)
                    .password(passwordEncoder.encode(request.getAdmin().getPassword()))
                    .name(request.getAdmin().getName().trim())
                    .role(User.AuthRole.INSTRUCTOR)
                    .businessRole(User.BusinessRole.COMPANY_ADMIN)
                    .status(User.Status.ACTIVE)
                    .build());

            LocalDateTime agreedAt = LocalDateTime.now(clock);
            List<UserAgreement> agreements = agreedTerms.stream()
                    .map(term -> UserAgreement.builder()
                            .user(admin)
                            .term(term)
                            .agreed(true)
                            .agreedAt(agreedAt)
                            .build())
                    .toList();
            userAgreementRepository.saveAll(agreements);

            return new CompanyDto.RegisterResponse(
                    company.getId(),
                    admin.getId(),
                    admin.getBusinessRole(),
                    admin.getStatus()
            );
        } catch (DataIntegrityViolationException exception) {
            String causeMessage = exception.getMostSpecificCause().getMessage().toLowerCase();
            if (causeMessage.contains("uk_companies_business_number")
                    || causeMessage.contains("business_number")) {
                throw new ApiException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
            }
            if (causeMessage.contains("uk_users_email") || causeMessage.contains("email")) {
                throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
            }
            throw exception;
        }
    }

    public CompanyDto.Response getMyCompany(Long userId) {
        return CompanyDto.Response.from(requireCompanyAdmin(userId).getCompany());
    }

    @Transactional
    public CompanyDto.Response updateMyCompany(Long userId, CompanyDto.UpdateRequest request) {
        Company company = requireCompanyAdmin(userId).getCompany();
        company.updateName(request.getName().trim());
        return CompanyDto.Response.from(company);
    }

    private void validateDuplicates(String businessNumber, String email) {
        if (companyRepository.existsByBusinessNumber(businessNumber)) {
            throw new ApiException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber.replace("-", "");
    }

    public User requireCompanyAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
        if (user.getBusinessRole() != User.BusinessRole.COMPANY_ADMIN) {
            throw new ApiException(ErrorCode.COMPANY_ADMIN_REQUIRED);
        }
        if (user.getCompany() == null) {
            throw new ApiException(ErrorCode.COMPANY_MEMBERSHIP_REQUIRED);
        }
        return user;
    }
}
