# LinguaRoute API 명세서

## 1. 문서 정보

| 항목 | 내용 |
| --- | --- |
| 대상 서비스 | AI 기반 기업 외국어교육 구독 플랫폼 |
| API 진입점 | API Gateway |
| 기본 경로 | `/api` |
| 데이터 형식 | `application/json` |
| 인증 방식 | `Authorization: Bearer {accessToken}` |
| 날짜 형식 | ISO 8601, 예: `2026-08-10T10:30:00+09:00` |
| 문서 상태 | 인증·구독·직원·강의·수강·AI 추천·플랫폼 운영 API의 2026-08-11 구현 및 통합 검증 결과 반영 |

구현·검증 범위는 [MVP 통합 검증 기록](./mvp-verification.md)을 따릅니다. 외부·보호·내부 API와 OAuth2 흐름을 포함한 curl 회귀와 역할별 브라우저 검증 결과를 실제 코드·Swagger UI와 함께 대조했습니다.

기존 API Gateway 서버는 유지하고 새 Gateway 서버를 추가하지 않습니다. 제공 Gateway 이미지는 수정하지 않으며, `docker-compose.yml` 환경변수로 가능한 라우팅만 보정합니다. 공개 허용 경로가 이미지에 고정된 경우에는 해당 경로를 MVP 외부 계약으로 사용합니다.

일반 외부 API는 기본 경로 `/api`를 사용합니다. 로그인만 Auth Server의 OAuth2 경로(`/oauth2/**`, `/login`)를 사용하며, 자체 이메일·비밀번호 로그인 후 Authorization Code를 JWT Access Token으로 교환합니다. 소셜 로그인은 사용하지 않습니다. Auth Server가 Refresh Token을 반환해도 MVP 프론트엔드는 저장·갱신에 사용하지 않습니다.

---

## 2. 공통 규칙

### 2.1 권한

| 권한 | 설명 |
| --- | --- |
| `PLATFORM_ADMIN` | 전체 기업·사용자·강의·결제·수강 상태 관리 |
| `COMPANY_ADMIN` | 자신의 기업, 직원, 좌석, 구독 및 결제 관리 |
| `EMPLOYEE` | 강의 조회·수강신청·학습·AI 추천 사용 |

### 2.2 공통 성공 응답

```json
{
  "data": {},
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### 2.3 공통 오류 응답

```json
{
  "code": "INVITATION_EXPIRED",
  "message": "만료된 초대코드입니다.",
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### 2.4 공통 상태 코드

| 상태 코드 | 사용 상황 |
| --- | --- |
| `200 OK` | 조회·수정·취소 성공 |
| `201 Created` | 회원·초대코드·강의·수강신청·결제 생성 성공 |
| `202 Accepted` | 이메일 발송 요청 접수 |
| `204 No Content` | 응답 본문이 필요 없는 삭제·폐기 성공 |
| `400 Bad Request` | 형식 오류 또는 필수값 누락 |
| `401 Unauthorized` | 로그인 또는 토큰 필요 |
| `403 Forbidden` | 역할 또는 기업 소속 권한 부족 |
| `404 Not Found` | 대상 자원 없음 |
| `409 Conflict` | 중복 가입·중복 신청·중복 결제·좌석 초과 |
| `422 Unprocessable Entity` | 상태상 처리할 수 없는 요청 |
| `503 Service Unavailable` | 외부 AI 또는 결제 시스템 장애 |

### 2.5 내부 API 보안

외부 클라이언트가 호출하는 API는 API Gateway의 `/api/**` 경로만 사용합니다. 서비스 간 내부 API는 Gateway에 노출하지 않고 대상 서비스를 직접 호출하는 `/internal/**` 경로로 분리합니다.

- Gateway는 외부 요청의 `/api/**/internal/**` 접근을 차단합니다.
- 내부 API 제공 서비스는 `X-Internal-Api-Key` 값을 실행 환경의 `INTERNAL_API_KEY`와 비교하여 검증합니다.
- 내부 API 호출 서비스는 동일한 `INTERNAL_API_KEY` 값을 `X-Internal-Api-Key` 헤더로 전달합니다.
- 각 실행 환경에는 동일한 `INTERNAL_API_KEY`를 주입합니다. 로컬 개발 기본값을 쓰더라도 운영·공유 환경의 실제 키는 문서나 저장소에 기록하지 않습니다.
- 신규 내부 API는 `/api/{service}/internal/**` 형태로 만들지 않습니다. 기존 코드에 남아 있는 `/api/**/internal/**` 경로는 담당 범위에서 `/internal/**`로 이전하고 호출 코드를 함께 수정합니다.

---

## 3. 인증 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| AUTH-01 | `GET`·`POST` | `/oauth2/authorize` → `/oauth2/token` | 공개 | 자체 이메일·비밀번호 로그인과 JWT Access Token 발급 | 필수 |
| AUTH-03 | `POST` | `/api/users/register?action=request-password-reset` | 공개 | 비밀번호 재설정 요청 | 필수 |
| AUTH-04 | `POST` | `/api/users/register?action=confirm-password-reset` | 공개 | 재설정 토큰으로 비밀번호 변경 | 필수 |
| AUTH-05 | `POST` | `/api/users/register?action=request-email-verification` | 공개 | 이메일 인증 요청 | 필수 |
| AUTH-06 | `POST` | `/api/users/register?action=confirm-email-verification` | 공개 | 이메일 인증 확인 | 필수 |
| AUTH-07 | `POST` | `/api/users/register?action=request-id-find` | 공개 | 아이디 찾기 | 필수 |
| AUTH-08 | `PUT` | `/api/users/me/password` | 로그인 | 비밀번호 변경 | 필수 |
| AUTH-09 | `POST` | `/api/users/register?action=exchange-oauth-code` | 공개 | Authorization Code를 Access Token으로 교환 | 필수 |
| AUTH-10 | `POST` | `/api/users/register?action=validate-invitation` | 공개 | 직원 가입 전 초대코드·만료·사용·구독·좌석 검증 | 필수 |

수업 가이드의 JSON `POST /api/users/login` 예시는 현재 제공 Gateway/Auth 이미지와 다르며 실제 요청은 `401`을 반환합니다. 제공 Auth Server는 `authorization_code` grant를 지원하므로, 자체 이메일·비밀번호 로그인 후 `/oauth2/authorize`에서 Authorization Code를 받고 `/oauth2/token`에서 JWT를 발급받는 계약을 사용합니다. `AUTH-03`부터 `AUTH-08`의 보조 인증 기능은 `user-service`가 소유합니다. 제공 Gateway 이미지는 신규 공개 `/api/auth/**` 경로를 허용하지 않으므로 공개 보조 인증 API는 기존 공개 가입 경로 `POST /api/users/register`에 `action` 쿼리 파라미터를 사용합니다. `action`이 없으면 기존 기업 대표계정 가입으로 처리합니다.

이메일 인증은 SMTP로 6자리 코드를 보내며 로컬 개발에서는 MailHog를 사용합니다. SMTP 접속 정보와 발신 주소는 환경 변수로 주입합니다. 인증 코드와 가입용 토큰은 해시로 저장하고 각각 15분 동안 한 번만 사용할 수 있습니다. 이메일 인증 요청은 이메일별 1분에 1회, 1시간에 5회로 제한합니다. 비밀번호 재설정 토큰도 해시로 저장하며 요청은 계정 존재 여부와 무관하게 같은 응답을 반환하고, 활성 계정별 1분에 1회, 1시간에 5회로 제한합니다.

### AUTH-01 로그인

브라우저는 Auth Server 로그인 화면으로 이동하고, 성공 후 콜백으로 받은 Authorization Code를 서버 측에서 토큰으로 교환합니다. 등록된 브라우저 클라이언트의 비밀값은 프론트엔드 번들에 포함하지 않습니다.

```http
GET /oauth2/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20read%20write&state={random-state}
```

로그인 페이지에서 이메일과 비밀번호를 제출하면 Auth Server가 BCrypt 비밀번호 해시를 검증합니다. 성공 시 `redirect_uri`에 `code`와 `state`를 붙여 리디렉션합니다. 콜백은 `AUTH-09`로 코드를 전달하고 user-service가 서버 측 코드 교환을 수행합니다. 성공 시 프론트가 받는 응답은 다음과 같습니다.

```json
{
  "data": { "accessToken": "jwt-access-token", "tokenType": "Bearer", "expiresIn": 300 },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

MVP에서는 Authorization Code로 Access Token을 발급받고 sessionStorage에 Access Token만 저장합니다. Refresh Token이 응답에 포함되어도 저장·사용하지 않습니다. Access Token이 만료되면 클라이언트는 로그인 화면으로 이동하고 사용자가 다시 로그인하여 새 Access Token을 발급받습니다. 로그아웃은 Auth Server 세션 종료 요청과 클라이언트 Access Token 삭제를 함께 처리한 뒤 LinguaRoute 첫 화면(`/`)으로 이동합니다.

콜백 화면은 다음 Gateway 공개 API로 Authorization Code를 전달합니다. 배포된 로컬 Auth Server 이미지를 사용하는 Compose 환경은 이미지에 등록된 실습용 `web-client` 값으로 동작하므로 팀원이 `AUTH_WEB_CLIENT_SECRET`을 별도로 설정하지 않습니다. 다른 Auth Server 또는 운영 환경에서는 `user-service`에만 해당 환경의 `AUTH_WEB_CLIENT_SECRET`을 주입하며 프론트엔드 번들에는 포함하지 않습니다.

```http
POST /api/users/register?action=exchange-oauth-code
Content-Type: application/json

{"code":"authorization-code"}
```

Auth Server가 사용하는 기존 `users.role`은 `EMPLOYEE`일 때 `STUDENT`, `COMPANY_ADMIN` 또는 `PLATFORM_ADMIN`일 때 `INSTRUCTOR`로 저장합니다. 실제 권한과 기업 소속은 `user-service`의 `business_role`, `company_id`, `status`를 기준으로 보호 API에서 확인합니다. Gateway가 전달한 기존 역할 값만으로 비즈니스 권한을 결정하지 않습니다.

직원 가입 화면은 최종 가입 전에 다음 공개 요청으로 초대코드를 검사합니다. 제공 Gateway의 공개 경로가 고정되어 있으므로 신규 `/api/employees/**` 공개 경로를 만들지 않고 기존 공개 가입 경로의 `action` 계약을 사용합니다. 이 검사는 코드를 소비하지 않으며 최종 가입 트랜잭션이 같은 조건을 다시 검사합니다.

```http
POST /api/users/register?action=validate-invitation
Content-Type: application/json

{"invitationCode":"ABCD-EFGH"}
```

### AUTH-03·04 비밀번호 재설정

재설정 요청:

```http
POST /api/users/register?action=request-password-reset
```

```json
{
  "email": "admin@company.com"
}
```

계정 존재 여부와 관계없이 `202 Accepted`와 공통 성공 형식을 반환합니다. 계정이 존재하면 등록된 이메일로 15분 동안 한 번만 사용할 수 있는 재설정 링크를 발송합니다.

재설정 확인:

```http
POST /api/users/register?action=confirm-password-reset
```

```json
{
  "resetToken": "password-reset-token",
  "newPassword": "NewPassword123!"
}
```

토큰이 유효하면 비밀번호를 변경하고 `200 OK`를 반환합니다. 사용되었거나 만료된 토큰은 `422 Unprocessable Entity`로 처리합니다.

### AUTH-05 이메일 인증 요청

```http
POST /api/users/register?action=request-email-verification
Content-Type: application/json
```

```json
{
  "email": "admin@company.com",
  "purpose": "SIGNUP"
}
```

응답 `202 Accepted`:

```json
{
  "data": {
    "accepted": true
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

서버는 이메일을 소문자로 정규화하고 6자리 인증 코드를 SMTP로 발송합니다. 코드 원문은 저장하지 않습니다.

### AUTH-06 이메일 인증 확인

```http
POST /api/users/register?action=confirm-email-verification
Content-Type: application/json
```

```json
{
  "email": "admin@company.com",
  "verificationCode": "123456"
}
```

성공 시 회원가입 요청에 사용할 일회용 `emailVerificationToken`을 반환합니다. 인증 코드와 토큰에는 15분 만료시간을 적용하고 재사용을 금지합니다. 가입 요청은 `action` 없이 기존 `POST /api/users/register` 본문을 사용합니다.

응답 `200 OK`:

```json
{
  "data": {
    "emailVerificationToken": "email-verification-token"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### AUTH-07 아이디 찾기

요청:

```http
POST /api/users/register?action=request-id-find
```

```json
{
  "name": "이직원",
  "businessNumber": "123-45-67890"
}
```

응답 `202 Accepted`:

```json
{
  "data": {
    "accepted": true
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

`user-service`는 이름과 기업 사업자번호가 일치하는 활성 사용자를 확인하고, 계정이 존재하면 등록된 로그인 이메일로 아이디 안내 메일을 보냅니다. 계정 존재 여부를 노출하지 않도록 미일치 요청에도 같은 `202 Accepted` 응답을 반환하며, 화면이나 API 응답에는 이메일을 표시하지 않습니다.

### AUTH-08 로그인 사용자 비밀번호 변경

```http
PUT /api/users/me/password
Authorization: Bearer {accessToken}
```

```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword123!"
}
```

현재 비밀번호가 일치하면 변경하고 `200 OK`를 반환합니다. 불일치는 `422 INVALID_PASSWORD`로 처리합니다.

---

## 4. 기업·사용자 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| COMPANY-01 | `POST` | `/api/users/register` | 공개 | 기업 대표계정 회원가입 | 필수 |
| COMPANY-02 | `GET` | `/api/companies/me` | 기업 관리자 | 기업 정보 조회 | 필수 |
| COMPANY-03 | `POST` | `/api/companies/me?action=update-company` | 기업 관리자 | 기업 정보 수정 | 필수 |
| USER-01 | `GET` | `/api/users/me` | 로그인 | 내 정보 조회 | 필수 |
| USER-02 | `POST` | `/api/users/me?action=update-profile` | 로그인 | 내 정보 수정 | 필수 |
| USER-03 | `PUT` | `/api/users/me/password` | 로그인 | 현재 비밀번호 확인 후 변경 | 필수 |
| USER-04 | `DELETE` | `/api/users/me` | 로그인 | 회원 탈퇴 | 필수 |

회원 탈퇴 시 `user-service`가 사용자를 `WITHDRAWN` 처리하고 비밀번호 해시를 로그인할 수 없는 임의 값으로 교체합니다. 이미 발급된 Access Token은 만료 전까지 남을 수 있으므로 보호 API는 사용자 상태를 확인해 탈퇴 사용자의 요청을 거부합니다.

로그인 이메일과 비밀번호 해시의 원본은 공용 `users` 테이블이며 `user-service`가 관리합니다. MVP에서는 `USER-02`로 로그인 이메일을 변경하지 않습니다.

### COMPANY-01 기업 대표계정 회원가입

Gateway 제공 이미지가 공개 허용하는 가입 경로에 맞춰 외부 클라이언트는 `POST /api/users/register`를 사용합니다. `user-service`는 같은 요청 구조를 기존 `POST /api/companies`에서도 처리하지만, Gateway 경유 MVP 흐름의 기준 경로는 `/api/users/register`입니다.

요청:

```json
{
  "company": {
    "name": "스칼라테크",
    "businessNumber": "123-45-67890"
  },
  "admin": {
    "email": "admin@company.com",
    "password": "Password123!",
    "name": "김관리"
  },
  "emailVerificationToken": "email-verification-token",
  "agreementIds": [1, 2]
}
```

응답 `201 Created`:

```json
{
  "data": {
    "companyId": 10,
    "userId": 101,
    "role": "COMPANY_ADMIN",
    "status": "ACTIVE"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

Gateway는 회원가입 요청을 `user-service`로 전달합니다. `user-service`가 이메일 인증 토큰과 필수 약관을 확인하고, 기업·사용자·약관 동의를 한 트랜잭션에서 생성합니다. 비밀번호는 BCrypt 해시로 `users.password`에 저장하고 원문은 저장하지 않습니다.

오류 코드:

| HTTP | 오류 코드 | 조건 |
| --- | --- | --- |
| `409` | `DUPLICATE_BUSINESS_NUMBER` | 이미 등록된 사업자번호 |
| `409` | `DUPLICATE_EMAIL` | 이미 가입한 이메일 |
| `400` | `INVALID_AGREEMENT` | 현재 활성 약관이 아닌 ID가 포함됨 |
| `422` | `REQUIRED_AGREEMENT_MISSING` | 현재 필수 약관 동의 누락 |
| `422` | `INVALID_EMAIL_VERIFICATION` | 인증 토큰이 없거나 이메일 불일치·만료·사용됨 |

### COMPANY-02 기업 정보 조회

검증된 Access Token의 `userId`에 해당하는 사용자가 `COMPANY_ADMIN`인지 확인하고, 해당 사용자의 소속 기업만 반환합니다.

응답 `200 OK`:

```json
{
  "data": {
    "id": 10,
    "name": "스칼라테크",
    "businessNumber": "1234567890",
    "status": "ACTIVE"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### COMPANY-03 기업 정보 수정

요청:

```json
{
  "name": "스칼라글로벌"
}
```

사업자번호는 이 API에서 변경하지 않습니다.

### USER-02 내 정보 수정

요청:

```json
{
  "name": "김수정"
}
```

---

## 5. 직원 초대·좌석 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| INVITE-01 | `POST` | `/api/companies/me/invitations` | 기업 관리자 | 일회용 초대코드 생성 | 필수 |
| INVITE-02 | `GET` | `/api/companies/me/invitations` | 기업 관리자 | 초대코드 목록·상태 조회 | 필수 |
| INVITE-03 | `DELETE` | `/api/companies/me/invitations/{invitationId}` | 기업 관리자 | 초대코드 폐기 | 필수 |
| INVITE-04 | `POST` | `/api/companies/me/invitations/{invitationId}/reissue` | 기업 관리자 | 초대코드 재발급 | 필수 |
| EMPLOYEE-01 | `POST` | `/api/users/register?action=employee-signup` | 공개 | 초대코드 기반 직원 가입 | 필수 |
| EMPLOYEE-02 | `GET` | `/api/companies/me/employees` | 기업 관리자 | 소속 직원 목록 조회 | 필수 |
| EMPLOYEE-03 | `POST` | `/api/companies/me/employees/{userId}/status?action=update-status` | 기업 관리자 | 직원 비활성화·소속 해제 | 필수 |
| SEAT-01 | `GET` | `/api/companies/me/seats` | 기업 관리자 | 구매·사용·잔여 좌석 조회 | 필수 |

### INVITE-01 초대코드 생성

요청:

```json
{
  "expiresInDays": 7
}
```

응답 `201 Created`:

```json
{
  "data": {
    "invitationId": 501,
    "code": "A7K9-P2QM",
    "codeMasked": "****-****",
    "status": "UNUSED",
    "expiresAt": "2026-08-17T23:59:59+09:00"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

초대코드 원문은 해시만 저장하므로 `POST` 생성·재발급 응답의 `code`에서만 한 번 제공됩니다. 기업 관리자는 이 응답을 복사해 전달해야 하며, 이후 목록 조회에서는 `code`가 `null`이고 `codeMasked`만 제공됩니다.

### INVITE-02 목록 조회와 INVITE-03 폐기, INVITE-04 재발급

```http
GET /api/companies/me/invitations
DELETE /api/companies/me/invitations/{invitationId}
POST /api/companies/me/invitations/{invitationId}/reissue
```

목록은 `UNUSED`, `USED`, `EXPIRED`, `REVOKED` 상태와 생성·만료·사용 시각을 반환합니다. 만료 시각이 지난 `UNUSED` 초대코드는 조회 시 `EXPIRED`로 반영됩니다. 폐기는 `UNUSED` 초대코드만 `REVOKED`로 변경합니다. 재발급은 기존 `UNUSED` 초대코드를 먼저 폐기하고 새 일회용 코드를 `7`일 유효 기간으로 발급합니다.

```json
{
  "data": [
    {
      "invitationId": 501,
      "code": null,
      "codeMasked": "****-****",
      "status": "UNUSED",
      "expiresAt": "2026-08-17T23:59:59+09:00",
      "createdAt": "2026-08-10T10:30:00+09:00",
      "usedAt": null
    }
  ],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### EMPLOYEE-01 직원 가입

요청:

```json
{
  "invitationCode": "A7K9-P2QM",
  "email": "employee@company.com",
  "password": "Password123!",
  "name": "이직원",
  "emailVerificationToken": "email-verification-token",
  "agreementIds": [1, 2]
}
```

서버 처리 순서:

```text
초대코드 존재·만료·사용 여부 검사
→ 이메일 인증 토큰과 필수 약관 동의 검사
→ 기업 구독 ACTIVE 검사
→ 잔여 좌석 검사
→ 직원 계정 생성
→ 좌석 배정
→ 초대코드 USED 처리
```

비밀번호와 `emailVerificationToken`은 `user-service`가 처리합니다. 직원 가입은 사용자·좌석·초대코드 상태를 한 트랜잭션에서 처리하고 실패하면 모두 롤백합니다. 비밀번호 원문은 저장하지 않습니다.

오류 코드:

| HTTP | 오류 코드 | 조건 |
| --- | --- | --- |
| `404` | `INVITATION_NOT_FOUND` | 존재하지 않는 초대코드 |
| `409` | `INVITATION_ALREADY_USED` | 이미 사용한 코드 |
| `409` | `SEAT_LIMIT_EXCEEDED` | 잔여 좌석 없음 |
| `422` | `INVITATION_EXPIRED` | 코드 만료 |
| `422` | `SUBSCRIPTION_INACTIVE` | 구독 비활성 |
| `422` | `INVITATION_REVOKED` | 폐기된 초대코드 |

### SEAT-01 좌석 조회 응답

```json
{
  "data": {
    "purchased": 50,
    "used": 42,
    "remaining": 8
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### EMPLOYEE-02 직원 목록, EMPLOYEE-03 상태 변경

`GET /api/companies/me/employees`는 요청한 기업 관리자의 소속 직원만 반환합니다. 수강 강의·진도는 `enrollment-service` 소유 데이터이므로 이 API에 포함하지 않습니다.

```json
{
  "data": [{
    "userId": 101,
    "email": "employee@company.com",
    "name": "이직원",
    "status": "ACTIVE",
    "joinedAt": "2026-08-10T10:30:00"
  }],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

`POST /api/companies/me/employees/{userId}/status?action=update-status` 요청은 아래 상태 중 하나를 사용합니다.

```json
{ "status": "INACTIVE" }
```

- `ACTIVE`: 비활성 직원을 다시 활성화하며, 활성 구독과 잔여 좌석을 확인한 뒤 좌석을 배정합니다.
- `INACTIVE`: 기업 소속은 유지하고 계정을 비활성화하며, 활성 직원 수에서 제외해 좌석을 회수합니다.
- `RELEASED`: 계정은 삭제하지 않고 기업 소속을 해제하며 계정을 비활성화합니다. 이후 해당 기업의 직원 목록에서는 조회되지 않습니다.

`SEAT-01`의 `purchased`는 현재 활성 구독의 좌석 한도이고, `used`는 해당 기업의 `ACTIVE` 직원 수, `remaining`은 `purchased - used`입니다. 활성 구독이 없거나 만료되었으면 직원 활성화·좌석 조회는 `422 SUBSCRIPTION_INACTIVE`를 반환합니다.

### 내부 구독 권한 조회

`enrollment-service`는 수강신청 전에 다음 내부 API로 기업의 최신 구독 권한을 확인합니다. 이 경로는 API Gateway의 외부 공개 경로에 노출하지 않습니다.

```http
GET /internal/companies/{companyId}/entitlement
X-Internal-Api-Key: {internalApiKey}
```

응답 `200 OK`:

```json
{
    "data": {
      "companyId": 10,
      "subscriptionId": 7001,
      "subscriptionStatus": "ACTIVE",
      "seatLimit": 50,
      "currentPeriodEnd": "2026-09-10T10:30:00+09:00",
      "autoRenew": true
    },
    "timestamp": "2026-08-10T10:30:00+09:00"
  }
```

내부 API 키가 잘못되면 `403 Forbidden`, 기업 또는 권한 정보가 없으면 `404 Not Found`를 반환합니다. `enrollment-service`는 호출 실패나 `ACTIVE`가 아닌 상태에서 수강신청을 허용하지 않고 `503 Service Unavailable` 또는 `422 SUBSCRIPTION_INACTIVE`를 반환합니다.

### 내부 사용자 권한 조회

Auth Server의 기존 `users.role`은 로그인 호환용이므로 각 보호 API는 다음 내부 API로 실제 비즈니스 권한을 확인합니다. 이 경로는 API Gateway에 공개하지 않습니다.

```http
GET /internal/users/{userId}/authorization-context
X-Internal-Api-Key: {internalApiKey}
```

응답 `200 OK`:

```json
{
  "data": {
    "userId": 101,
    "companyId": 10,
    "businessRole": "COMPANY_ADMIN",
    "status": "ACTIVE"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

호출 서비스는 `status=ACTIVE`인지 확인하고 필요한 `businessRole`과 `companyId` 범위를 검증합니다. 조회 실패 시 권한을 허용하지 않습니다.

---

## 6. 강의·차시 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| COURSE-01 | `GET` | `/api/courses` | 직원 | 목록·검색·필터 | 필수 |
| COURSE-02 | `GET` | `/api/courses/{courseId}` | 직원 | 강의 상세 조회 | 필수 |
| COURSE-03 | `GET` | `/api/courses/{courseId}/lessons` | 직원 | 강의 차시 목록 조회 | 필수 |
| ADMIN-COURSE-00 | `GET` | `/api/admin/courses` | 플랫폼 관리자 | 전체 강의·상태 검색 | 필수 |
| ADMIN-COURSE-00A | `GET` | `/api/admin/courses/{courseId}` | 플랫폼 관리자 | 활성·비활성 강의 상세 조회 | 필수 |
| ADMIN-COURSE-00B | `GET` | `/api/admin/courses/{courseId}/lessons` | 플랫폼 관리자 | 활성·비활성 강의 차시 조회 | 필수 |
| ADMIN-COURSE-01 | `POST` | `/api/admin/courses` | 플랫폼 관리자 | 강의 등록 | 필수 |
| ADMIN-COURSE-02 | `POST` | `/api/admin/courses/{courseId}?action=update-course` | 플랫폼 관리자 | 강의 수정 | 필수 |
| ADMIN-COURSE-03 | `POST` | `/api/admin/courses/{courseId}/status?action=update-status` | 플랫폼 관리자 | 강의 활성·비활성 | 필수 |
| ADMIN-LESSON-01 | `POST` | `/api/admin/courses/{courseId}/lessons` | 플랫폼 관리자 | 강의 차시 등록 | 필수 |

### COURSE-01 목록·검색·필터

> 현재 `dev` 구현은 목표 페이징 API를 아직 제공하지 않습니다. 추천 서비스는
> 강의 서비스가 제공하는 아래 내부 계약을 사용하며, 외부 응답의 `id`를 추천
> 도메인의 `courseId`로 변환합니다.

```http
GET /internal/courses/recommend?language=ENGLISH&excludeIds=3&excludeIds=5
X-Internal-Api-Key: {internalApiKey}
```

이 내부 API는 지정 언어의 `ACTIVE` 강의 중 `excludeIds`에 없는 강의를 배열로 반환합니다.
서비스 간 계약이므로 API Gateway의 외부 강의 경로로 공개하지 않습니다. Gateway는
`/internal/**`와 전환 기간의 `/api/courses/internal/**` 요청을 `404`로 차단하고,
recommend-service는 Gateway를
거치지 않고 course-service를 직접 호출하면서 내부 API 키를 전달합니다.

요청 예시:

```http
GET /api/courses?keyword=미팅&language=ENGLISH&situation=CUSTOMER_MEETING&level=INTERMEDIATE&page=0&size=20
```

응답 `200 OK`:

```json
{
  "data": {
    "content": [
      {
        "courseId": 12,
        "title": "해외 고객 미팅 영어",
        "language": "ENGLISH",
        "situation": "CUSTOMER_MEETING",
        "level": "INTERMEDIATE",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

- `keyword`는 강의명 부분 검색입니다.
- `language`, `situation`, `level`은 enum 값과 정확히 일치해야 합니다.
- 직원용 목록과 상세 조회에는 `ACTIVE` 강의만 노출됩니다. 비활성 강의 상세 조회는 `422 COURSE_INACTIVE`를 반환합니다.

### COURSE-02 강의 상세 조회

응답 `200 OK`:

```json
{
  "data": {
    "id": 12,
    "title": "해외 고객 미팅 영어",
    "description": "해외 고객 미팅에서 제품 사양과 기술 용어를 설명하는 비즈니스 영어 과정",
    "language": "ENGLISH",
    "situation": "CUSTOMER_MEETING",
    "level": "INTERMEDIATE",
    "status": "ACTIVE",
    "createdAt": "2026-08-10T10:30:00",
    "updatedAt": "2026-08-10T10:30:00"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### COURSE-03 강의 차시 목록 조회

응답 `200 OK`:

```json
{
  "data": [
    {
      "lessonId": 1201,
      "title": "고객 미팅 시작하기",
      "contentUrl": "https://example.com/lessons/1201",
      "sequence": 1,
      "required": true,
      "durationSeconds": 900
    }
  ],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

직원용 강의 상세 조회와 동일하게 `ACTIVE` 강의만 조회할 수 있습니다.

### ADMIN-COURSE-00 관리자 강의 목록

플랫폼 관리자는 `GET /api/admin/courses`에서 `keyword`, `language`, `situation`, `level`, `status`, `page`, `size`를 사용할 수 있습니다. 직원용 목록과 달리 `status`를 생략하면 `ACTIVE`와 `INACTIVE`를 모두 반환하므로 비활성 강의를 다시 활성화할 수 있습니다. API Gateway가 전달한 사용자 ID를 내부 권한 API로 재검증합니다.

관리자 수정 화면은 `GET /api/admin/courses/{courseId}`와 `GET /api/admin/courses/{courseId}/lessons`를 사용합니다. 두 API는 직원용 상세 API와 달리 `INACTIVE` 강의도 반환하며, 활성 상태를 바꾸기 전에 현재 저장된 기본 정보와 차시를 그대로 확인할 수 있습니다.

### ADMIN-COURSE-01 강의 등록

API Gateway가 인증한 사용자 ID를 `X-User-Id` 헤더로 전달합니다. `course-service`는 내부 사용자 권한 조회 API로 활성 상태의 `PLATFORM_ADMIN`인지 다시 확인합니다.

```json
{
  "title": "해외 고객 미팅 영어",
  "description": "해외 고객 미팅에서 제품 사양과 기술 용어를 설명하는 비즈니스 영어 과정",
  "language": "ENGLISH",
  "situation": "CUSTOMER_MEETING",
  "level": "INTERMEDIATE"
}
```

등록 성공 시 `201 Created`, 수정 및 상태 변경 성공 시 `200 OK`를 반환합니다.

### ADMIN-COURSE-02 강의 수정

```json
{
  "title": "해외 고객 미팅 영어 실전",
  "description": "실전 중심으로 수정된 과정",
  "language": "ENGLISH",
  "situation": "CUSTOMER_MEETING",
  "level": "ADVANCED"
}
```

### ADMIN-COURSE-03 강의 활성·비활성

```json
{
  "status": "INACTIVE"
}
```

플랫폼 관리자 권한이 아니면 `403 PLATFORM_ADMIN_REQUIRED`, 사용자 권한 서비스에 연결할 수 없으면 `503 USER_AUTHORIZATION_UNAVAILABLE`를 반환합니다.

### ADMIN-LESSON-01 강의 차시 등록

```json
{
  "title": "고객 미팅 시작하기",
  "contentUrl": "https://example.com/lessons/1201",
  "sequence": 1,
  "required": true,
  "durationSeconds": 900
}
```

성공 시 `201 Created`로 등록된 차시를 반환합니다. 같은 강의의 `sequence`는 중복될 수 없습니다.

### course-service 내부 API

`course-service`의 내부 API는 Gateway에 노출하지 않고 서비스 간 직접 호출에만 사용합니다. 모든 요청은 `X-Internal-Api-Key`를 포함해야 합니다.

#### 추천 후보 조회

```http
GET /internal/courses/recommend?language=ENGLISH&excludeIds=1,2
X-Internal-Api-Key: {internalApiKey}
```

응답 `200 OK`:

```json
[
  {
    "id": 12,
    "title": "해외 고객 미팅 영어",
    "description": "해외 고객 미팅에서 제품 사양과 기술 용어를 설명하는 비즈니스 영어 과정",
    "language": "ENGLISH",
    "situation": "CUSTOMER_MEETING",
    "level": "INTERMEDIATE",
    "status": "ACTIVE",
    "createdAt": "2026-08-10T10:30:00",
    "updatedAt": "2026-08-10T10:30:00"
  }
]
```

#### 수강 가능 여부 확인

```http
GET /internal/courses/{courseId}/enrollment-validation
X-Internal-Api-Key: {internalApiKey}
```

응답 `200 OK`:

```json
{
  "courseId": 12,
  "status": "ACTIVE",
  "enrollable": true
}
```

`enrollable=false`이면 `enrollment-service`는 수강신청을 생성하지 않습니다. 강의가 없으면 `404 COURSE_NOT_FOUND`, 내부 API 키가 없거나 다르면 `401 INVALID_INTERNAL_API_KEY`를 반환합니다.

#### 차시 목록 조회

```http
GET /internal/courses/{courseId}/lessons
X-Internal-Api-Key: {internalApiKey}
```

`enrollment-service`가 차시 소속 검증과 필수 차시 기준 진도율 계산에 사용합니다. 외부 Gateway에는 노출하지 않습니다.

---

## 7. 수강·학습 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| ENROLL-01 | `POST` | `/api/enrollments` | 직원 | 개별 강의 수강신청 | 필수 |
| ENROLL-02 | `GET` | `/api/enrollments/me` | 직원 | 신청한 강의 목록 조회 | 필수 |
| ENROLL-03 | `GET` | `/api/enrollments/{enrollmentId}` | 직원 | 수강 상태·진도율 조회 | 필수 |
| LEARNING-01 | `POST` | `/api/enrollments/{enrollmentId}/lessons/{lessonId}/start` | 직원 | 차시 학습 시작 | 필수 |
| LEARNING-02 | `POST` | `/api/enrollments/{enrollmentId}/lessons/{lessonId}/complete` | 직원 | 차시 완료 | 필수 |
| COMPANY-ENROLL-01 | `GET` | `/api/companies/me/enrollments` | 기업 관리자 | 직원별 수강 상태 조회 | 필수 |
| COMPANY-ENROLL-02 | `GET` | `/api/companies/me/enrollments/progress` | 기업 관리자 | 직원별 진도율 조회 | 필수 |

### ENROLL-01 수강신청

요청:

```json
{
  "courseId": 12
}
```

처리 조건:

```text
로그인 사용자가 EMPLOYEE인가?
→ 기업 구독이 ACTIVE인가?
→ 강의가 ACTIVE인가?
→ 같은 강의를 이미 신청하지 않았는가?
→ 수강신청 생성
```

응답 `201 Created`:

```json
{
  "data": {
    "enrollmentId": 9001,
    "courseId": 12,
    "status": "ENROLLED",
    "progressRate": 0
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

중복 신청은 `409 DUPLICATE_ENROLLMENT`를 반환합니다.

현재 구현은 개별 강의 결제를 요청하지 않습니다. `payment-service`의 구독 결제 결과가 `user-service`의 기업 구독 권한에 반영되어 있고, 해당 권한이 `ACTIVE`인 경우에만 수강신청을 허용합니다.

### ENROLL-02·03 수강 목록·상세

`GET /api/enrollments/me`은 본인의 수강 목록을, `GET /api/enrollments/{enrollmentId}`은 본인 수강의 차시별 상태와 서버 계산 진도율을 반환합니다. 둘 다 활성 `EMPLOYEE` 권한과 같은 기업 소속을 서버에서 재확인합니다.

### LEARNING-01·02 차시 시작·완료

차시 시작과 완료는 각각 `200 OK`로 아래와 같은 응답을 반환합니다. 차시 ID가 해당 강의에 없으면 `422 LESSON_NOT_IN_COURSE`를 반환합니다.

```json
{
  "data": {
    "enrollmentId": 9001,
    "lessonId": 1201,
    "lessonStatus": "COMPLETED",
    "progressRate": 100.0,
    "enrollmentStatus": "COMPLETED",
    "startedAt": "2026-08-10T10:30:00+09:00",
    "completedAt": "2026-08-10T10:45:00+09:00"
  },
  "timestamp": "2026-08-10T10:45:00+09:00"
}
```

서버는 필수 차시의 완료 수를 기준으로 진도율을 계산하고, 첫 시작 시각 및 모든 필수 차시 완료 시각을 저장합니다.

### COMPANY-ENROLL-01·02 직원별 수강 상태·진도율

기업 관리자는 자기 기업의 수강 레코드를 페이지 단위로 조회합니다. `page`(기본 0), `size`(기본 20, 최대 100)를 사용할 수 있습니다. 두 경로 모두 `enrollmentId`, `userId`, `courseId`, `status`, `progressRate`, 학습 시각을 반환하며, `/progress`는 진도율 조회용 별칭입니다. 사용자 이름·부서 정보는 `user-service` 소유이므로 이 API 응답에는 포함하지 않습니다.

### enrollment-service 내부 API

추천 서비스는 이미 신청한 강의를 제외하기 위해 수강 이력을 내부 API로 조회합니다. 이 경로는 Gateway에 노출하지 않고 `X-Internal-Api-Key`를 검증합니다.

```http
GET /internal/enrollments/history/{userId}
X-Internal-Api-Key: {internalApiKey}
```

응답 `200 OK`:

```json
{
  "userId": 101,
  "activeCourseIds": [12, 15]
}
```

`activeCourseIds` 필드명은 추천 서비스 호환을 위해 유지하지만, 실제 의미는 `ENROLLED`, `LEARNING`, `COMPLETED` 상태의 수강 강의 ID 전체입니다. 내부 API 키가 없거나 다르면 `401 INVALID_INTERNAL_API_KEY`를 반환합니다.

### LEARNING-02 차시 완료 응답

```json
{
  "data": {
    "enrollmentId": 9001,
    "lessonId": 103,
    "lessonStatus": "COMPLETED",
    "progressRate": 60,
    "enrollmentStatus": "LEARNING",
    "completedAt": "2026-08-10T14:00:00+09:00"
  },
  "timestamp": "2026-08-10T14:00:00+09:00"
}
```

진도율은 클라이언트가 보내지 않고 서버가 `완료 차시 수 / 전체 필수 차시 수 × 100`으로 계산합니다.

---

## 8. 요금제·구독·결제 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| PLAN-01 | `GET` | `/api/plans` | 기업 관리자 | 월간·연간 요금제 조회 | 필수 |
| SUB-01 | `POST` | `/api/subscriptions` | 기업 관리자 | 구독권 결제 | 필수 |
| SUB-02 | `GET` | `/api/subscriptions/me` | 기업 관리자 | 구독 상태·만료일·갱신일 조회 | 필수 |
| SUB-03 | `POST` | `/api/subscriptions/me/cancel` | 기업 관리자 | 구독 해지 | 필수 |
| PAY-01 | `GET` | `/api/payments` | 기업 관리자 | 결제 내역 조회 | 필수 |

Gateway는 인증된 사용자 ID를 `X-User-Id`로 전달합니다. `payment-service`는 클라이언트가 보낸 회사 식별값을 신뢰하지 않고, `user-service`의 내부 권한 조회 API로 활성 상태의 `COMPANY_ADMIN`인지 확인한 뒤 응답의 `companyId`를 구독·결제 범위로 사용합니다. `payment-service`는 다른 서비스 테이블을 직접 조회하지 않고 `companyId`를 논리 참조로만 저장합니다.

### PLAN-01 요금제 조회 응답

```json
{
  "data": [
    {
      "planPriceId": 1,
      "planName": "BUSINESS_50",
      "billingCycle": "MONTHLY",
      "seatLimit": 50,
      "price": 299000,
      "currency": "KRW"
    },
    {
      "planPriceId": 2,
      "planName": "BUSINESS_50",
      "billingCycle": "YEARLY",
      "seatLimit": 50,
      "price": 2990000,
      "currency": "KRW"
    }
  ],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### SUB-01 구독 결제

헤더:

```http
Authorization: Bearer {accessToken}
Idempotency-Key: 1e7f52d5-c0d5-4a86-aefe-3334f664ee65
```

요청:

```json
{
  "planPriceId": 1,
  "paymentMethodToken": "payment-method-token"
}
```

MVP는 실제 PG나 카드 정보를 사용하지 않습니다. 테스트용 `paymentMethodToken` 값 `mock-success`는 성공, `mock-failure`는 실패 결과를 생성하며 그 외 값은 `400 Bad Request`로 처리합니다.

성공 응답 `201 Created`:

```json
{
  "data": {
    "subscriptionId": 7001,
    "paymentId": 8001,
    "status": "ACTIVE",
    "currentPeriodStart": "2026-08-10T10:30:00+09:00",
    "currentPeriodEnd": "2026-09-10T10:30:00+09:00",
    "nextBillingAt": "2026-09-10T10:30:00+09:00"
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

같은 기업에서 동일한 멱등성 키로 다시 요청하면 새 결제를 생성하지 않고 기존 결과를 반환합니다.

실패 응답 `422 Unprocessable Entity`:

```json
{
  "code": "PAYMENT_FAILED",
  "message": "결제가 승인되지 않았습니다.",
  "timestamp": "2026-08-10T10:31:00+09:00"
}
```

`mock-failure`도 결제 실패 이력과 `PaymentFailed` Outbox 이벤트를 저장합니다. 같은 `Idempotency-Key`로 실패 요청을 반복하면 새 결제 이력을 만들지 않고 기존 실패 결과를 기준으로 응답합니다.

### SUB-03 구독 해지

```json
{
  "reason": "교육 인원 감소"
}
```

해지는 즉시 이용 권한을 제거하지 않고 현재 이용 기간 종료 후 `EXPIRED`가 되도록 설계합니다.

현재 구현은 해지 요청 시 구독 `status=ACTIVE`를 유지하고 `autoRenew=false`, `canceledAt`을 저장합니다. 따라서 현재 이용 기간 종료 전까지 직원 권한을 유지할 수 있고, 만료 처리 시점에 `SubscriptionExpired` 이벤트를 발행합니다.

### PAY-01 결제 내역

인증 사용자의 `user-service` 권한 컨텍스트에서 확인한 기업 기준으로 결제 이력을 최신 요청순으로 반환합니다.

```http
GET /api/payments
Authorization: Bearer {accessToken}
```

응답 `200 OK`:

```json
{
  "data": [
    {
      "paymentId": 8001,
      "subscriptionId": 7001,
      "companyId": 10,
      "amount": 299000,
      "currency": "KRW",
      "status": "SUCCESS",
      "providerPaymentId": "mock-0c7f...",
      "failureReason": null,
      "requestedAt": "2026-08-10T10:30:00+09:00",
      "paidAt": "2026-08-10T10:30:00+09:00",
      "failedAt": null
    }
  ],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### 구독 상태 Kafka 이벤트

`payment-service`는 결제·구독 상태 변경과 같은 트랜잭션에서 `outbox_events`에 이벤트를 저장하고, 스케줄러가 단일 토픽 `subscription.events`로 발행합니다. 이벤트 key는 `companyId`입니다. `user-service`는 같은 토픽을 소비해 `company_entitlements`를 갱신하고 `processed_events.event_id`로 중복 이벤트를 무시합니다.

| 이벤트 | 발행 조건 | `user-service` 처리 | MVP |
| --- | --- | --- | --- |
| `PaymentCompleted` | 결제 성공 | 이용 권한 활성화 및 좌석·기간 반영 | 필수 |
| `PaymentFailed` | 초기 또는 갱신 결제 실패 | 초기 권한 미부여, 기존 권한은 현재 기간까지 유지 | 필수 |
| `SubscriptionCanceled` | 구독 해지 요청 완료 | 자동 갱신 중지, 현재 기간까지 권한 유지 | 필수 |
| `SubscriptionExpired` | 현재 이용 기간 종료 | 이용 권한 만료 및 신규 가입·수강신청 차단 | 필수 |
| `SubscriptionRenewed` | 갱신 결제 성공 | 새 이용 기간과 좌석 한도 반영 | 필수 |

모든 이벤트는 고유한 `eventId`를 포함하고 소비자는 중복 수신을 안전하게 무시해야 합니다. 상세 payload와 권한 상태 변경 규칙은 [ERD의 Kafka 이벤트](./erd.md#11-kafka-이벤트)를 따릅니다.

---

## 9. AI 강의 추천 API

> 외부 경로, 서비스 간 내부 호출, 추천 결과 저장과 프론트 라이브 API 호출을 구현했다. 로컬 Provider와 실제 직원 OAuth 토큰을 사용한 Gateway 종단 간 호출, Gateway 사용자 헤더 덮어쓰기와 MariaDB 저장을 검증했다. OpenAI Provider 재현에는 실행 환경의 `OPENAI_API_KEY`가 필요하며 키가 없으면 로컬 Provider를 사용한다.

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| AI-01 | `POST` | `/api/courses/recommendations` | 직원 | AI 강의 추천 | 필수 |

추천 요청 처리와 결과 생성 책임은 `recommend-service`가 소유합니다. 추천 요청과 결과는 추천 서비스 소유 테이블에 저장합니다. 외부 URL은 API Gateway 계약에 따라 `/api/courses/recommendations`를 유지하며, `recommend-service`는 `course-service` API로 실제 `ACTIVE` 상태 및 요청 언어와 일치하는 강의인지 조회·검증합니다.

### AI-01 추천 요청

```json
{
  "language": "ENGLISH",
  "level": "INTERMEDIATE",
  "job": "GLOBAL_SALES",
  "situation": "CUSTOMER_MEETING",
  "goal": "제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 싶음"
}
```

성공 응답 `200 OK`:

```json
{
  "data": {
    "recommendationId": 3001,
    "source": "RULE_BASED_FALLBACK",
    "courses": [
      {
        "courseId": 12,
        "title": "해외 고객 미팅 영어",
        "language": "ENGLISH",
        "level": "INTERMEDIATE",
        "reason": "선택한 언어, 현재 수준, 업무 상황과 전문용어 학습 목표에 적합한 활성 강의입니다."
      }
    ]
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

현재 연동 골격 응답:

```json
{
  "data": {
    "recommendationId": 3002,
    "source": "RULE_BASED_FALLBACK",
    "courses": [
      {
        "courseId": 12,
        "title": "해외 고객 미팅 영어",
        "reason": "선택한 언어, 수준, 비즈니스 상황과 전문용어 학습 목표에 가까운 강의입니다."
      }
    ]
  },
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

현재 구현은 이미 수강 중인 강의를 후보 조회에서 제외하고, OpenAI 또는 로컬 Provider가 반환한 결과를 실제 `ACTIVE` 강의 및 선택 언어와 다시 대조합니다. 후보에 없는 ID, 중복 ID, 다른 언어와 비활성 강의는 제거하며 최대 3개만 반환합니다. Provider가 실패하거나 유효한 결과가 없으면 수준·상황 일치도를 이용한 규칙 기반 fallback으로 전환하고 요청·결과를 추천 서비스 소유 테이블에 저장합니다. 전문용어 목표와 강의 설명의 직접 매칭은 추후 백엔드 보완 항목입니다.

### recommend-service 내부 의존 API

`recommend-service`는 추천 후보와 기존 수강 이력을 조회할 때 Gateway를 거치지 않고 대상 서비스를 직접 호출합니다. 모든 내부 호출에는 `X-Internal-Api-Key`가 필요합니다.

| 대상 서비스 | 현재 경로 | 비고 |
| --- | --- | --- |
| `user-service` | `/internal/users/{userId}/authorization` | 최신 역할·소속·상태 확인 |
| `course-service` | `/internal/courses/recommend` | `language`, `excludeIds` 파라미터 사용 |
| `enrollment-service` | `/internal/enrollments/history/{userId}` | `activeCourseIds`로 기존 수강 강의 제외 |

`course-service` 추천 후보 응답은 `id`, `title`, `description`, `language`, `situation`, `level`, `status` 구조이며 `recommend-service`의 현재 Pydantic 모델과 일치합니다.

---

## 10. 약관 API

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| TERM-01 | `GET` | `/api/users/register?action=active-terms` | 공개 | 현재 필수·선택 약관 조회 | 필수 |
| TERM-02 | `POST` | `/api/users/me/agreements` | 로그인 | 가입 후 선택 약관 동의 변경 | 필수 |

기업 관리자와 직원 회원가입 요청은 현재 필수 약관의 `agreementIds`를 포함해야 하며, 서버는 누락된 필수 약관이 있으면 가입을 거부합니다.

### TERM-01 활성 약관 조회

제공 Gateway의 공개 허용 경로에 맞춰 외부 클라이언트는 위 경로를 사용한다. user-service 직접 호출 호환 경로인 `GET /api/terms/active`도 같은 응답을 반환한다.

응답 `200 OK`:

```json
{
  "data": [
    {
      "id": 1,
      "type": "SERVICE_TERMS",
      "version": "1.0",
      "content": "LinguaRoute 서비스 이용약관",
      "required": true,
      "effectiveAt": "2026-08-10T00:00:00"
    }
  ],
  "timestamp": "2026-08-10T10:30:00+09:00"
}
```

### TERM-02 약관 동의 저장

요청:

```json
{
  "agreementIds": [3]
}
```

이미 동의한 약관 ID는 중복 저장하지 않으며 정상 처리합니다.

---

## 11. 플랫폼 운영 API

아래 운영 조회 API는 플랫폼 관리자만 호출할 수 있습니다. 사용자·기업은 `user-service`, 결제는 `payment-service`, 수강은 `enrollment-service`가 각각 소유하며 프론트 운영 화면이 Gateway를 통해 결과를 조합합니다. 플랫폼 관리자 계정은 네 API 모두 `200`, 직원 계정은 `403`으로 실제 검증했습니다.

| ID | Method | URL | 권한 | 기능 | MVP |
| --- | --- | --- | --- | --- | --- |
| OPS-01 | `GET` | `/api/admin/users` | 플랫폼 관리자 | 사용자 상태 조회 | 필수 |
| OPS-02 | `GET` | `/api/admin/companies` | 플랫폼 관리자 | 기업 상태 조회 | 필수 |
| OPS-03 | `GET` | `/api/admin/payments` | 플랫폼 관리자 | 결제 상태 조회 | 필수 |
| OPS-04 | `GET` | `/api/admin/enrollments` | 플랫폼 관리자 | 수강 상태 조회 | 필수 |
| OPS-05 | `GET` | `/api/admin/audit-logs` | 플랫폼 관리자 | 주요 감사 로그 조회 | 추후 확장 |

### 11.1 OPS-01 사용자 상태 조회

쿼리 파라미터:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `keyword` | 아니오 | 사용자 이름, 이메일 또는 소속 기업명 부분 검색. 대소문자 구분 없음 |
| `businessRole` | 아니오 | `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `EMPLOYEE` |
| `status` | 아니오 | `ACTIVE`, `INACTIVE`, `WITHDRAWN` |
| `page` | 아니오 | 0부터 시작. 기본값 `0`, 음수는 `0`으로 보정 |
| `size` | 아니오 | 기본값 `20`, 허용 범위 `1~100`으로 보정 |

응답은 `createdAt`, `userId` 내림차순이다.

```json
{
  "data": {
    "content": [
      {
        "userId": 9102,
        "email": "employee.lee@skala-tech.local",
        "name": "이수강",
        "businessRole": "EMPLOYEE",
        "status": "ACTIVE",
        "companyId": 9101,
        "companyName": "스칼라테크",
        "createdAt": "2026-08-10T09:12:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-08-11T09:42:45+09:00"
}
```

플랫폼 관리자는 기업 소속이 없으므로 `companyId`, `companyName`이 `null`일 수 있다.

### 11.2 OPS-02 기업 상태 조회

쿼리 파라미터:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `keyword` | 아니오 | 기업명 또는 사업자번호 부분 검색. 사업자번호의 `-`는 제거해 비교 |
| `status` | 아니오 | `ACTIVE`, `INACTIVE` |
| `page` | 아니오 | 0부터 시작. 기본값 `0`, 음수는 `0`으로 보정 |
| `size` | 아니오 | 기본값 `20`, 허용 범위 `1~100`으로 보정 |

응답은 `createdAt`, `companyId` 내림차순이다. 결제·구독 상세는 payment-service 소유이므로 이 응답에 포함하지 않는다.

```json
{
  "data": {
    "content": [
      {
        "companyId": 9101,
        "name": "스칼라테크",
        "businessNumber": "9910000001",
        "status": "ACTIVE",
        "createdAt": "2026-08-10T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-08-11T09:42:45+09:00"
}
```

두 API는 Bearer Token의 사용자 ID로 user-service의 최신 `businessRole`, `status`를 다시 확인한다. 활성 `PLATFORM_ADMIN`이 아니면 `403 PLATFORM_ADMIN_REQUIRED` 또는 `403 USER_INACTIVE`를 반환한다. 알 수 없는 enum 필터는 `400 BAD_REQUEST`를 반환한다.

- `OPS-01`, `OPS-02`, `OPS-03`은 배열을 `data`로 반환합니다.
- `OPS-04`는 `page`, `size`, `totalElements`, `totalPages`, `content`를 포함한 페이지 응답을 `data`로 반환하며 기본값은 `page=0`, `size=100`입니다.
- `OPS-03`, `OPS-04`는 Gateway의 payment/enrollment 라우트에 `/api/admin/**` 경로를 명시적으로 추가해 해당 소유 서비스로 전달합니다.

---

## 12. 주요 오류 코드

| 오류 코드 | HTTP | 설명 |
| --- | --- | --- |
| `INVALID_PASSWORD` | `422` | 현재 비밀번호 불일치 |
| `INVALID_VERIFICATION_CODE` | `422` | 이메일 인증 코드 불일치·만료·재사용 |
| `EMAIL_VERIFICATION_REQUEST_LIMIT` | `429` | 이메일별 인증 요청 1분 1회 또는 1시간 5회 초과 |
| `INVALID_PASSWORD_RESET_TOKEN` | `422` | 비밀번호 재설정 토큰이 유효하지 않거나 만료·재사용됨 |
| `PASSWORD_RESET_REQUEST_LIMIT` | `429` | 활성 계정별 재설정 요청 1분 1회 또는 1시간 5회 초과 |
| `INVALID_INTERNAL_API_KEY` | `401` 또는 `403` | 서비스 간 내부 API 키 누락·불일치. 신규 내부 API는 `401`을 우선 사용하며, 기존 user-service 내부 API는 현재 구현상 `403`을 반환 |
| `INVALID_EMAIL_VERIFICATION` | `422` | 이메일 인증 토큰이 유효하지 않거나 이미 사용됨 |
| `USER_INACTIVE` | `403` | 비활성 또는 탈퇴 사용자의 보호 API 요청 |
| `DUPLICATE_BUSINESS_NUMBER` | `409` | 이미 등록된 사업자번호 |
| `DUPLICATE_EMAIL` | `409` | 이미 가입한 이메일 |
| `INVALID_AGREEMENT` | `400` | 현재 활성 약관이 아닌 ID |
| `REQUIRED_AGREEMENT_MISSING` | `422` | 필수 약관 동의 누락 |
| `COMPANY_NOT_FOUND` | `404` | 기업 없음 |
| `INVITATION_NOT_FOUND` | `404` | 초대코드 없음 |
| `INVITATION_ALREADY_USED` | `409` | 사용된 초대코드 |
| `INVITATION_EXPIRED` | `422` | 만료된 초대코드 |
| `SEAT_LIMIT_EXCEEDED` | `409` | 좌석 한도 초과 |
| `SUBSCRIPTION_INACTIVE` | `422` | 활성 구독 없음 |
| `DUPLICATE_PAYMENT` | `409` | 중복 결제 요청 |
| `INVALID_USER_CONTEXT` | `401` | Gateway가 전달한 인증 사용자 정보가 없거나 올바르지 않음 |
| `COMPANY_ADMIN_REQUIRED` | `403` | 기업 관리자 권한 필요 |
| `PLATFORM_ADMIN_REQUIRED` | `403` | 플랫폼 관리자 권한 필요 |
| `USER_AUTHORIZATION_UNAVAILABLE` | `503` | 사용자 권한 정보를 확인할 수 없음 |
| `PAYMENT_FAILED` | `422` | 결제 실패 |
| `COURSE_NOT_FOUND` | `404` | 강의 없음 |
| `COURSE_INACTIVE` | `422` | 비활성 강의 |
| `DUPLICATE_ENROLLMENT` | `409` | 중복 수강신청 |
| `ENROLLMENT_NOT_FOUND` | `404` | 수강신청 없음 |
| `AI_RECOMMENDATION_FAILED` | `503` | AI와 대체 추천 모두 실패 |

---

## 13. Swagger 검증 체크리스트

| 확인 항목 | 완료 기준 |
| --- | --- |
| API Gateway 경로 | 모든 프론트 요청이 Gateway 주소를 사용함 |
| 인증 | 로그인 토큰으로 보호 API 호출 성공 |
| 요청 형식 | Swagger Request Body와 본 문서가 일치함 |
| 응답 형식 | Swagger Response와 본 문서가 일치함 |
| 상태 코드 | 정상·예외 상태 코드가 실제 응답과 일치함 |
| 권한 | 기업 관리자·직원·플랫폼 관리자 권한이 분리됨 |
| 결제 이벤트 | 결제 전후 구독 권한 상태 변화 확인 |
| 구독 이벤트 | 실패·해지·만료·갱신 이벤트와 중복 소비 검증 |
| AI 추천 | 실제 활성 강의 ID만 반환되는지 확인 |
