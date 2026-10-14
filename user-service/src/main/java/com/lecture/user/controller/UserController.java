package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.AuthDto;
import com.lecture.user.dto.CompanyDto;
import com.lecture.user.dto.UserDto;
import com.lecture.user.service.CompanyService;
import com.lecture.user.service.UserService;
import com.lecture.user.service.UserAgreementService;
import com.lecture.user.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAgreementService userAgreementService;
    private final CompanyService companyService;
    private final AuthenticatedUser authenticatedUser;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CompanyDto.RegisterResponse>> registerCompanyAdmin(
            @Valid @RequestBody CompanyDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(companyService.register(request)));
    }

    /**
     * GET /users/me - 내 정보 조회
     * 검증된 JWT subject의 숫자 userId를 사용
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> getMe(
            @AuthenticationPrincipal Jwt jwt) {

        UserDto.UserResponse response = userService.getUserById(authenticatedUser.userId(jwt));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateMe(authenticatedUser.userId(jwt), request)
        ));
    }

    @PostMapping(value = "/me", params = "action=update-profile")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> updateMeFromBrowser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return updateMe(jwt, request);
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AuthDto.PasswordChangeRequest request) {
        userService.changePassword(authenticatedUser.userId(jwt), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt jwt) {
        userService.withdraw(authenticatedUser.userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/agreements")
    public ResponseEntity<ApiResponse<UserDto.AgreementResponse>> agreeToTerms(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserDto.AgreementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userAgreementService.agree(authenticatedUser.userId(jwt), request)
        ));
    }

}
