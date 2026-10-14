package com.lecture.user.dto;

import com.lecture.user.entity.Company;
import com.lecture.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public static <T> PageResponse<T> from(Page<T> result) {
            return PageResponse.<T>builder()
                    .content(result.getContent())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class UserStatusResponse {
        private Long userId;
        private String email;
        private String name;
        private User.BusinessRole businessRole;
        private User.Status status;
        private Long companyId;
        private String companyName;
        private LocalDateTime createdAt;

        public static UserStatusResponse from(User user) {
            Company company = user.getCompany();
            return new UserStatusResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getBusinessRole(),
                    user.getStatus(),
                    company == null ? null : company.getId(),
                    company == null ? null : company.getName(),
                    user.getCreatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class CompanyStatusResponse {
        private Long companyId;
        private String name;
        private String businessNumber;
        private Company.Status status;
        private LocalDateTime createdAt;

        public static CompanyStatusResponse from(Company company) {
            return new CompanyStatusResponse(
                    company.getId(),
                    company.getName(),
                    company.getBusinessNumber(),
                    company.getStatus(),
                    company.getCreatedAt()
            );
        }
    }
}
