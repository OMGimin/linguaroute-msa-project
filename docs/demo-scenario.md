# LinguaRoute 발표 시연 가이드

## 1. 시연 목표

`기업 구독 → 직원 강의 탐색·AI 추천·학습 → 기업 진도 확인 → 플랫폼 운영`이 실제 Auth Server와 API Gateway를 통해 연결되는 것을 보여줍니다.

## 2. 시연 전 준비

프로젝트 루트에서 다음 상태를 확인합니다.

```bash
docker compose ps
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost:8085/health
cd vue-frontend && npm run dev
```

- 프론트엔드: <http://localhost:3000>
- API Gateway: <http://localhost:8080>
- Auth Server: <http://localhost:9000>
- 공통 비밀번호와 데모 이메일: [README의 데모 계정](../README.md#8-데모-계정과-seed-데이터)을 사용합니다.
- 브라우저 확대/축소는 100%, 창은 1280×900 이상으로 맞춥니다.
- 시연 직전에는 테스트용 생성·수정 API를 실행하지 않습니다.

## 3. 발표 시연 순서

### 0단계. 서비스 소개 - 약 20초

1. `http://localhost:3000`을 엽니다.
2. 랜딩 화면에서 세 이해관계자 관점을 짚습니다.
   - 직원: 강의 탐색과 학습
   - 기업 관리자: 구독·좌석·진도 관리
   - 플랫폼 관리자: 기업·사용자·강의·거래 운영

확인 문장: “LinguaRoute는 기업이 구독하고 직원이 학습하며 운영자가 전체 상태를 관리하는 B2B 외국어교육 플랫폼입니다.”

### 1단계. 기업 관리자 - 약 1분 20초

1. `admin@skala-tech.local`로 로그인합니다.
2. 기업 대시보드에서 활성 직원, 잔여 좌석, 수강 중 강의, 평균 진도율을 확인합니다.
3. `요금제 · 결제`에서 활성 구독과 결제 내역을 확인합니다.
4. `직원 · 초대`에서 초대코드와 좌석 사용량을 확인합니다.
5. `학습 현황`에서 `employee.lee@skala-tech.local`의 진도율을 확인합니다.

확인 API:

- `GET /api/subscriptions/me`
- `GET /api/payments`
- `GET /api/companies/me/seats`
- `GET /api/companies/me/employees`
- `GET /api/companies/me/enrollments/progress`

확인 문장: “기업 관리자는 결제 결과로 확보한 좌석과 직원별 학습 결과를 같은 흐름에서 확인합니다.”

### 2단계. 직원 - 약 2분

1. 로그아웃 후 `employee.lee@skala-tech.local`로 로그인합니다.
2. `강의 찾기`에서 언어를 `일본어`로 선택해 실제 `ACTIVE` 강의만 조회되는 것을 확인합니다.
3. `AI 추천`으로 이동해 영어·고급·글로벌 세일즈·업무 발표 조건과 `제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 질문에 자신 있게 답하고 싶어요.`라는 목표를 입력합니다.
4. 추천 결과의 강의명·추천 이유와 `AI 추천` 또는 `규칙 기반 추천` 출처 안내를 확인하고 강의 상세 이동 버튼을 설명합니다.
5. `내 학습`으로 이동합니다.
6. `비즈니스 이메일 영어`가 `COMPLETED · 100%`, `해외 고객 미팅 영어`가 `LEARNING · 33.33%`로 표시되는지 확인합니다.
7. `이어서 학습하기` 버튼이 다음 학습 행동으로 연결되는 것을 설명합니다. 발표 중에는 진도 데이터를 변경하지 않습니다.

확인 API:

- `GET /api/courses?language=JAPANESE`
- `POST /api/courses/recommendations`
- `GET /api/enrollments/me`
- `GET /api/courses/{courseId}/lessons`

확인 문장: “직원은 자신의 조건으로 실제 활성 강의를 추천받고, 서버가 계산한 수강 상태와 진도율로 학습을 이어갑니다.”

추천 요청은 `recommendations`, `recommendation_items`에 이력을 저장합니다. 시연 중에는 준비한 조건으로 한 번만 요청하고 반복 호출하지 않습니다.

### 3단계. 플랫폼 관리자 - 약 1분

1. 로그아웃 후 `platform-admin@linguaroute.local`로 로그인합니다.
2. 운영 대시보드에서 기업·사용자·결제·수강 집계를 확인합니다.
3. `강의 관리`에서 상태 필터를 `INACTIVE`로 변경합니다.
4. 비활성 강의도 조회되고 `활성화` 작업이 제공되는 것을 확인합니다. 발표 중에는 상태를 실제 변경하지 않습니다.

확인 API:

- `GET /api/admin/companies`
- `GET /api/admin/users`
- `GET /api/admin/payments`
- `GET /api/admin/enrollments`
- `GET /api/admin/courses?status=INACTIVE`

확인 문장: “플랫폼 관리자는 서비스별 데이터를 API Gateway 뒤에서 통합 조회하고 콘텐츠 노출 상태를 운영합니다.”

### 4단계. 마무리 - 약 20초

다음 문장으로 시연을 마칩니다.

> “결제와 좌석, AI 추천과 직원 학습, 기업 진도, 플랫폼 운영이 하나의 경로로 이어졌습니다. AI가 후보를 선택하고 서버가 실제 활성 강의만 다시 검증합니다.”

## 4. 실패 시 복구 순서

1. 화면이 비어 있으면 새로고침하고 현재 URL과 로그인 역할을 확인합니다.
2. `401`이면 로그아웃 후 해당 역할 계정으로 다시 로그인합니다.
3. `403`이면 로그인 계정의 비즈니스 역할이 화면과 일치하는지 확인합니다.
4. API 오류면 `docker compose ps`와 해당 서비스 로그를 확인합니다.
5. 추천 화면 오류면 새로고침 후 한 번만 다시 시도하고, 계속 실패하면 규칙 기반 fallback 또는 준비한 실제 응답 스냅샷으로 설명합니다.
6. 데이터가 예상과 다르면 발표 중 초기화하지 말고 준비한 발표자료의 실제 화면 스냅샷으로 설명을 이어갑니다.

## 5. Chrome 자동 시연 원칙

`$linguaroute-demo` 스킬을 사용하면 이 문서를 기준으로 Chrome을 조작합니다.

- 서비스 상태와 로그인 역할을 단계마다 확인합니다.
- 결제 생성, 초대 생성, 강의 상태 변경, 수강 상태 변경처럼 데이터가 바뀌는 버튼은 사용자가 명시적으로 요청한 경우에만 누릅니다.
- 추천 요청도 이력을 저장하므로 시연 시나리오에서 준비한 한 번만 실행합니다.
- 기본 자동 시연은 조회와 화면 이동만 수행합니다.
- 추천 결과는 실제 `ACTIVE` 강의, 요청 언어, 기존 수강 제외 조건과 출처 안내를 함께 확인합니다.
