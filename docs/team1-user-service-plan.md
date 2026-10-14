# 팀원 1 `user-service` 구현 기록

> 담당자: 임해안
>
> 담당 영역: 기업·회원·직원·초대코드·좌석
>
> 작업 브랜치: `feature/user-company`
>
> 기준 문서: `docs/product-spec.md`, `docs/api-spec.md`, `docs/erd.md`, `docs/mvp-checklist.md`

> 현재 상태: 기능 브랜치 구현과 `dev` 병합, 자동 테스트 및 Gateway 통합 검증 완료

## 1. 확정된 인증·데이터 경계

- 기존 Auth Server는 자체 이메일·비밀번호 로그인 후 OAuth2 Authorization Code 흐름으로 JWT Access Token을 발급한다. 소셜 로그인은 사용하지 않는다.
- `user-service`는 공용 `users` 테이블과 비밀번호 해시, 이메일 인증, 아이디 찾기, 비밀번호 변경·재설정을 소유한다.
- `users.role`은 Auth Server 호환용이며 `STUDENT`, `INSTRUCTOR`만 저장한다.
- 실제 서비스 권한은 `users.business_role`의 `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `EMPLOYEE`로 판단한다.
- `EMPLOYEE`는 `STUDENT`, `COMPANY_ADMIN`과 `PLATFORM_ADMIN`은 `INSTRUCTOR`로 매핑한다.
- 보호 API는 Access Token의 기존 Role을 비즈니스 권한으로 신뢰하지 않고 사용자 ID로 최신 상태·소속·비즈니스 역할을 조회한다.
- 내부 권한 조회는 `X-Internal-Api-Key`를 사용하는 `/internal/users/{userId}/authorization-context`로 제공한다.

## 2. 현재 작업 상태

### 저장소·브랜치

- [x] `feature/user-company` 브랜치 생성
- [x] 최신 `origin/main`의 인증 서버 유지 방침 반영
- [x] 기존 작업 복원 후 충돌 없음 확인
- [x] 변경사항 커밋
- [x] 원격 기능 브랜치 푸시
- [x] Base `dev`, Compare `feature/user-company` Pull Request 생성 및 병합

### 이번 구현 범위

- [x] `Company`, `User`, `Term`, `UserAgreement` Entity와 Repository
- [x] Auth 호환 역할과 비즈니스 역할 분리
- [x] `users.password` BCrypt 해시 저장
- [x] 기업·관리자·약관 동의 트랜잭션 처리
- [x] 일회용 이메일 인증 토큰 해시 검증·소비
- [x] 서비스 직접 호환 `POST /api/companies`
- [x] Gateway 공개 가입 `POST /api/users/register`
- [x] `GET /api/companies/me`, `POST /api/companies/me?action=update-company`
- [x] `GET /api/users/me`, `POST /api/users/me?action=update-profile`
- [x] Gateway 공개 `GET /api/users/register?action=active-terms`와 서비스 직접 호환 `GET /api/terms/active`
- [x] `POST /api/users/me/agreements`
- [x] 내부 사용자 권한 조회와 API 키 검증
- [x] 공통 성공·오류 응답 및 주요 예외 상태 코드
- [x] 이메일 인증 요청·6자리 코드 확인·SMTP 발송
- [x] Gateway를 통한 OAuth2 Authorization Code JWT 발급 후 보호 API 호출
- [x] 공개 비밀번호 재설정 요청·확인과 아이디 찾기 SMTP 발송
- [x] 보호 API 비밀번호 변경·회원 탈퇴 서버 구현 및 MockMvc 검증
- [x] 실제 Bearer Token Gateway 호출로 비밀번호 변경·회원 탈퇴 검증

## 3. 기업 대표계정 가입 처리

외부 계약:

```http
POST /api/users/register
```

`POST /api/companies`는 user-service 직접 호출 호환 경로로 유지한다. 제공 Gateway 이미지가 공개 허용하는 MVP 외부 가입 경로는 `POST /api/users/register`이며, 요청·응답 구조는 동일하다.

처리 순서:

```text
요청 형식 검사
→ 사업자번호·이메일 중복 검사
→ 현재 필수 약관 동의 검사
→ 이메일과 일치하는 인증 완료 토큰 검사
→ 인증 토큰 일회용 소비
→ Company 저장
→ BCrypt 비밀번호와 Auth 호환 역할 INSTRUCTOR 저장
→ 비즈니스 역할 COMPANY_ADMIN 저장
→ 약관 버전과 동의 시각 저장
→ 201 Created
```

위 처리는 하나의 DB 트랜잭션에서 실행한다. 기업·사용자·약관 저장이 실패하면 이메일 인증 토큰 소비도 함께 롤백되어야 한다.

검증 항목:

- [x] 사업자번호의 하이픈 제거 및 유일성 검사
- [x] 이메일 소문자 정규화 및 유일성 검사
- [x] 필수 약관 누락 시 `422 REQUIRED_AGREEMENT_MISSING`
- [x] 잘못된 이메일 인증 토큰이면 `422 INVALID_EMAIL_VERIFICATION`
- [x] 비밀번호 원문 미저장 및 BCrypt 일치 확인
- [x] 응답 Role은 실제 비즈니스 역할 `COMPANY_ADMIN`
- [x] 중복 사업자번호·이메일 `409`

## 4. 권한 처리

### 외부 보호 API

- JWT의 `userId`, `user_id`, 숫자형 `sub` 순서로 사용자 ID를 찾는다.
- DB에서 사용자의 최신 `status`, `businessRole`, `companyId`를 다시 확인한다.
- 기업 API는 `ACTIVE` 상태이며 `COMPANY_ADMIN`이고 소속 기업이 있는 사용자만 허용한다.
- 비활성 또는 탈퇴 사용자는 `403 USER_INACTIVE`로 거부한다.

### 내부 권한 조회

```http
GET /internal/users/{userId}/authorization-context
X-Internal-Api-Key: ${INTERNAL_API_KEY}
```

- [x] 올바른 키이면 `userId`, `companyId`, `businessRole`, `status` 반환
- [x] 키가 없거나 다르면 `403 INVALID_INTERNAL_API_KEY`
- [x] 이 경로는 OAuth2 Scope에 의존하지 않음
- [x] Gateway 외부 `/internal/**` 요청이 `404`로 차단되는지 통합 확인

## 5. 검증 현황

### 자동 검증

- [x] `./gradlew compileJava compileTestJava`
- [x] `./gradlew test --rerun-tasks`
- [x] Controller 통합 테스트 14개 성공
- [x] 약관 서비스 테스트 1개 성공
- [x] Spring Context 테스트 1개 성공
- [x] 총 16개, 실패·오류·건너뜀 0개
- [x] `git diff --check`
- [x] `docker compose config --quiet`

### 실제 실행 검증

- [x] 최신 코드로 `user-service` 검증 이미지 재빌드
- [x] 격리 MariaDB에서 최신 DDL 적용 확인
- [x] `/actuator/health` `200 UP`
- [x] `/api-docs` `200`, Swagger UI Redirect `302` 확인
- [x] 인증 완료 토큰 준비 후 기업가입 `201`
- [x] `users.password` BCrypt, `role=INSTRUCTOR`, `business_role=COMPANY_ADMIN` 확인
- [x] 인증 토큰 `used_at`, 기업·사용자·약관 동의 저장 확인
- [x] 잘못된 토큰 `422`, 중복 가입 `409`, 실패 요청 Rollback 확인
- [x] 내부 API 키 성공 `200`, 누락·오류 `403`
- [x] Gateway 경유 비밀번호 재설정 요청 `202`, 링크 확인 `200`, 재사용 `422` 확인
- [x] Gateway 경유 아이디 찾기 `202` 및 MailHog 안내 메일 확인
- [x] OAuth2 Authorization Code JWT 발급과 실제 Bearer Token으로 `/api/users/me`, `/api/companies/me` 호출
- [x] Gateway 경유 `POST /api/users/register` `201` 확인

수업 가이드의 `POST /api/users/login` 예시는 현재 제공 이미지와 다르며 Gateway에서 `401`을 반환한다. 실제 제공 Auth Server는 `authorization_code` grant를 지원하므로, 브라우저는 `/oauth2/authorize`와 `/login`을 거쳐 Authorization Code를 받고 user-service의 서버 측 코드 교환으로 JWT를 발급받는다. 실제 토큰으로 사용자·기업 보호 API `200`, 탈퇴 사용자 `403 USER_INACTIVE`를 확인했다.

## 6. `dev` 병합 후 운영 주의사항

### 공용 초기 DDL과 기존 볼륨

- 강의 도메인은 현재 `language`, `situation`, `level`, `status` 계약으로 통합됐고 Spring 서비스 테스트가 통과합니다.
- 새 볼륨은 `init-db/01_init.sql`로 전체 현재 스키마와 seed 데이터를 생성합니다.
- 기존 볼륨은 `db-migration`이 `init-db/migrations/001_users_auth_compat.sql`과 이후 마이그레이션을 순서대로 적용합니다.
- `lecture-db-migration`의 정상 상태는 계속 실행 중인 `Up`이 아니라 작업 완료 `Exited (0)`입니다.
- `docker compose down -v`는 DB와 Kafka 데이터를 삭제하므로 사용자의 명시적 요청 없이 실행하지 않습니다.

### 기존 Auth Server 제한

- 제공 Gateway/Auth 이미지의 JSON 로그인 경로는 사용하지 않는다.
- 등록된 브라우저 클라이언트 `web-client`의 Authorization Code 흐름을 사용한다. 배포된 로컬 Auth Server 이미지는 Compose 기본값으로 연결하고, 다른 환경의 클라이언트 비밀값은 `AUTH_WEB_CLIENT_SECRET`으로 user-service에만 주입한다.

## 7. 구현 단계 완료 기록

### PR 1 — 기업 계정 기반 마무리

1. 이메일 인증 요청·확인 API와 MailHog SMTP 연동: 완료. 제공 Gateway의 공개 경로 제약에 맞춰 `POST /api/users/register?action=request-email-verification`, `POST /api/users/register?action=confirm-email-verification`을 사용한다.
2. 배포된 Auth Server 이미지 기준으로 별도 비밀값 설정 없이 브라우저 OAuth 로그인 확인 완료. 다른 환경에서는 `AUTH_WEB_CLIENT_SECRET`을 주입
3. 관련 API·ERD·MVP 체크리스트 갱신 완료

### PR 2 — 초대코드·직원 가입

- [x] `Invitation` Entity와 SHA-256 해시 코드 생성
- [x] 생성·목록·폐기·재발급 API
- [x] 만료·일회성·동시 사용 방지
- [x] 이메일 인증·필수 약관을 포함한 직원 가입 API
- [x] Auth 호환 `STUDENT`, 비즈니스 역할 `EMPLOYEE` 저장

초대코드 원문은 생성·재발급 응답에서만 한 번 반환한다. 목록은 `code=null`, `codeMasked`와 상태·시각만 반환하며, 원문 해시는 `invitations.code_hash`에만 저장한다. 직원 가입 화면은 이메일 인증·필수 약관 동의 후 Gateway 공개 경로인 `POST /api/users/register?action=employee-signup`을 호출한다.

### PR 3 — 구독 권한·좌석

- [x] `CompanyEntitlement` 테이블과 `/internal/companies/{companyId}/entitlement` 조회 API
- [x] `ProcessedEvent`
- [x] 결제·구독 Kafka 이벤트 멱등 소비와 `CompanyEntitlement` 자동 갱신
- [x] 활성 직원 기준 좌석 사용량 계산
- [x] 직원 가입 시 구독 활성·잔여 좌석 검사
- [x] 내부 구독 권한 조회 API

### PR 4 — 회원·운영 기능

- [x] 직원 목록과 상태 변경
- [x] 구매·사용·잔여 좌석 조회
- [x] 비활성화·소속 해제 시 좌석 회수, 재활성화 시 좌석 재검증
- [x] 비밀번호 재설정 링크 발송·단회 확인
- [x] 아이디 찾기 등록 이메일 안내
- [x] 회원 탈퇴와 로그인 불가 비밀번호 교체 서버 구현
- [x] 실제 Bearer Token Gateway 통합 검증
- [x] 플랫폼 관리자용 기업·사용자 상태 조회

## 8. 커밋·PR 전 체크

```bash
git status
git diff --check
git diff
./gradlew test --rerun-tasks
docker compose config --quiet
```

- 작업 범위의 파일만 명시적으로 스테이징한다.
- 기능 구현·테스트·실제 실행·관련 문서가 모두 끝난 MVP 항목만 `[x]`로 변경한다.
- 커밋과 푸시는 사용자 확인 후 진행한다.
