package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.UserDto;
import com.lecture.user.security.InternalApiKeyValidator;
import com.lecture.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @GetMapping("/{id}/authorization-context")
    public ResponseEntity<ApiResponse<UserDto.AuthorizationContextResponse>> getAuthorizationContext(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        internalApiKeyValidator.validate(internalApiKey);
        return ResponseEntity.ok(ApiResponse.success(userService.getAuthorizationContext(id)));
    }
}
