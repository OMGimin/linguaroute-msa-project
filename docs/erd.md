# LinguaRoute MSA ERD

> 문서 상태: 2026-08-11 현재 엔티티·초기 스키마·이벤트 구현 동기화

## 1. 설계 원칙

- MVP에서는 현재 Docker Compose 구성과 동일하게 MariaDB 한 개와 `lecture_db` 한 개를 사용합니다.
- 각 마이크로서비스는 공용 `lecture_db` 안에서 자신의 테이블만 소유하고 접근합니다.
- 다른 서비스 소유 테이블을 직접 조회·조인하거나 서비스 사이 외래키를 만들지 않습니다.
- 다른 서비스에서 받은 `company_id`, `user_id`, `course_id` 등은 논리 참조 ID로만 저장합니다.
- 서비스 간 데이터 확인은 REST API 또는 Kafka 이벤트로 처리합니다.
- 기업별 데이터는 `company_id`를 기준으로 분리합니다.
- 강사 제공 Auth Server가 공용 `users` 테이블을 직접 읽는 현재 구조만 호환 예외로 유지하며, 새 서비스 간 직접 조인은 추가하지 않습니다.

---

## 2. 전체 논리 관계

```mermaid
flowchart LR
    AUTH_SERVER["Auth Server<br/>Email Login/JWT"] -. "users 로그인 필드 읽기" .-> USER
    COMPANY["Company<br/>user-service"] --> USER["User<br/>user-service"]
    COMPANY --> INVITATION["Invitation<br/>user-service"]
    COMPANY -. "논리 참조" .-> SUBSCRIPTION["Subscription<br/>payment-service"]
    PLAN["Plan<br/>payment-service"] --> SUBSCRIPTION
    SUBSCRIPTION --> PAYMENT["Payment<br/>payment-service"]
    USER -. "논리 참조" .-> ENROLLMENT["Enrollment<br/>enrollment-service"]
    COURSE["Course<br/>course-service"] --> LESSON["Lesson<br/>course-service"]
    COURSE -. "논리 참조" .-> ENROLLMENT
    ENROLLMENT --> PROGRESS["Lesson Progress<br/>enrollment-service"]
    USER -. "논리 참조·권한 조회" .-> RECOMMENDATION["Recommendation<br/>recommend-service"]
    COURSE -. "논리 참조·ACTIVE 후보 조회" .-> RECOMMENDATION_ITEM["Recommendation Item<br/>recommend-service"]
    RECOMMENDATION --> RECOMMENDATION_ITEM
```

점선은 서로 다른 서비스 소유 영역 사이의 논리 참조이며 실제 데이터베이스 외래키를 생성하지 않습니다.

---

## 3. 기존 Auth Server 호환

Auth Server는 별도의 신규 인증 테이블을 소유하지 않고 공용 `users` 테이블의 `id`, `email`, `password`, `name`, `role`을 읽어 자체 이메일·비밀번호 로그인과 OAuth2 Authorization Code 기반 JWT Access Token 발급을 수행합니다. 소셜 로그인은 사용하지 않습니다. Auth Server가 Refresh Token을 반환해도 MVP 프론트엔드는 저장·갱신에 사용하지 않습니다.

제공 JAR의 역할 enum이 `STUDENT`, `INSTRUCTOR`로 고정되어 있으므로 `users.role`은 로그인 호환 필드로 유지합니다. `EMPLOYEE`는 `STUDENT`, `COMPANY_ADMIN`과 `PLATFORM_ADMIN`은 `INSTRUCTOR`로 매핑합니다. LinguaRoute의 실제 권한은 `users.business_role`에 저장하고 각 보호 API가 `user-service`의 사용자 상태·기업 소속과 함께 확인합니다.

---

## 4. user-service ERD

```mermaid
erDiagram
    COMPANY ||--o{ USER : contains
    COMPANY ||--o{ INVITATION : issues
    COMPANY ||--o| COMPANY_ENTITLEMENT : owns
    USER ||--o{ INVITATION : creates
    USER ||--o{ USER_AGREEMENT : agrees
    USER ||--o{ PASSWORD_RESET_TOKEN : resets
    TERM ||--o{ USER_AGREEMENT : accepted_as

    COMPANY {
        bigint id PK
        varchar name
        varchar business_number UK
        varchar status
        datetime created_at
        datetime updated_at
    }

    USER {
        bigint id PK
        bigint company_id FK
        varchar email UK
        varchar password
        varchar name
        varchar role
        varchar business_role
        varchar status
        datetime created_at
        datetime updated_at
    }

    INVITATION {
        bigint id PK
        bigint company_id FK
        bigint created_by_user_id FK
        bigint used_by_user_id
        varchar code_hash UK
        varchar status
        datetime expires_at
        datetime used_at
        datetime created_at
    }

    COMPANY_ENTITLEMENT {
        bigint id PK
        bigint company_id FK
        bigint subscription_id
        varchar entitlement_status
        int seat_limit
        boolean auto_renew
        datetime current_period_end
        datetime created_at
        datetime updated_at
    }

    PROCESSED_EVENT {
        bigint id PK
        varchar event_id UK
        varchar event_type
        datetime occurred_at
        datetime processed_at
    }

    TERM {
        bigint id PK
        varchar type
        varchar version
        text content
        boolean required
        datetime effective_at
    }

    USER_AGREEMENT {
        bigint id PK
        bigint user_id FK
        bigint term_id FK
        boolean agreed
        datetime agreed_at
    }

    EMAIL_VERIFICATION {
        bigint id PK
        varchar email
        varchar purpose
        varchar code_hash
        varchar token_hash UK
        datetime expires_at
        datetime verified_at
        datetime used_at
        datetime created_at
    }

    PASSWORD_RESET_TOKEN {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        datetime expires_at
        datetime used_at
        datetime created_at
    }

```

### user-service 주요 제약조건

| 테이블 | 제약조건 |
| --- | --- |
| `companies` | `business_number` 유일 |
| `users` | `email` 유일, `password`에는 BCrypt 해시만 저장 |
| `users.role` | 기존 Auth Server 호환용 `STUDENT`, `INSTRUCTOR`만 저장 |
| `users.business_role` | 실제 권한 `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `EMPLOYEE` 저장 |
| `users.company_id` | `PLATFORM_ADMIN`은 `NULL`, 기업 관리자와 활성·비활성 직원은 필수, `RELEASED` 처리된 직원은 `NULL` |
| `invitations` | 원문 코드 대신 `code_hash` 저장 및 유일 처리 |
| `invitations` | `UNUSED` 상태이고 만료 전일 때만 사용 가능 |
| `company_entitlements` | 기업별 1개, 결제 이벤트의 최신 구독 권한을 조회용으로 저장 |
| `processed_events` | `event_id` 유일로 Kafka 이벤트 중복 처리 방지 |
| `user_agreements` | `(user_id, term_id)` 유일 |
| `email_verifications` | 코드와 토큰을 해시로 저장하고 만료·일회성 사용 처리 |
| `password_reset_tokens` | `user_id`, 토큰 SHA-256 해시, 만료·사용 시각을 저장해 15분 만료·일회성 사용 처리 |

`user-service`는 사용자와 비밀번호 해시, 이메일 인증, 아이디 찾기, 비밀번호 변경·재설정을 소유합니다. Auth Server는 로그인 시 같은 `users` 테이블의 호환 필드만 읽습니다. 활성 직원 수가 사용 좌석 수입니다. 구매 좌석 수와 구독 상태의 원본은 `payment-service`가 소유하며, `user-service`는 최신 이용 권한을 `company_entitlements`에 저장하여 직원 가입과 수강신청 권한 조회에 사용합니다. `subscription_id`는 `payment-service`에 대한 논리 참조입니다. 구독 해지 시 `auto_renew`만 `false`로 바꾸고 `current_period_end`까지 `entitlement_status=ACTIVE`를 유지합니다.

현재 구현은 `invitations` 테이블의 초대코드 SHA-256 해시 저장, 초대 생성·목록·폐기·재발급, 초대 기반 직원 가입의 단회 사용·만료·구독·좌석 검증을 포함합니다. 사용 좌석은 별도 테이블이 아니라 같은 기업의 `ACTIVE` 직원 수로 계산하며, 직원 비활성화·소속 해제는 즉시 좌석을 회수합니다. 재활성화는 `company_entitlements` 잠금 조회 후 잔여 좌석을 다시 검증합니다. 또한 `company_entitlements` 테이블, `/internal/companies/{companyId}/entitlement` 조회 API, `subscription.events` Kafka 소비와 `processed_events` 기반 중복 처리를 포함합니다.

---

## 5. course-service ERD

```mermaid
erDiagram
    COURSE ||--|{ LESSON : contains

    COURSE {
        bigint id PK
        varchar title
        text description
        varchar language
        varchar situation
        varchar level
        varchar status
        datetime created_at
        datetime updated_at
    }

    LESSON {
        bigint id PK
        bigint course_id FK
        varchar title
        text content_url
        int sequence_no
        boolean required
        int duration_seconds
    }

```

### course-service 주요 제약조건

| 테이블 | 제약조건 |
| --- | --- |
| `courses` | 상태는 `ACTIVE`, `INACTIVE`만 사용 |
| `lessons` | `(course_id, sequence_no)` 유일 |

실제 물리 테이블은 `courses`, `lessons`입니다. `lessons`는 현재 생성·수정 시각 컬럼을 저장하지 않습니다. 강의 등록·수정·활성·비활성 상태 관리는 확정 MVP입니다. 별도의 게시·비게시 상태는 두지 않고 `ACTIVE`, `INACTIVE`로 통합합니다.

---

## 6. recommend-service ERD

```mermaid
erDiagram
    RECOMMENDATION ||--o{ RECOMMENDATION_ITEM : contains

    RECOMMENDATION {
        bigint id PK
        bigint user_id
        bigint company_id
        varchar language
        varchar level
        varchar job
        varchar situation
        text goal
        varchar source
        varchar status
        datetime created_at
    }

    RECOMMENDATION_ITEM {
        bigint id PK
        bigint recommendation_id FK
        bigint course_id
        int rank
        text reason
        datetime created_at
    }
```

### recommend-service 주요 제약조건

| 테이블 | 제약조건 |
| --- | --- |
| `recommendations` | 요청 사용자·기업·언어·수준·직무·상황·목표와 최종 출처·상태 저장 |
| `recommendation_items` | `(recommendation_id, course_id)` 유일, 같은 추천 안에서 강의 중복 방지 |
| `recommendation_items.recommendation_id` | 같은 서비스 소유 FK, 추천 삭제 시 항목 `ON DELETE CASCADE` |
| `user_id`, `company_id`, `course_id` | 다른 서비스 소유 ID이므로 실제 FK 없이 논리 참조로 저장 |
| 추천 결과 | 최대 3개, `ACTIVE` 상태·요청 언어 일치·기존 수강 제외 후 저장 |

`recommend-service`는 `recommendations`와 `recommendation_items`를 소유합니다. 두 테이블은 `init-db/01_init.sql`에 명시되어 있고 SQLAlchemy Entity와 동일한 컬럼·제약조건을 사용합니다. 응답의 `recommendationId`는 `recommendations.id`의 영속 기본키입니다.

```text
직원 요청
→ user-service 내부 API로 최신 역할·소속·상태 확인
→ enrollment-service 내부 API로 기존 수강 강의 ID 조회
→ course-service 내부 API로 같은 언어의 ACTIVE 후보 조회
→ OpenAI 또는 로컬 Provider가 후보 안에서 최대 3개와 이유 생성
→ recommend-service가 후보 ID·중복·언어·상태 재검증
→ 실패 시 규칙 기반 fallback으로 전환
→ recommendations와 recommendation_items 저장 후 응답
```

---

## 7. enrollment-service ERD

```mermaid
erDiagram
    ENROLLMENT ||--o{ LESSON_PROGRESS : tracks

    ENROLLMENT {
        bigint id PK
        bigint company_id
        bigint user_id
        bigint course_id
        varchar status
        decimal progress_rate
        datetime enrolled_at
        datetime started_at
        datetime completed_at
        datetime created_at
        datetime updated_at
    }

    LESSON_PROGRESS {
        bigint id PK
        bigint enrollment_id FK
        bigint lesson_id
        varchar status
        datetime started_at
        datetime completed_at
        datetime updated_at
    }

```

### enrollment-service 주요 제약조건

| 테이블 | 제약조건 |
| --- | --- |
| `enrollment` | `(user_id, course_id)` 유일하여 중복 수강신청 방지 |
| `lesson_progress` | `(enrollment_id, lesson_id)` 유일 |
| 진도율 | 클라이언트 입력 금지, 서버 계산 |
| 완료 상태 | 모든 필수 차시 완료 시 `COMPLETED` |

`company_id`와 `user_id`는 `user-service`, `course_id`와 `lesson_id`는 `course-service`에 대한 논리 참조입니다.

### 진도율 계산

```text
진도율 = 완료한 필수 차시 수 / 전체 필수 차시 수 × 100
```

---

## 8. payment-service ERD

`payment-service`의 실제 물리 테이블명은 JPA 구현 기준으로 복수형을 사용합니다. 논리 엔티티는 `Plan`, `PlanPrice`, `Subscription`, `Payment`, `OutboxEvent`입니다.

```mermaid
erDiagram
    PLANS ||--|{ PLAN_PRICES : priced_as
    PLAN_PRICES ||--o{ SUBSCRIPTIONS : selected_by
    SUBSCRIPTIONS ||--o{ PAYMENTS : paid_by
    SUBSCRIPTIONS ||--o{ OUTBOX_EVENTS : emits

    PLANS {
        bigint id PK
        varchar name
        varchar description
        varchar status
        datetime created_at
    }

    PLAN_PRICES {
        bigint id PK
        bigint plan_id FK
        varchar billing_cycle
        bigint price
        varchar currency
        int seat_limit
        varchar status
        datetime created_at
    }

    SUBSCRIPTIONS {
        bigint id PK
        bigint company_id
        bigint plan_price_id FK
        varchar status
        boolean auto_renew
        datetime current_period_start
        datetime current_period_end
        datetime next_billing_at
        datetime canceled_at
        datetime created_at
        datetime updated_at
    }

    PAYMENTS {
        bigint id PK
        bigint subscription_id FK
        bigint company_id
        varchar idempotency_key
        bigint amount
        varchar currency
        varchar status
        varchar provider_payment_id
        text failure_reason
        datetime requested_at
        datetime paid_at
        datetime failed_at
    }

    OUTBOX_EVENTS {
        bigint id PK
        varchar event_id UK
        varchar aggregate_type
        bigint aggregate_id
        varchar event_type
        text payload
        varchar status
        datetime created_at
        datetime published_at
    }

```

### payment-service 주요 제약조건

| 테이블 | 제약조건 |
| --- | --- |
| `plan_prices` | `(plan_id, billing_cycle)` 유일 |
| `subscriptions` | 기업별 활성 구독은 최대 1개 |
| `subscriptions.auto_renew` | 해지 시 `false`로 변경하고 현재 이용 기간까지 `ACTIVE` 유지 |
| `payments` | `(company_id, idempotency_key)` 유일로 기업별 중복 결제 방지 |
| `payments` | 금액은 요청값이 아니라 `plan_prices.price`에서 서버가 결정 |
| `outbox_events` | 결제·구독 상태 변경과 같은 트랜잭션으로 저장하고 `event_id` 유일 처리 |

`subscriptions.company_id`와 `payments.company_id`는 `user-service`에 대한 논리 참조입니다. `payment-service`는 클라이언트가 보낸 회사 식별값을 신뢰하지 않고, Gateway가 전달한 인증 사용자 ID로 `user-service` 내부 권한 조회 API를 호출해 활성 `COMPANY_ADMIN`의 `companyId`를 확인한 뒤 저장합니다. `payment-service`는 `user-service` 소유 테이블을 직접 조회하지 않습니다.

---

## 9. 추후 확장: 감사 로그

주요 작업 감사 로그는 확정 MVP가 아니라 추후 확장 기능입니다. 확장 시에도 통합 운영 DB를 만들지 않고 각 서비스가 자신의 감사 로그를 소유합니다.

| 소유 서비스 | 추후 테이블 | 기록 대상 예시 |
| --- | --- | --- |
| `user-service` | `user_audit_log` | 초대코드, 직원 상태, 약관 동의 변경 |
| `course-service` | `course_admin_audit_log` | 강의 등록·수정·상태 변경 |
| `enrollment-service` | `enrollment_audit_log` | 수강신청·학습 상태 변경 |
| `payment-service` | `payment_audit_log` | 구독·결제 상태 변경 |

감사 로그를 구현할 때는 append-only 방식으로 저장하고, 플랫폼 관리자 조회 API는 Gateway가 각 서비스의 관리자 API를 조합합니다.

---

## 10. 상태값

### 10.1 인증·사용자·초대

| 엔티티 | 상태 |
| --- | --- |
| `Company` | `ACTIVE`, `INACTIVE` |
| `User` | `ACTIVE`, `INACTIVE`, `WITHDRAWN` |
| `Invitation` | `UNUSED`, `USED`, `EXPIRED`, `REVOKED` |
| 로그인 호환 역할 | `STUDENT`, `INSTRUCTOR` |
| 비즈니스 역할 | `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `EMPLOYEE` |

### 10.2 강의·수강

| 엔티티 | 상태 |
| --- | --- |
| `Course` | `ACTIVE`, `INACTIVE` |
| `Enrollment` | `ENROLLED`, `LEARNING`, `COMPLETED` |
| `LessonProgress` | `NOT_STARTED`, `LEARNING`, `COMPLETED` |

### 10.3 구독·결제

| 엔티티 | 상태 |
| --- | --- |
| `Subscription` | `PENDING`, `ACTIVE`, `CANCELED`, `EXPIRED` |
| `Payment` | `PENDING`, `SUCCESS`, `FAILED` |

### 10.4 AI 추천

| 엔티티 | 상태 |
| --- | --- |
| `Recommendation` | `SUCCESS`, `FALLBACK`, `FAILED` |
| 추천 출처 | `AI`, `RULE_BASED_FALLBACK` |

---

## 11. Kafka 이벤트

구독 이벤트의 발행자는 `payment-service`, 이용 권한 소비자는 `user-service`입니다. 모든 구독 이벤트는 `eventId`를 가지며 소비자는 `processed_event.event_id`로 중복 처리를 방지합니다. `payment-service`는 상태 변경과 같은 트랜잭션에서 `outbox_events`에 이벤트를 저장하고, 단일 Kafka 토픽 `subscription.events`로 발행합니다. 이벤트 key는 `companyId`입니다.

### 11.1 PaymentCompleted

```json
{
  "eventId": "c149229f-c428-4bf9-bb1a-b97431c4f8b5",
  "eventType": "PaymentCompleted",
  "occurredAt": "2026-08-10T10:30:00+09:00",
  "companyId": 10,
  "subscriptionId": 7001,
  "paymentId": 8001,
  "planPriceId": 1,
  "seatLimit": 50,
  "currentPeriodEnd": "2026-09-10T10:30:00+09:00"
}
```

`user-service`는 `entitlement_status=ACTIVE`, 좌석 한도와 현재 이용 종료일을 반영합니다.

### 11.2 PaymentFailed

```json
{
  "eventId": "70fb3a43-d0f6-4af2-b8e2-133299bdfbbb",
  "eventType": "PaymentFailed",
  "occurredAt": "2026-08-10T10:31:00+09:00",
  "companyId": 10,
  "subscriptionId": 7001,
  "paymentId": 8002,
  "planPriceId": 1,
  "failureCode": "CARD_DECLINED",
  "failureReason": "결제가 승인되지 않았습니다."
}
```

초기 결제 실패 시 이용 권한을 활성화하지 않습니다. 갱신 결제 실패 시 현재 이용 종료일 전까지 기존 권한을 유지하며, 별도의 만료 처리 시점에 `SubscriptionExpired`를 발행합니다.

### 11.3 SubscriptionCanceled

```json
{
  "eventId": "7d0c5a16-361b-40ce-b41a-257e29e61f80",
  "eventType": "SubscriptionCanceled",
  "occurredAt": "2026-08-15T09:00:00+09:00",
  "companyId": 10,
  "subscriptionId": 7001,
  "canceledAt": "2026-08-15T09:00:00+09:00",
  "effectiveAt": "2026-09-10T10:30:00+09:00"
}
```

`user-service`는 `auto_renew=false`로 변경하지만 `effectiveAt`까지 `entitlement_status=ACTIVE`를 유지합니다.

### 11.4 SubscriptionExpired

```json
{
  "eventId": "43d96592-3b2c-4ec8-97d3-12c34825eb04",
  "eventType": "SubscriptionExpired",
  "occurredAt": "2026-09-10T10:30:00+09:00",
  "companyId": 10,
  "subscriptionId": 7001,
  "expiredAt": "2026-09-10T10:30:00+09:00"
}
```

`user-service`는 `entitlement_status=EXPIRED`로 변경하고 신규 직원 가입과 신규 수강신청을 차단합니다.

### 11.5 SubscriptionRenewed

```json
{
  "eventId": "a59e5e28-3dcf-4738-82a1-7de8447a9565",
  "eventType": "SubscriptionRenewed",
  "occurredAt": "2026-09-10T10:30:00+09:00",
  "companyId": 10,
  "subscriptionId": 7001,
  "paymentId": 8101,
  "planPriceId": 1,
  "seatLimit": 50,
  "currentPeriodStart": "2026-09-10T10:30:00+09:00",
  "currentPeriodEnd": "2026-10-10T10:30:00+09:00",
  "nextBillingAt": "2026-10-10T10:30:00+09:00"
}
```

`user-service`는 `entitlement_status=ACTIVE`, `auto_renew=true`, 좌석 한도와 새 이용 기간을 반영합니다.

### 11.6 EnrollmentCompleted

`enrollment-service`는 수강신청 생성 시점이 아니라 모든 필수 차시가 완료되어 `Enrollment.status=COMPLETED`로 전환될 때만 `enrollment.completed` 토픽에 아래 이벤트를 발행합니다.

```json
{
  "eventId": "c149229f-c428-4bf9-bb1a-b97431c4f8b5",
  "eventType": "EnrollmentCompleted",
  "occurredAt": "2026-08-11T10:45:00",
  "enrollmentId": 9001,
  "userId": 101,
  "courseId": 9101
}
```

현재 `recommend-service`는 이 이벤트를 수신해 로그로 기록하며 기존 추천 이력을 갱신하지 않습니다. 추천 요청 시점마다 `course-service`와 `enrollment-service` 내부 조회 API에서 최신 기준 데이터를 가져오고 새 추천 요청·결과를 저장합니다.

### 11.7 구독 이벤트 처리 흐름

```text
payment-service가 상태 변경과 같은 트랜잭션에서 Outbox 이벤트 저장
→ Kafka로 이벤트 발행
→ user-service가 eventId 중복 여부 확인
→ company_entitlement 반영
→ processed_event 저장
```

상태 변경과 이벤트 발행 사이의 유실을 방지하기 위해 Transactional Outbox 방식을 사용합니다.

MVP에서는 미발행 Outbox 이벤트를 5초 간격으로 재시도하고 성공할 때까지 삭제하지 않습니다. `processed_event`는 프로젝트 기간 동안 삭제하지 않아 동일한 `eventId`의 재수신을 계속 차단합니다.

---

## 12. 삭제 정책

| 데이터 | 정책 |
| --- | --- |
| 기업·사용자 | 물리 삭제 대신 `INACTIVE` 또는 `WITHDRAWN` 상태 사용 |
| 강의 | 수강 이력 보존을 위해 물리 삭제 대신 `INACTIVE` 사용 |
| 초대코드 | 폐기 시 `REVOKED`, 만료 시 `EXPIRED` 사용 |
| 수강 이력 | 학습 기록 보존을 위해 물리 삭제하지 않음 |
| 결제 | 회계 및 감사 목적으로 삭제하지 않음 |
| 추천 이력 | MVP에서는 삭제 API를 제공하지 않으며, 같은 서비스 안에서 추천을 삭제할 경우 항목은 cascade 삭제 |
| 감사 로그 | 추후 구현 시 수정·삭제하지 않는 append-only 방식 사용 |

---

## 13. 데이터 구현 체크리스트

| 확인 항목 | 이유 |
| --- | --- |
| 제공 백엔드 엔티티와 테이블 | 현재 ERD와 실제 구조의 차이 확인 |
| 데이터베이스 | MariaDB 한 개와 `lecture_db`를 유지하고 서비스별 소유 테이블만 변경 |
| 서비스 경계 | 다른 서비스 소유 테이블 직접 조회·조인과 서비스 사이 외래키가 없는지 확인 |
| 논리 참조 | 서비스 밖의 ID가 논리 참조로만 저장되는지 확인 |
| 이벤트 전달 보장 | Outbox를 5초 간격으로 재시도하고 `processed_event`는 프로젝트 기간 동안 보관 |
| 추천 데이터 | `recommendations`, `recommendation_items`가 초기 DDL·SQLAlchemy Entity와 일치하고 `recommendationId`가 영속 PK인지 확인 |
