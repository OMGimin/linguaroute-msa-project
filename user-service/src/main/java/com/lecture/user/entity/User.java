package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_role", nullable = false, length = 30)
    private BusinessRole businessRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void updateName(String name) {
        this.name = name;
    }

    public void changeStatus(Status status) {
        this.status = status;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void releaseFromCompany() {
        this.company = null;
        this.status = Status.INACTIVE;
    }

    public enum AuthRole {
        STUDENT,
        INSTRUCTOR
    }

    public enum BusinessRole {
        PLATFORM_ADMIN,
        COMPANY_ADMIN,
        EMPLOYEE
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        WITHDRAWN
    }
}
