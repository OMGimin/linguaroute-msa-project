-- LinguaRoute MVP 초기 DDL
-- 공용 lecture_db 안에서 서비스별 소유 테이블만 실제 FK를 만들고,
-- 다른 서비스의 ID는 논리 참조값으로만 저장한다.

CREATE TABLE IF NOT EXISTS companies (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    business_number     VARCHAR(10)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_companies_business_number (business_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    company_id          BIGINT,
    email               VARCHAR(255) NOT NULL,
    password            VARCHAR(255) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL COMMENT 'STUDENT | INSTRUCTOR',
    business_role       VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE' COMMENT 'PLATFORM_ADMIN | COMPANY_ADMIN | EMPLOYEE',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE | WITHDRAWN',
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invitations (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    company_id          BIGINT      NOT NULL,
    created_by_user_id  BIGINT      NOT NULL,
    used_by_user_id     BIGINT,
    code_hash           CHAR(64)    NOT NULL,
    status              VARCHAR(20) NOT NULL COMMENT 'UNUSED | USED | EXPIRED | REVOKED',
    expires_at          DATETIME(6) NOT NULL,
    used_at             DATETIME(6),
    created_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitations_code_hash (code_hash),
    KEY idx_invitations_company_created (company_id, created_at),
    CONSTRAINT fk_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_invitations_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_invitations_used_by FOREIGN KEY (used_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS email_verifications (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255) NOT NULL,
    purpose             VARCHAR(30)  NOT NULL COMMENT 'SIGNUP',
    code_hash           CHAR(64)     NOT NULL,
    token_hash          CHAR(64),
    expires_at          DATETIME(6)  NOT NULL,
    verified_at         DATETIME(6),
    used_at             DATETIME(6),
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_token_hash (token_hash),
    KEY idx_email_verifications_email_purpose (email, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    token_hash          CHAR(64)    NOT NULL,
    expires_at          DATETIME(6) NOT NULL,
    used_at             DATETIME(6),
    created_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
    KEY idx_password_reset_tokens_user_created (user_id, created_at),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS terms (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    type                VARCHAR(50)  NOT NULL,
    version             VARCHAR(30)  NOT NULL,
    content             TEXT         NOT NULL,
    required            BOOLEAN      NOT NULL,
    effective_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terms_type_version (type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_agreements (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    term_id             BIGINT      NOT NULL,
    agreed              BOOLEAN     NOT NULL,
    agreed_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_agreements_user_term (user_id, term_id),
    CONSTRAINT fk_user_agreements_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_agreements_term FOREIGN KEY (term_id) REFERENCES terms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS company_entitlements (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    company_id          BIGINT      NOT NULL,
    subscription_id     BIGINT      NOT NULL COMMENT 'payment-service subscriptions.id 논리 참조',
    entitlement_status  VARCHAR(20) NOT NULL COMMENT 'ACTIVE | EXPIRED',
    seat_limit          INT         NOT NULL,
    current_period_end  DATETIME(6) NOT NULL,
    auto_renew          BOOLEAN     NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_entitlements_company (company_id),
    CONSTRAINT fk_company_entitlements_company FOREIGN KEY (company_id) REFERENCES companies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS processed_events (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    event_id            CHAR(36)    NOT NULL,
    event_type          VARCHAR(50) NOT NULL,
    occurred_at         DATETIME(6),
    processed_at        DATETIME(6) NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_processed_events_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO terms (id, type, version, content, required, effective_at)
VALUES
    (1, 'SERVICE_TERMS', '1.0', 'LinguaRoute 서비스 이용약관', TRUE, '2026-08-10 00:00:00'),
    (2, 'PRIVACY_COLLECTION', '1.0', 'LinguaRoute 개인정보 수집 및 이용 동의', TRUE, '2026-08-10 00:00:00')
ON DUPLICATE KEY UPDATE id = id;

CREATE TABLE IF NOT EXISTS courses (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    language            VARCHAR(50)  NOT NULL COMMENT 'ENGLISH | JAPANESE | CHINESE',
    situation           VARCHAR(50)  NOT NULL COMMENT 'CUSTOMER_MEETING | PRESENTATION | EMAIL | BUSINESS_TRIP | DAILY_CONVERSATION',
    level               VARCHAR(50)  NOT NULL COMMENT 'BEGINNER | ELEMENTARY | INTERMEDIATE | ADVANCED',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [설계 이유 - 추천 데이터 소유권]
-- 초기 구현은 SQLAlchemy가 실행 시 테이블을 자동 생성했기 때문에 초기 DDL만 보면
-- 추천 테이블이 누락된 것처럼 보였다. 새 환경에서도 동일한 스키마를 재현하고
-- 개인과제에서 데이터 흐름을 설명할 수 있도록 명시적인 DDL을 함께 관리한다.
-- user_id/company_id는 user-service, course_id는 course-service 소유 데이터이므로
-- 서비스 간 외래키를 만들지 않는다. 반면 recommendation_id는 같은 추천 서비스
-- 내부 관계이므로 FK와 cascade를 사용해 추천 요청 삭제 시 항목도 함께 정리한다.
CREATE TABLE IF NOT EXISTS recommendations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    company_id  BIGINT       NOT NULL,
    language    VARCHAR(30)  NOT NULL,
    level       VARCHAR(30)  NOT NULL,
    job         VARCHAR(50)  NOT NULL,
    situation   VARCHAR(50)  NOT NULL,
    goal        TEXT         NOT NULL,
    source      VARCHAR(30)  NOT NULL COMMENT 'AI | RULE_BASED_FALLBACK',
    status      VARCHAR(20)  NOT NULL COMMENT 'SUCCESS | FALLBACK | FAILED',
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_recommendations_user_id (user_id),
    KEY idx_recommendations_company_id (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recommendation_items (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    recommendation_id   BIGINT      NOT NULL,
    course_id           BIGINT      NOT NULL,
    rank                INT         NOT NULL,
    reason              TEXT        NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_recommendation_course (recommendation_id, course_id),
    FOREIGN KEY (recommendation_id) REFERENCES recommendations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lessons (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    course_id           BIGINT       NOT NULL,
    title               VARCHAR(255) NOT NULL,
    content_url         TEXT         NOT NULL,
    sequence_no         INT          NOT NULL,
    required            BOOLEAN      NOT NULL DEFAULT TRUE,
    duration_seconds    INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_course_sequence (course_id, sequence_no),
    CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollments (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    company_id          BIGINT        NOT NULL COMMENT 'user-service companies.id 논리 참조',
    user_id             BIGINT        NOT NULL COMMENT 'user-service users.id 논리 참조',
    course_id           BIGINT        NOT NULL COMMENT 'course-service courses.id 논리 참조',
    status              VARCHAR(20)   NOT NULL DEFAULT 'ENROLLED' COMMENT 'ENROLLED | LEARNING | COMPLETED',
    progress_rate       DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    enrolled_at         DATETIME(6)   NOT NULL,
    started_at          DATETIME(6),
    completed_at        DATETIME(6),
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollment_user_course (user_id, course_id),
    KEY idx_enrollments_company_user (company_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson_progress (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    enrollment_id       BIGINT      NOT NULL,
    lesson_id           BIGINT      NOT NULL COMMENT 'course-service lessons.id 논리 참조',
    status              VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NOT_STARTED | LEARNING | COMPLETED',
    started_at          DATETIME(6),
    completed_at        DATETIME(6),
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_enrollment_lesson (enrollment_id, lesson_id),
    CONSTRAINT fk_lesson_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plans (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plans_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plan_prices (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    plan_id             BIGINT      NOT NULL,
    billing_cycle       VARCHAR(20) NOT NULL COMMENT 'MONTHLY | YEARLY',
    price               BIGINT      NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'KRW',
    seat_limit          INT         NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE',
    created_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_price_cycle (plan_id, billing_cycle),
    CONSTRAINT fk_plan_prices_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subscriptions (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    company_id              BIGINT      NOT NULL COMMENT 'user-service companies.id 논리 참조',
    plan_price_id           BIGINT      NOT NULL,
    status                  VARCHAR(20) NOT NULL COMMENT 'PENDING | ACTIVE | CANCELED | EXPIRED',
    auto_renew              BOOLEAN     NOT NULL DEFAULT TRUE,
    current_period_start    DATETIME(6) NOT NULL,
    current_period_end      DATETIME(6) NOT NULL,
    next_billing_at         DATETIME(6),
    canceled_at             DATETIME(6),
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_subscriptions_company_status (company_id, status),
    CONSTRAINT fk_subscriptions_plan_price FOREIGN KEY (plan_price_id) REFERENCES plan_prices(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    subscription_id         BIGINT,
    company_id              BIGINT       NOT NULL COMMENT 'user-service companies.id 논리 참조',
    idempotency_key         VARCHAR(100) NOT NULL,
    amount                  BIGINT       NOT NULL,
    currency                VARCHAR(10)  NOT NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SUCCESS | FAILED',
    provider_payment_id     VARCHAR(255),
    failure_reason          TEXT,
    requested_at            DATETIME(6)  NOT NULL,
    paid_at                 DATETIME(6),
    failed_at               DATETIME(6),
    created_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_company_idempotency_key (company_id, idempotency_key),
    UNIQUE KEY uk_payments_provider_payment_id (provider_payment_id),
    CONSTRAINT fk_payments_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS outbox_events (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    event_id            CHAR(36)    NOT NULL,
    aggregate_type      VARCHAR(50) NOT NULL,
    aggregate_id        BIGINT      NOT NULL,
    event_type          VARCHAR(50) NOT NULL,
    payload             TEXT        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PUBLISHED',
    created_at          DATETIME(6) NOT NULL,
    published_at        DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_events_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- Demo seed data
-- ---------------------------------------------------------------------------
-- 발표와 로컬 검증에서 바로 사용할 수 있는 최소 데이터셋입니다.
-- 공통 비밀번호는 Password123! 이며 BCrypt 해시만 저장합니다.

INSERT INTO companies (id, name, business_number, status, created_at, updated_at)
VALUES
    (9101, '스칼라테크', '9910000001', 'ACTIVE', '2026-08-10 09:00:00', '2026-08-10 09:00:00'),
    (9102, '글로벌링크', '9910000002', 'ACTIVE', '2026-08-10 09:05:00', '2026-08-10 09:05:00')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO users (id, company_id, email, password, name, role, business_role, status, created_at, updated_at)
VALUES
    (9100, NULL, 'platform-admin@linguaroute.local', '$2a$10$oUjs811YM5.JK7cliWxctOY32QifujCNDB1QYe2Pr74Lag6inAu4S', '플랫폼 관리자', 'INSTRUCTOR', 'PLATFORM_ADMIN', 'ACTIVE', '2026-08-10 09:10:00', '2026-08-10 09:10:00'),
    (9101, 9101, 'admin@skala-tech.local', '$2a$10$oUjs811YM5.JK7cliWxctOY32QifujCNDB1QYe2Pr74Lag6inAu4S', '박관리', 'INSTRUCTOR', 'COMPANY_ADMIN', 'ACTIVE', '2026-08-10 09:11:00', '2026-08-10 09:11:00'),
    (9102, 9101, 'employee.lee@skala-tech.local', '$2a$10$oUjs811YM5.JK7cliWxctOY32QifujCNDB1QYe2Pr74Lag6inAu4S', '이수강', 'STUDENT', 'EMPLOYEE', 'ACTIVE', '2026-08-10 09:12:00', '2026-08-10 09:12:00'),
    (9103, 9101, 'employee.kim@skala-tech.local', '$2a$10$oUjs811YM5.JK7cliWxctOY32QifujCNDB1QYe2Pr74Lag6inAu4S', '김학습', 'STUDENT', 'EMPLOYEE', 'ACTIVE', '2026-08-10 09:13:00', '2026-08-10 09:13:00'),
    (9104, 9102, 'admin@global-link.local', '$2a$10$oUjs811YM5.JK7cliWxctOY32QifujCNDB1QYe2Pr74Lag6inAu4S', '정관리', 'INSTRUCTOR', 'COMPANY_ADMIN', 'ACTIVE', '2026-08-10 09:14:00', '2026-08-10 09:14:00')
ON DUPLICATE KEY UPDATE
    company_id = VALUES(company_id),
    name = VALUES(name),
    role = VALUES(role),
    business_role = VALUES(business_role),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO user_agreements (user_id, term_id, agreed, agreed_at)
VALUES
    (9101, 1, TRUE, '2026-08-10 09:20:00'),
    (9101, 2, TRUE, '2026-08-10 09:20:00'),
    (9102, 1, TRUE, '2026-08-10 09:21:00'),
    (9102, 2, TRUE, '2026-08-10 09:21:00'),
    (9103, 1, TRUE, '2026-08-10 09:22:00'),
    (9103, 2, TRUE, '2026-08-10 09:22:00'),
    (9104, 1, TRUE, '2026-08-10 09:23:00'),
    (9104, 2, TRUE, '2026-08-10 09:23:00')
ON DUPLICATE KEY UPDATE
    agreed = VALUES(agreed),
    agreed_at = VALUES(agreed_at);

INSERT INTO courses (id, title, description, language, situation, level, status, created_at, updated_at)
VALUES
    (9101, '해외 고객 미팅 영어', '해외 고객과 요구사항을 확인하고 제품 사양과 기술 용어를 설명하는 실전 비즈니스 영어 과정입니다.', 'ENGLISH', 'CUSTOMER_MEETING', 'INTERMEDIATE', 'ACTIVE', '2026-08-10 10:00:00', '2026-08-10 10:00:00'),
    (9102, '비즈니스 이메일 영어', '견적, 일정, 납기와 회신을 명확하게 작성하고 기술 사양을 이메일로 전달하는 실전 과정입니다.', 'ENGLISH', 'EMAIL', 'ELEMENTARY', 'ACTIVE', '2026-08-10 10:05:00', '2026-08-10 10:05:00'),
    (9103, '일본 출장 회화', '일본 파트너와 공항·호텔·회의실에서 일정과 업무 요청을 조율하는 비즈니스 출장 과정입니다.', 'JAPANESE', 'BUSINESS_TRIP', 'BEGINNER', 'ACTIVE', '2026-08-10 10:10:00', '2026-08-10 10:10:00'),
    (9104, '중국어 프레젠테이션', '중국 파트너 앞에서 서비스 가치, 성과 지표와 다음 협업 계획을 발표하는 표현을 연습합니다.', 'CHINESE', 'PRESENTATION', 'INTERMEDIATE', 'ACTIVE', '2026-08-10 10:15:00', '2026-08-10 10:15:00'),
    (9105, '임원 보고 영어 프레젠테이션', '핵심 KPI와 제품 로드맵을 설명하고 임원 질의응답에 대응하는 고급 발표 영어입니다.', 'ENGLISH', 'PRESENTATION', 'ADVANCED', 'ACTIVE', '2026-08-10 10:20:00', '2026-08-10 10:20:00'),
    (9106, '일상 스몰토크 영어', '동료와 자연스럽게 대화하는 초급 일상 영어 과정입니다.', 'ENGLISH', 'DAILY_CONVERSATION', 'BEGINNER', 'INACTIVE', '2026-08-10 10:25:00', '2026-08-10 10:25:00')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    language = VALUES(language),
    situation = VALUES(situation),
    level = VALUES(level),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO lessons (id, course_id, title, content_url, sequence_no, required, duration_seconds)
VALUES
    (910101, 9101, '고객 미팅 시작하기', 'https://example.com/lessons/910101', 1, TRUE, 900),
    (910102, 9101, '제품 핵심 가치 설명하기', 'https://example.com/lessons/910102', 2, TRUE, 1200),
    (910103, 9101, '질문과 이견에 대응하기', 'https://example.com/lessons/910103', 3, TRUE, 1100)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), content_url = VALUES(content_url), required = VALUES(required), duration_seconds = VALUES(duration_seconds);

INSERT INTO plans (id, name, description, status, created_at)
VALUES
    (1, 'BUSINESS_50', '직원 50명까지 이용 가능한 기업 구독 플랜', 'ACTIVE', '2026-08-10 10:30:00'),
    (2, 'STARTUP_20', '직원 20명까지 이용 가능한 스타트업 구독 플랜', 'ACTIVE', '2026-08-10 10:31:00')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO plan_prices (id, plan_id, billing_cycle, price, currency, seat_limit, status, created_at)
VALUES
    (1, 1, 'MONTHLY', 299000, 'KRW', 50, 'ACTIVE', '2026-08-10 10:35:00'),
    (2, 1, 'YEARLY', 2990000, 'KRW', 50, 'ACTIVE', '2026-08-10 10:36:00'),
    (3, 2, 'MONTHLY', 129000, 'KRW', 20, 'ACTIVE', '2026-08-10 10:37:00'),
    (4, 2, 'YEARLY', 1290000, 'KRW', 20, 'ACTIVE', '2026-08-10 10:38:00')
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    currency = VALUES(currency),
    seat_limit = VALUES(seat_limit),
    status = VALUES(status);

INSERT INTO subscriptions (id, company_id, plan_price_id, status, auto_renew, current_period_start, current_period_end, next_billing_at, canceled_at, created_at, updated_at)
VALUES
    (9101, 9101, 1, 'ACTIVE', TRUE, '2026-08-10 11:00:00', '2026-09-10 11:00:00', '2026-09-10 11:00:00', NULL, '2026-08-10 11:00:00', '2026-08-10 11:00:00')
ON DUPLICATE KEY UPDATE
    plan_price_id = VALUES(plan_price_id),
    status = VALUES(status),
    auto_renew = VALUES(auto_renew),
    current_period_start = VALUES(current_period_start),
    current_period_end = VALUES(current_period_end),
    next_billing_at = VALUES(next_billing_at),
    updated_at = VALUES(updated_at);

INSERT INTO payments (id, subscription_id, company_id, idempotency_key, amount, currency, status, provider_payment_id, failure_reason, requested_at, paid_at, failed_at, created_at)
VALUES
    (9101, 9101, 9101, 'demo-subscription-20260810', 299000, 'KRW', 'SUCCESS', 'mock-demo-20260810-1001', NULL, '2026-08-10 11:00:00', '2026-08-10 11:00:02', NULL, '2026-08-10 11:00:00')
ON DUPLICATE KEY UPDATE
    subscription_id = VALUES(subscription_id),
    amount = VALUES(amount),
    currency = VALUES(currency),
    status = VALUES(status),
    provider_payment_id = VALUES(provider_payment_id),
    failure_reason = VALUES(failure_reason),
    paid_at = VALUES(paid_at);

INSERT INTO company_entitlements (company_id, subscription_id, entitlement_status, seat_limit, current_period_end, auto_renew, created_at, updated_at)
VALUES
    (9101, 9101, 'ACTIVE', 50, '2026-09-10 11:00:00', TRUE, '2026-08-10 11:00:03', '2026-08-10 11:00:03')
ON DUPLICATE KEY UPDATE
    subscription_id = VALUES(subscription_id),
    entitlement_status = VALUES(entitlement_status),
    seat_limit = VALUES(seat_limit),
    current_period_end = VALUES(current_period_end),
    auto_renew = VALUES(auto_renew),
    updated_at = VALUES(updated_at);

INSERT INTO enrollments (id, company_id, user_id, course_id, status, progress_rate, enrolled_at, started_at, completed_at, created_at, updated_at)
VALUES
    (9101, 9101, 9102, 9101, 'LEARNING', 50.00, '2026-08-10 12:00:00', '2026-08-10 12:05:00', NULL, '2026-08-10 12:00:00', '2026-08-10 12:30:00'),
    (9102, 9101, 9102, 9102, 'COMPLETED', 100.00, '2026-08-10 13:00:00', '2026-08-10 13:05:00', '2026-08-10 14:00:00', '2026-08-10 13:00:00', '2026-08-10 14:00:00'),
    (9103, 9101, 9103, 9103, 'ENROLLED', 0.00, '2026-08-10 15:00:00', NULL, NULL, '2026-08-10 15:00:00', '2026-08-10 15:00:00')
ON DUPLICATE KEY UPDATE
    company_id = VALUES(company_id),
    user_id = VALUES(user_id),
    course_id = VALUES(course_id),
    status = VALUES(status),
    progress_rate = VALUES(progress_rate),
    started_at = VALUES(started_at),
    completed_at = VALUES(completed_at),
    updated_at = VALUES(updated_at);

INSERT INTO lesson_progress (enrollment_id, lesson_id, status, started_at, completed_at, updated_at)
VALUES
    (9101, 1, 'COMPLETED', '2026-08-10 12:05:00', '2026-08-10 12:20:00', '2026-08-10 12:20:00'),
    (9101, 2, 'LEARNING', '2026-08-10 12:21:00', NULL, '2026-08-10 12:30:00'),
    (9102, 3, 'COMPLETED', '2026-08-10 13:05:00', '2026-08-10 13:30:00', '2026-08-10 13:30:00'),
    (9102, 4, 'COMPLETED', '2026-08-10 13:35:00', '2026-08-10 14:00:00', '2026-08-10 14:00:00')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    started_at = VALUES(started_at),
    completed_at = VALUES(completed_at),
    updated_at = VALUES(updated_at);
