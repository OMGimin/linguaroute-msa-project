package com.lecture.user.service;

import com.lecture.user.dto.CompanyDto;
import com.lecture.user.entity.CompanyEntitlement;
import com.lecture.user.entity.User;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.CompanyEntitlementRepository;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeManagementService {

    private final CompanyService companyService;
    private final CompanyEntitlementRepository companyEntitlementRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public List<CompanyDto.EmployeeResponse> getEmployees(Long adminId) {
        User admin = companyService.requireCompanyAdmin(adminId);
        return userRepository.findAllByCompany_IdAndBusinessRoleOrderByCreatedAtDesc(
                        admin.getCompany().getId(), User.BusinessRole.EMPLOYEE)
                .stream()
                .map(CompanyDto.EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public CompanyDto.EmployeeResponse updateEmployeeStatus(
            Long adminId, Long employeeId, CompanyDto.UpdateEmployeeStatusRequest request) {
        User admin = companyService.requireCompanyAdmin(adminId);
        Long companyId = admin.getCompany().getId();
        User employee = userRepository.findByIdAndCompany_IdAndBusinessRole(
                        employeeId, companyId, User.BusinessRole.EMPLOYEE)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (request.getStatus() == CompanyDto.EmployeeStatusAction.RELEASED) {
            employee.releaseFromCompany();
            return CompanyDto.EmployeeResponse.from(employee);
        }

        if (request.getStatus() == CompanyDto.EmployeeStatusAction.ACTIVE
                && employee.getStatus() != User.Status.ACTIVE) {
            validateSeatAvailable(companyId);
        }
        employee.changeStatus(request.getStatus() == CompanyDto.EmployeeStatusAction.ACTIVE
                ? User.Status.ACTIVE : User.Status.INACTIVE);
        return CompanyDto.EmployeeResponse.from(employee);
    }

    public CompanyDto.SeatResponse getSeats(Long adminId) {
        User admin = companyService.requireCompanyAdmin(adminId);
        Long companyId = admin.getCompany().getId();
        CompanyEntitlement entitlement = requireActiveEntitlement(companyId, false);
        long used = activeEmployeeCount(companyId);
        return new CompanyDto.SeatResponse(entitlement.getSeatLimit(), used, entitlement.getSeatLimit() - used);
    }

    private void validateSeatAvailable(Long companyId) {
        CompanyEntitlement entitlement = requireActiveEntitlement(companyId, true);
        if (activeEmployeeCount(companyId) >= entitlement.getSeatLimit()) {
            throw new ApiException(ErrorCode.SEAT_LIMIT_EXCEEDED);
        }
    }

    private CompanyEntitlement requireActiveEntitlement(Long companyId, boolean forUpdate) {
        CompanyEntitlement entitlement = (forUpdate
                ? companyEntitlementRepository.findByCompanyIdForUpdate(companyId)
                : companyEntitlementRepository.findByCompany_Id(companyId))
                .orElseThrow(() -> new ApiException(ErrorCode.SUBSCRIPTION_INACTIVE));
        if (entitlement.getEntitlementStatus() != CompanyEntitlement.Status.ACTIVE
                || !entitlement.getCurrentPeriodEnd().isAfter(LocalDateTime.now(clock))) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_INACTIVE);
        }
        return entitlement;
    }

    private long activeEmployeeCount(Long companyId) {
        return userRepository.countByCompany_IdAndBusinessRoleAndStatus(
                companyId, User.BusinessRole.EMPLOYEE, User.Status.ACTIVE);
    }
}
