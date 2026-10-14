package com.lecture.user.controller;

import com.lecture.user.dto.AdminDto;
import com.lecture.user.dto.ApiResponse;
import com.lecture.user.entity.Company;
import com.lecture.user.entity.User;
import com.lecture.user.security.AuthenticatedUser;
import com.lecture.user.service.AdminQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminDto.PageResponse<AdminDto.UserStatusResponse>>> getUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.BusinessRole businessRole,
            @RequestParam(required = false) User.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminQueryService.getUsers(
                authenticatedUser.userId(jwt), keyword, businessRole, status, page, size
        )));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<AdminDto.PageResponse<AdminDto.CompanyStatusResponse>>> getCompanies(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Company.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminQueryService.getCompanies(
                authenticatedUser.userId(jwt), keyword, status, page, size
        )));
    }
}
