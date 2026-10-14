package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.AuthDto;
import com.lecture.user.dto.InvitationDto;
import com.lecture.user.dto.TermDto;
import com.lecture.user.service.EmailVerificationService;
import com.lecture.user.service.AccountRecoveryService;
import com.lecture.user.service.OAuthTokenExchangeService;
import com.lecture.user.service.InvitationService;
import com.lecture.user.service.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/register")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AccountRecoveryService accountRecoveryService;
    private final OAuthTokenExchangeService oauthTokenExchangeService;
    private final InvitationService invitationService;
    private final TermService termService;

    @GetMapping(params = "action=active-terms")
    public ResponseEntity<ApiResponse<List<TermDto.Response>>> getActiveTerms() {
        return ResponseEntity.ok(ApiResponse.success(termService.getActiveTerms()));
    }

    @PostMapping(params = "action=employee-signup")
    public ResponseEntity<ApiResponse<AuthDto.EmployeeSignupResponse>> signupEmployee(
            @Valid @RequestBody AuthDto.EmployeeSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(invitationService.signupEmployee(request)));
    }

    @PostMapping(params = "action=request-email-verification")
    public ResponseEntity<ApiResponse<AuthDto.AcceptedResponse>> requestEmailVerification(
            @Valid @RequestBody AuthDto.EmailVerificationRequest request
    ) {
        emailVerificationService.requestSignupVerification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new AuthDto.AcceptedResponse(true)));
    }

    @PostMapping(params = "action=confirm-email-verification")
    public ResponseEntity<ApiResponse<AuthDto.EmailVerificationConfirmResponse>> confirmEmailVerification(
            @Valid @RequestBody AuthDto.EmailVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                emailVerificationService.confirmSignupVerification(request)
        ));
    }

    @PostMapping(params = "action=request-password-reset")
    public ResponseEntity<ApiResponse<AuthDto.AcceptedResponse>> requestPasswordReset(
            @Valid @RequestBody AuthDto.PasswordResetRequest request
    ) {
        accountRecoveryService.requestPasswordReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new AuthDto.AcceptedResponse(true)));
    }

    @PostMapping(params = "action=confirm-password-reset")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody AuthDto.PasswordResetConfirmRequest request
    ) {
        accountRecoveryService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping(params = "action=request-id-find")
    public ResponseEntity<ApiResponse<AuthDto.AcceptedResponse>> requestIdFind(
            @Valid @RequestBody AuthDto.IdFindRequest request
    ) {
        accountRecoveryService.requestIdFind(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new AuthDto.AcceptedResponse(true)));
    }

    @PostMapping(params = "action=exchange-oauth-code")
    public ResponseEntity<ApiResponse<AuthDto.OAuthTokenResponse>> exchangeOAuthCode(
            @Valid @RequestBody AuthDto.OAuthCodeExchangeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(oauthTokenExchangeService.exchangeCode(request)));
    }

    @PostMapping(params = "action=validate-invitation")
    public ResponseEntity<ApiResponse<InvitationDto.ValidationResponse>> validateInvitation(
            @Valid @RequestBody InvitationDto.ValidateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.validateForSignup(request.getInvitationCode())));
    }
}
