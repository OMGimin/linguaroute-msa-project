package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.CompanyDto;
import com.lecture.user.dto.InvitationDto;
import com.lecture.user.service.CompanyService;
import com.lecture.user.service.InvitationService;
import com.lecture.user.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final InvitationService invitationService;
    private final com.lecture.user.service.EmployeeManagementService employeeManagementService;
    private final AuthenticatedUser authenticatedUser;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyDto.RegisterResponse>> register(
            @Valid @RequestBody CompanyDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(companyService.register(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CompanyDto.Response>> getMyCompany(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                companyService.getMyCompany(authenticatedUser.userId(jwt))
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CompanyDto.Response>> updateMyCompany(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompanyDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                companyService.updateMyCompany(authenticatedUser.userId(jwt), request)
        ));
    }

    @PostMapping(value = "/me", params = "action=update-company")
    public ResponseEntity<ApiResponse<CompanyDto.Response>> updateMyCompanyFromBrowser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompanyDto.UpdateRequest request) {
        return updateMyCompany(jwt, request);
    }

    @PostMapping("/me/invitations")
    public ResponseEntity<ApiResponse<InvitationDto.Response>> createInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InvitationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                invitationService.create(authenticatedUser.userId(jwt), request)));
    }

    @GetMapping("/me/invitations")
    public ResponseEntity<ApiResponse<java.util.List<InvitationDto.Response>>> getInvitations(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.getAll(authenticatedUser.userId(jwt))));
    }

    @GetMapping("/me/employees")
    public ResponseEntity<ApiResponse<java.util.List<CompanyDto.EmployeeResponse>>> getEmployees(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeManagementService.getEmployees(authenticatedUser.userId(jwt))));
    }

    @PatchMapping("/me/employees/{employeeId}/status")
    public ResponseEntity<ApiResponse<CompanyDto.EmployeeResponse>> updateEmployeeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long employeeId,
            @Valid @RequestBody CompanyDto.UpdateEmployeeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(employeeManagementService.updateEmployeeStatus(
                authenticatedUser.userId(jwt), employeeId, request)));
    }

    @PostMapping(value = "/me/employees/{employeeId}/status", params = "action=update-status")
    public ResponseEntity<ApiResponse<CompanyDto.EmployeeResponse>> updateEmployeeStatusFromBrowser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long employeeId,
            @Valid @RequestBody CompanyDto.UpdateEmployeeStatusRequest request) {
        return updateEmployeeStatus(jwt, employeeId, request);
    }

    @GetMapping("/me/seats")
    public ResponseEntity<ApiResponse<CompanyDto.SeatResponse>> getSeats(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeManagementService.getSeats(authenticatedUser.userId(jwt))));
    }

    @DeleteMapping("/me/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long invitationId) {
        invitationService.revoke(authenticatedUser.userId(jwt), invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/invitations/{invitationId}/reissue")
    public ResponseEntity<ApiResponse<InvitationDto.Response>> reissueInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long invitationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                invitationService.reissue(authenticatedUser.userId(jwt), invitationId)));
    }
}
