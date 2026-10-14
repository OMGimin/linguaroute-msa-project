package com.lecture.user.controller;

import com.lecture.user.dto.ApiResponse;
import com.lecture.user.dto.TermDto;
import com.lecture.user.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TermDto.Response>>> getActiveTerms() {
        return ResponseEntity.ok(ApiResponse.success(termService.getActiveTerms()));
    }
}
