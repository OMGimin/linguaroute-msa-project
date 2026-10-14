package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.CompanyDto;
import com.lecture.user.security.InternalApiKeyValidator;
import com.lecture.user.service.CompanyEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/companies")
@RequiredArgsConstructor
public class InternalCompanyController {

    private final CompanyEntitlementService companyEntitlementService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @GetMapping("/{companyId}/entitlement")
    public ResponseEntity<ApiResponse<CompanyDto.EntitlementResponse>> getEntitlement(
            @PathVariable Long companyId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(ApiResponse.success(companyEntitlementService.getEntitlement(companyId)));
    }
}
