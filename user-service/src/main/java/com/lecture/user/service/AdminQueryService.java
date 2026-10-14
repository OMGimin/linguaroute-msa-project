package com.lecture.user.service;

import com.lecture.user.dto.AdminDto;
import com.lecture.user.entity.Company;
import com.lecture.user.entity.User;
import com.lecture.user.error.ApiException;
import com.lecture.user.error.ErrorCode;
import com.lecture.user.repository.CompanyRepository;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public AdminDto.PageResponse<AdminDto.UserStatusResponse> getUsers(
            Long requesterId,
            String keyword,
            User.BusinessRole businessRole,
            User.Status status,
            int page,
            int size
    ) {
        requireActivePlatformAdmin(requesterId);
        Page<AdminDto.UserStatusResponse> result = userRepository.searchForPlatformAdmin(
                        likePattern(keyword), businessRole, status, pageRequest(page, size))
                .map(AdminDto.UserStatusResponse::from);
        return AdminDto.PageResponse.from(result);
    }

    public AdminDto.PageResponse<AdminDto.CompanyStatusResponse> getCompanies(
            Long requesterId,
            String keyword,
            Company.Status status,
            int page,
            int size
    ) {
        requireActivePlatformAdmin(requesterId);
        String keywordPattern = likePattern(keyword);
        String businessNumberPattern = keywordPattern == null
                ? null
                : "%" + keyword.replace("-", "").trim().toLowerCase(Locale.ROOT) + "%";
        Page<AdminDto.CompanyStatusResponse> result = companyRepository.searchForPlatformAdmin(
                        keywordPattern, businessNumberPattern, status, pageRequest(page, size))
                .map(AdminDto.CompanyStatusResponse::from);
        return AdminDto.PageResponse.from(result);
    }

    private void requireActivePlatformAdmin(Long requesterId) {
        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
        if (user.getBusinessRole() != User.BusinessRole.PLATFORM_ADMIN) {
            throw new ApiException(ErrorCode.PLATFORM_ADMIN_REQUIRED);
        }
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
    }

    private String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
