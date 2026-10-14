package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.AuthDto;
import com.lecture.user.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final InvitationService invitationService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthDto.EmployeeSignupResponse>> signup(
            @Valid @RequestBody AuthDto.EmployeeSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(invitationService.signupEmployee(request)));
    }

}
