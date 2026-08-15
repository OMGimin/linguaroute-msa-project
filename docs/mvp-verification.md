# MVP 통합 검증 기록

> 검증일: 2026-08-11 · 대상: 로컬 Docker Compose, API Gateway(`http://localhost:8080`), MailHog, Vue 브라우저 화면

이 문서는 팀 공유용 실행 검증 기록이다. 체크리스트의 `[x]`는 구현·테스트·실행 확인이 끝난 항목만 의미하며, API 연결 여부와 실제 실행 검증 여부를 구분해 표시한다.

## 실행 결과

| 영역 | 확인 내용 | 결과 |
| --- | --- | --- |
| OAuth2 로그인 | Auth Server Authorization Code를 Gateway에서 교환해 관리자·직원·플랫폼 관리자 JWT 발급 | 통과 |
| 이메일 인증 | 인증 요청 `202`, MailHog 수신, 코드 확인 `200`, 같은 코드 재사용 `422` | 통과 |
| 기업·사용자 | 기업/내 정보, 약관, 직원·좌석·초대 목록의 역할별 Gateway 조회 | 통과 |
| 초대코드 | 생성 `201`, 재발급 `201`, 폐기 `204`, 유효 기간 오류 `400` | 통과 |
| 구독·결제 조회 | 요금제, 현재 구독, 결제 이력 | 통과 |
| 결제 이벤트 | `PaymentCompleted`, `PaymentFailed`, `SubscriptionCanceled`, `SubscriptionExpired`, `SubscriptionRenewed` Outbox 생성 및 entitlement 소비 단위 테스트 | 통과 |
| 강의 | 목록/검색/필터/상세/차시 조회와 관리자 차시 등록 | 통과 |
| 수강·학습 | 수강신청 `201`, 중복 `409`, 차시 시작 `200`, 완료·진도율·수료 `200` | 통과 |
| 수강 권한 | 직원의 기업 API `403`, 플랫폼 관리자의 내 수강 목록 `403` | 통과 |
| 기업 수강 관리 | 직원별 수강 상태·진도율 조회 | 통과 |
| 플랫폼 운영(user-service) | 플랫폼 관리자의 사용자·기업 검색·상태 조회, 일반 역할·비활성 관리자 차단 | 통과 |
| 전체 API curl 회귀 | 공개·보호·내부 API, OAuth2, 이메일, Kafka 이벤트를 포함한 79개 요청 | 79/79 통과 |
| 브라우저 | Chrome 역할별 38개 상황과 정리 후 관리자 강의·기업 진도 화면 2개 추가 확인 | 기존 38/38 및 추가 2개 통과 |
| AI 추천 서비스 | 로컬 Provider로 사용자 권한·수강 이력·활성 강의 조회, 결과 재검증과 추천 저장 | 통과 |
| AI 추천 화면 | 실제 API 요청 변환·응답 매핑 단위 테스트 3개 및 프로덕션 빌드 | 통과 |

### 문서 동기화 전 재검증

2026-08-11 현재 `dev`에서 Spring Boot 5개 서비스의 `./gradlew test`, 추천 서비스 테스트 16개, 프론트 추천 테스트 3개와 `npm run build`를 다시 실행해 모두 통과했습니다. 새 Docker 볼륨으로 재기동한 뒤 실제 직원 OAuth 토큰으로 Gateway 추천 API를 호출해 다음을 확인했습니다.

- 외부에서 보낸 `X-User-Id: 9101`이 토큰 사용자 `9103`으로 덮어써짐
- 기존 수강 중인 일본어 강의가 추천 후보에서 제외됨
- 영어 추천은 `200`, 최대 3개 `ACTIVE` 강의와 이유 반환
- `recommendations`에 사용자·기업·요청·출처·상태 저장
- `recommendation_items`에 강의 ID·순위·추천 이유 저장

## 실제 Gateway 검증 경로

```text
GET  /api/users/register?action=active-terms
POST /api/users/register?action=employee-signup
GET  /api/companies/me
POST /api/companies/me?action=update-company
GET  /api/users/me
POST /api/users/me?action=update-profile
GET  /api/companies/me/invitations
GET  /api/companies/me/employees
POST /api/companies/me/employees/{userId}/status?action=update-status
GET  /api/companies/me/seats
GET  /api/plans
GET  /api/subscriptions/me
GET  /api/payments
GET  /api/courses?keyword=...&language=...&situation=...&level=...
GET  /api/courses/{courseId}
GET  /api/courses/{courseId}/lessons
GET  /api/admin/courses?status=...
POST /api/admin/courses/{courseId}?action=update-course
POST /api/admin/courses/{courseId}/status?action=update-status
GET  /api/enrollments/me
GET  /api/companies/me/enrollments
GET  /api/companies/me/enrollments/progress
GET  /api/admin/users?keyword=...&businessRole=...&status=...&page=...&size=...
GET  /api/admin/companies?keyword=...&status=...&page=...&size=...
```

## 이번 검증에서 수정한 사항

- 강의 차시·수강·진도율 API와 Gateway 라우팅을 구현하고, 모든 수강 관련 상태를 서버가 계산하도록 정리했다.
- 수강 목록·상세도 최신 `EMPLOYEE` 권한을 내부 조회해 차단하도록 보완했다.
- 기업 가입 화면이 목업 토큰을 사용하던 문제를 수정해 실제 이메일 인증 API를 호출하고 검증 토큰을 사용하도록 변경했다.
- 강의 목록, 상세, 내 학습, 학습 플레이어, 기업 진도 화면에 라이브 API 연결을 추가했다.
- 구독 결제 화면은 요금제 조회 후 선택한 `planPriceId`와 `Idempotency-Key`로 `POST /api/subscriptions`를 호출하도록 연결했고, 강의 등록 화면은 강의 생성 뒤 각 차시 등록과 상태 변경 API를 순서대로 호출하도록 연결했다.
- API 문서의 수강·학습 응답과 README 통신 흐름을 구현과 맞췄다.
- 제공 Gateway의 공개 허용 경로에 맞춰 약관 조회와 직원 가입을 `/api/users/register` action 별칭으로 연결했다.
- 데모 이메일 도메인을 `skala-tech.local`로 통일하고 기존 볼륨에도 적용되는 순차 마이그레이션과 `lecture_db` 재생성 스크립트를 추가했다.
- 제공 Gateway의 브라우저 CORS 정책이 `PATCH` 사전 요청을 거부하므로, 프로필·기업·직원 상태·강의 수정 API에 동일 서비스 로직을 사용하는 `POST action` 별칭을 추가했다. 기존 `PATCH`는 직접 호출 호환 경로로 유지했다.
- 기업 가입 완료 후 인증 없이 결제 화면으로 이동하던 흐름을 로그인 후 원래 결제 화면으로 복귀하도록 수정했고, 기업 설정·결제 화면의 관리자 정보는 로그인 사용자 API 응답을 표시하도록 변경했다.
- 관리자 전용 강의 목록에 상태 필터를 추가해 비활성 강의를 새로고침 후에도 조회·재활성화할 수 있도록 했고, 기업 진도 화면은 직원·진도 API의 실제 값을 결합하도록 정리했다.
- 참조되지 않는 구형 프론트 컴포넌트·store·전용 이미지와 과거 목업·이전 Swagger 문서 이미지를 제거했다.

## 팀 공유 전 인지할 제한

- 프론트엔드는 별도 모드 환경 변수 없이 항상 실제 Auth Server와 API를 호출한다. 백엔드가 준비되지 않으면 목업 데이터로 대체하지 않고 오류 또는 빈 상태를 표시한다.
- 기업 대시보드, 구독·결제, 강의 목록·상세·관리·등록, 내 학습·플레이어·수강, 기업 진도·직원 관리와 플랫폼 운영 화면은 현재 API를 호출한다. 응답이 비어 있으면 목업으로 대체하지 않고 빈 상태를 표시한다.
- 이번 프론트 연결 변경은 `npm run build`로 정적 검증했다. 2026-08-11 라이브 재검증에서 Gateway의 서비스 토큰 발급과 `GET /api/plans`는 `200`을 반환했다. 배포된 분할 인프라 파일의 `msa-lecture/auth-server:1.0`을 사용하는 Compose 기본 구성에서는 별도 `AUTH_WEB_CLIENT_SECRET` 설정 없이 브라우저 Authorization Code 교환 API도 `200`을 반환한다. 다른 Auth Server를 사용하는 환경에서는 해당 등록값을 환경변수로 덮어써야 한다.
- 플랫폼 운영 화면은 사용자·기업·결제·수강 운영 API를 직접 호출하며, 플랫폼 관리자 `200`과 일반 사용자 `403` 권한 분기를 통합 실행으로 확인했다.
- 2026-08-11 전체 curl 회귀 검사에서 추천 API를 포함한 79개 요청이 모두 통과했다. 제공 Gateway가 고정적으로 차단하는 `GET /api/terms/active` 대신 외부 계약인 `GET /api/users/register?action=active-terms`를 검증했다.
- Chrome 재검증에서 기존 실패 5건(프로필, 기업명, 직원 상태, 강의 수정, 강의 상태)을 모두 통과했고, 테스트 중 변경한 값은 원래 값으로 복원 후 재조회했다.
- AI 추천 화면은 별도 데모 모드 없이 `POST /api/courses/recommendations`를 호출하며, 로딩·빈 결과·오류와 규칙 기반 fallback 상태를 구분해 표시한다.
- AI 추천 백엔드는 로컬 Provider를 사용한 직접 통합 호출에서 `200` 응답과 추천·추천 항목 DB 저장을 확인했다. Gateway는 클라이언트의 `X-User-Id`를 토큰 subject로 덮어쓰는 것도 확인했다.
- 실제 직원 OAuth 로그인부터 Gateway, `OpenAiRecommendationProvider`, 추천 결과 저장까지 종단 간 시연을 완료했다. API 키와 전체 프롬프트는 검증 기록에 남기지 않는다.
- 현재 검증 DB에는 API 검증용 강의·차시·수강 및 초대코드 데이터가 추가되어 있다. 제출 전에는 `./scripts/reset-local-database.sh`로 `lecture_db`를 현재 seed 기준으로 초기화할 수 있다.

## 재현 명령

```bash
cd user-service && ./gradlew test
cd ../payment-service && ./gradlew test
cd ../course-service && ./gradlew test
cd ../enrollment-service && ./gradlew test
cd ../vue-frontend && npm run build
cd .. && docker compose config --quiet
```
