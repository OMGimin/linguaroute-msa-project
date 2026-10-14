# AI 추천 서비스 운영·재검증 가이드

## 1. 문서 목적

이 문서는 `feature/ai-recommendation` 구현이 `dev`에 병합된 뒤 다른 PC나 새 개발 환경에서
현재 기능을 재현·운영 검증하기 위한 절차입니다. 현재 구현·검증 결과의 원본은
[MVP 통합 검증 기록](./mvp-verification.md)이며, 이 문서의 명령과 보완 항목은 재검증용입니다.
개인과제용 판단 기록은 현재 PC 바탕화면의 다음 파일에만 있으며
Git 저장소에는 포함되지 않습니다.

```text
C:\Users\MNW\Desktop\AI추천_Gateway_의사결정_기록.md
```

다른 PC에서도 이 기록이 필요하면 위 파일을 개인 저장공간을 통해 별도로 복사합니다. 이 파일이
없어도 개발 재개에는 영향이 없으며, 팀 저장소나 공개 공유 공간에는 업로드하지 않습니다.

담당 범위:

- `recommend-service`의 OpenAI 추천 연동과 장애 fallback 검증
- API Gateway의 추천 경로, 인증 헤더와 내부 API 차단 검증
- user/course/recommend-service 사이의 실제 통합 테스트
- 추천 결과의 MariaDB 저장 확인

확정된 API 계약은 [API 명세서](./api-spec.md), 전체 요구사항은
[서비스 기획서](./product-spec.md)와 [MVP 체크리스트](./mvp-checklist.md)를 우선합니다.

---

## 2. 새 환경에서 저장소와 실행 환경 준비

### 2.1 저장소 받기

```powershell
git clone https://github.com/skala-msa-team/linguaroute-msa.git
cd linguaroute-msa
git fetch origin
git checkout dev
git pull origin dev
git status -sb
git log --oneline --decorate -10
```

작업 트리에 본인이 만들지 않은 변경이 있으면 먼저 소유자를 확인합니다. 최신 `dev`는 바로
병합하지 말고 `git fetch origin` 후 변경 파일과 API 계약을 먼저 비교합니다.

### 2.2 Python 환경 만들기

프로젝트 Docker 이미지는 Python 3.12를 사용하므로 새 환경에서도 Python 3.12를 권장합니다.
가상환경은 Git에 포함되지 않으므로 PC마다 새로 생성합니다.

```powershell
cd recommend-service
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m compileall -q app tests main.py
```

macOS 또는 Linux에서는 활성화 명령과 Python 경로만 다음처럼 바꿉니다.

```bash
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pytest -q
python -m compileall -q app tests main.py
```

`.venv`, `.env`, API 키와 Access Token은 Git에 추가하지 않습니다.

### 2.3 현재 완료 상태와 재검증 대상

2026-08-11 현재 추천 구현은 `dev`에 병합됐습니다. 추천 서비스 테스트 16개, Spring Boot 5개
서비스 테스트, 프론트 추천 테스트 3개와 Vue 프로덕션 빌드가 통과했습니다. 실제 직원 OAuth
토큰을 사용한 Gateway 요청, 외부 `X-User-Id` 덮어쓰기, 내부 API 호출, 정상·fallback 응답과
MariaDB 추천·추천 항목 저장도 확인했습니다.

새 환경에서 다시 확인할 항목은 다음과 같습니다.

1. `OPENAI_API_KEY`가 없는 기본 환경의 로컬 Provider와 저장
2. 운영 키를 사용하는 환경의 OpenAI Provider와 구조화 출력
3. Gateway의 외부 `/internal/**` 차단과 인증 사용자 헤더 덮어쓰기
4. 직접 서비스 포트 차단, 요청 제한과 비밀값 주입 등 운영 보완사항

---

## 3. 우선순위 1: 팀용 OpenAI API 키 준비

### 3.1 권장 발급 방식

팀 프로젝트에서는 개인 키보다 OpenAI Platform 프로젝트에 속한 서비스 계정 키를 사용합니다.
프로젝트 또는 조직 Owner가 다음 절차를 진행해야 합니다.

1. [OpenAI API Platform](https://platform.openai.com/)에 로그인합니다.
2. LinguaRoute에서 사용할 프로젝트를 선택하거나 새 프로젝트를 생성합니다.
3. `Settings → Project → Members`로 이동합니다.
4. `+ Service account`를 선택합니다.
5. 이름을 `linguaroute-recommend-service`처럼 용도가 드러나게 지정합니다.
6. 생성 직후 한 번만 표시되는 Secret Key를 팀의 비밀 관리 수단에 저장합니다.

서비스 계정 생성 권한이 없다면 프로젝트 Owner에게 생성을 요청합니다.

- [OpenAI 프로젝트와 서비스 계정 관리](https://help.openai.com/en/articles/9186755-managing-projects-in-the-api-platform)
- [API 키 생성 위치](https://help.openai.com/en/articles/4936850-where-do-i-find-my-openai-api-key)

### 3.2 프로젝트 설정 확인

키를 받기 전에 프로젝트 Owner와 다음 항목을 확인합니다.

- API 결제수단 또는 크레딧이 준비돼 있는가?
- 프로젝트의 월 사용 한도와 알림 기준이 설정돼 있는가?
- 프로젝트에서 `gpt-5.6-luna` 사용이 허용돼 있는가?
- 키 권한을 필요한 추론 요청 범위로 제한할 수 있는가?

ChatGPT 구독과 OpenAI API 결제는 별도입니다. API 키가 있어도 결제나 프로젝트 모델 권한이
없으면 인증·사용 한도 오류가 발생할 수 있습니다.

### 3.3 안전한 주입 방법

API 키를 채팅, 코드, 문서, `docker-compose.yml` 또는 Git 커밋에 입력하지 않습니다.
현재 PowerShell 세션에서만 테스트하려면 다음처럼 환경변수로 설정합니다.

```powershell
$env:OPENAI_API_KEY="발급받은 실제 키"
```

키 값을 출력하지 않고 설정 여부만 확인합니다.

```powershell
if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
    "OPENAI_API_KEY 설정 안 됨"
} else {
    "OPENAI_API_KEY 설정됨"
}
```

Docker Compose는 실행한 PowerShell 세션의 환경변수를 읽습니다. 키를 설정한 동일한 창에서
Compose를 실행해야 합니다. 키가 유출되면 즉시 폐기하고 새 키를 발급합니다.

- [OpenAI API Quickstart](https://platform.openai.com/docs/quickstart/make-your-first-api-request)
- [API 키 권한 설정](https://help.openai.com/en/articles/8867743)

---

## 4. 우선순위 2: Luna 실제 호출 1회 검증

먼저 전체 MSA와 분리된 최소 스모크 테스트로 OpenAI 연결만 확인합니다. 실제 호출은 비용이
발생하므로 정상 케이스 한 번으로 제한하고 응답 전문이나 API 키를 로그에 남기지 않습니다.

검증 입력:

- 요청 언어: `ENGLISH`
- 수준: `INTERMEDIATE`
- 상황: `CUSTOMER_MEETING`
- 후보: 고정된 `ACTIVE` 영어 강의 1~3개
- 모델: `gpt-5.6-luna`

확인할 결과:

- HTTP 인증과 모델 접근이 성공하는가?
- `OpenAiRecommendationResult` 구조로 파싱되는가?
- 추천 수가 최대 3개인가?
- 반환된 `courseId`가 전달한 후보 안에 있는가?
- 추천 이유가 비어 있지 않은가?

정상 호출 증거로 다음 정보만 기록합니다.

```text
실행 일시
사용 모델
성공 여부
반환된 courseId
구조화 파싱 성공 여부
민감정보를 제거한 오류 코드 또는 요약
```

실제 키나 전체 프롬프트는 증거 자료에 포함하지 않습니다.

---

## 5. 우선순위 3: 신규 내부 API 공통 계약 통합 검증

팀 합의에 따라 신규 내부 API는 `/api/{service}/internal/**`가 아니라
`/internal/{service}/**`로 분리합니다. 공통 규칙은 다른 담당자의 브랜치에서
`AGENTS.md`와 `docs/api-spec.md` 및 실제 제공 서비스 코드에 반영됐고 최신 `dev` 병합도
완료했습니다. 다음 환경에서는 코드 변경보다 실제 통합 동작을 확인합니다.

recommend-service에 영향을 주는 경로 변경:

| 대상 | 폐기한 레거시 경로 | 현재 계약 |
|---|---|---|
| Course 추천 후보 | `/api/courses/internal/recommend` | `/internal/courses/recommend` |
| Enrollment 이력 | `/api/enrollments/internal/history/{userId}` | `/internal/enrollments/history/{userId}` |

두 호출 모두 다음 헤더를 전달해야 합니다.

```http
X-Internal-Api-Key: {internalApiKey}
```

### 5.1 Course 담당자 구현 확인

현재 계약:

```http
GET /internal/courses/recommend?language=ENGLISH&excludeIds=3&excludeIds=5
X-Internal-Api-Key: {internalApiKey}
```

필수 동작:

- 키 누락 → `401 Unauthorized` (`INVALID_INTERNAL_API_KEY`)
- 키 불일치 → `401 Unauthorized` (`INVALID_INTERNAL_API_KEY`)
- 정상 키 → `200 OK`
- 응답은 강의 배열
- 요청 언어와 일치하는 `ACTIVE` 강의만 반환
- 각 항목에 `id`, `title`, `language`, `level`, `situation`, `status` 포함

recommend-service는 이 API를 Gateway를 거치지 않고 course-service에 직접 호출합니다.
제공자와 소비자 변경은 최신 `dev` 병합으로 함께 반영됐습니다. 자동 테스트 통과와 별개로
실제 실행 환경에서 정상 키와 누락·불일치 키를 각각 호출해 응답을 확인합니다.

### 5.2 Enrollment 이력 호출 정리

recommend-service 쪽 계약 반영은 완료했습니다.

1. `/internal/enrollments/history/{userId}`를 직접 호출합니다.
2. `X-Internal-Api-Key` 헤더를 전달합니다.
3. 응답의 `activeCourseIds`를 Course API의 `excludeIds`로 전달합니다.

`activeCourseIds`는 호환성을 위해 유지된 필드명이며 실제 의미는 `ENROLLED`,
`LEARNING`, `COMPLETED` 상태의 강의 ID 전체입니다. 변수명만 보고 현재 수강 중인
강의로 한정하지 말고, 이미 수강 이력이 있는 강의를 모두 제외해야 합니다.
다음 환경에서는 제공 서비스가 같은 계약으로 실행되는지만 통합 테스트합니다.

### 5.3 Payment 변경 영향

구독·결제 담당자 기준으로 현재 payment-service에는 실행 중인 내부 API가 없습니다.
기존 `/api/payments/internal/request`는 제거됐고 새 API는 `/api/plans`,
`/api/subscriptions`, `/api/payments` 외부 보호 API를 사용합니다. 따라서 recommend-service의
직접 수정 대상은 아닙니다.

최신 `dev`에서는 enrollment-service의 기존 `PaymentServiceClient`도 제거됐습니다.
앞으로 payment-service에 내부 API가 필요하면
`/internal/payments/**`와 `X-Internal-Api-Key` 계약을 적용합니다.

### 5.4 이후 dev 재병합 전 확인

현재 기준 최신 `dev` 병합은 완료됐습니다. 이후 추가 변경을 다시 병합할 때 다음을 먼저
확인합니다.

```powershell
git fetch origin
git log --oneline HEAD..origin/dev
git diff --name-status HEAD...origin/dev
git diff HEAD...origin/dev -- course-service recommend-service docker-compose.yml
```

특히 다음 충돌을 확인합니다.

- 추천 내부 API 경로 또는 query parameter 변경
- Enrollment 이력 API의 유지·삭제 여부
- `id`와 `courseId` 필드 변경
- 응답 배열과 `data.content` 페이지 구조 혼용
- Course enum 변경
- `INTERNAL_API_KEY` 환경변수 이름 변경

---

## 6. 우선순위 4: Gateway 통합 검증

Gateway 외부 계약:

```text
POST /api/courses/recommendations
→ recommend-service

/api/courses/internal/**
/api/enrollments/internal/**
/api/payments/internal/**
/internal/**
→ 404 Not Found
```

`/api/**/internal/**`는 기존 경로 마이그레이션 기간의 레거시 차단이고, `/internal/**`는
신규 공통 계약의 외부 차단입니다. 신규 경로는 현재 공개 `/api/**` 라우트와 일치하지 않지만,
향후 catch-all 또는 Discovery Locator가 추가돼도 노출되지 않도록 명시적으로 차단합니다.

검증 시나리오:

| 요청 | 기대 결과 |
|---|---|
| 추천 경로 + 정상 직원 토큰 | recommend-service로 전달 |
| 추천 경로 + 토큰 없음/오류 | `401 Unauthorized` |
| `COMPANY_ADMIN` 또는 비활성 직원 | `403 Forbidden` |
| 일반 `/api/courses/**` | course-service로 전달 |
| 외부 `/api/courses/internal/**` | `404 Not Found` |
| 외부 `/api/enrollments/internal/**` | `404 Not Found` |
| 외부 `/api/payments/internal/**` | `404 Not Found` |
| 외부 `/internal/**` | `404 Not Found` |

추가로 외부 클라이언트가 임의의 `X-User-Id`, `X-User-Role`을 보냈을 때 Gateway가 이를
제거하고 검증된 토큰 값으로 덮어쓰는지 확인합니다. 이 동작이 확인되지 않으면 추천 API의
사용자 식별이 안전하다고 판단하지 않습니다.

현재 Gateway는 제공된 이미지로 실행되므로 정적 Compose 테스트만으로 완료 처리하지 않습니다.
실제 컨테이너 요청과 Gateway 로그를 증거로 남깁니다.

---

## 7. 우선순위 5: 전체 MSA 추천 흐름 검증

필요 서비스:

- MariaDB
- Eureka Server
- Auth Server
- API Gateway
- user-service
- course-service
- recommend-service

실행 전 구성 확인:

```powershell
# 저장소 루트에서 실행
docker compose config --quiet
docker compose config --services
docker compose up -d
docker compose ps
```

전체 흐름:

```text
직원 로그인
→ Gateway 토큰 검증 및 사용자 ID 전달
→ recommend-service
→ user-service 최신 businessRole/status/companyId 조회
→ course-service ACTIVE 후보 조회
→ gpt-5.6-luna 구조화 추천
→ 후보 ID·중복·언어·상태 재검증
→ recommendations/recommendation_items 저장
→ Gateway 응답
```

반드시 확인할 시나리오:

| 시나리오 | 기대 결과 |
|---|---|
| `EMPLOYEE + ACTIVE` | 추천 성공 |
| `COMPANY_ADMIN` | `403 Forbidden` |
| `EMPLOYEE + INACTIVE` | `403 Forbidden` |
| 잘못된 language/level/situation | `422 Unprocessable Entity` |
| OpenAI 정상 | `source=AI` |
| OpenAI 인증 오류·timeout | `source=RULE_BASED_FALLBACK` |
| 후보에 없는 courseId | 결과에서 제거 |
| 중복 courseId | 하나만 유지 |
| 다른 언어 또는 `INACTIVE` 강의 | 결과에서 제거 |
| 정상·fallback 결과 | MariaDB에 요청과 항목 저장 |

OpenAI 장애 검증은 정상 호출 이후 실제 키를 폐기하거나 과도한 호출을 만들지 않습니다. 테스트용
잘못된 키 또는 매우 짧은 timeout을 별도 실행 환경에 주입하고, 검증이 끝나면 원래 설정으로
복원합니다.

---

## 8. 개인과제용 증거 수집

통합 테스트 중 다음 자료를 민감정보를 제거한 상태로 저장합니다.

- Luna 정상 추천 응답과 `source=AI`
- OpenAI 오류 시 fallback 응답
- `EMPLOYEE`, `COMPANY_ADMIN`, `INACTIVE` 권한별 응답
- Gateway 내부 경로 `404` 응답
- 정상·fallback 추천의 DB 저장 결과
- user/course/recommend/Gateway의 관련 로그
- 테스트 실행 결과
- 문제 발생 시 수정 전후 코드와 원인 분석

API 키, Access Token, Authorization 헤더, 전체 개인정보는 캡처 전에 가립니다. 구현 완료와 실제
통합 검증 완료를 구분하고, 실행하지 못한 항목은 보고서에 미검증으로 기록합니다.

---

## 9. MVP 완료 상태

다음 AI 추천 MVP 기준은 2026-08-11 구현·통합 검증 기록으로 완료 처리했습니다.

- [x] OpenAI Provider 구조화 출력과 서버 재검증
- [x] `/internal/courses/recommend` 내부 키 성공·실패 검증
- [x] Enrollment `activeCourseIds`를 Course `excludeIds`로 전달
- [x] Gateway 외부 내부 경로 차단
- [x] Gateway 인증 사용자 헤더 덮어쓰기
- [x] 권한·입력별 `200/401/403/422` 확인
- [x] 정상·fallback 응답과 DB 저장
- [x] 전체 자동 테스트와 프론트 빌드
- [x] [MVP 체크리스트](./mvp-checklist.md) 갱신

OpenAI 유료 호출의 재현은 실행 환경마다 유효한 프로젝트 키와 모델 권한이 필요합니다. 키가 없는
환경은 로컬 Provider로 동작하며, 이를 OpenAI 네트워크 호출 성공으로 표현하지 않습니다.

검증 후 커밋은 실행 환경, 통합 테스트, 문서 갱신을 논리적으로 분리합니다. 실제 키와 토큰이
staging 영역에 포함되지 않았는지 `git diff --cached`로 반드시 확인한 뒤 push합니다.

---

## 10. 담당 범위 코드 점검 결과와 보완 우선순위

아래 항목은 최신 `dev` 병합 후 `recommend-service`, Gateway 연동, 내부 API 소비 코드를
검토한 결과입니다. 현재 자동 테스트 통과와 운영 적합성을 구분하며, 실제 수정 전에는 팀의
Gateway 운영 방식과 실패 정책을 합의합니다.

### 10.1 최우선: 추천 API의 사용자 식별 신뢰 경계 확정

현재 추천 라우터는 `Authorization` 헤더의 존재만 요구하고 토큰을 직접 검증하지 않으며,
사용자 식별에는 Gateway가 주입한 `X-User-Id`를 사용합니다. 실제 Gateway 요청에서 외부가 보낸
`X-User-Id`를 제거하고 토큰 사용자 ID로 덮어쓰는 동작을 확인했습니다. 따라서 로컬 MVP의
신뢰 경계는 Gateway로 확정하되, 다음 운영 위험은 남아 있습니다.

1. 운영 환경에서도 외부의 `X-User-Id`, `X-User-Role`을 제거하고 검증된 토큰 값으로 다시 설정하는지 배포 검증합니다.
2. Gateway 우회를 허용해야 한다면 recommend-service도 JWT를 검증하고 토큰의 사용자 ID를 기준으로 처리합니다.

특히 Compose가 recommend-service의 `8085` 포트를 호스트에 직접 공개하므로 Gateway를 우회해
위조 헤더로 호출할 수 있는지 확인해야 합니다. 운영·공유 환경에서는 직접 포트 공개를 제거하거나
방화벽·네트워크 정책으로 Gateway만 접근하도록 제한합니다. 현재 Compose의 직접 포트 공개는
로컬 교육·디버깅 편의를 위한 설정이며 운영 적합성을 의미하지 않습니다.

### 10.2 최우선: 개발 기본 비밀값의 운영 유입 차단

현재 `local-internal-api-key`와 MariaDB의 `SqlDba-1`이 설정 및 Compose 기본값으로 남아 있습니다.
로컬 교육 환경의 실행 편의를 위한 값이지만, 환경변수 누락 시 서비스가 알려진 동일 값으로
실행되는 fail-open 설정입니다.

- 운영·공유 프로필에서는 `INTERNAL_API_KEY`, DB 사용자와 비밀번호가 없으면 시작을 실패시킵니다.
- 실제 값은 Git, Compose 파일, 로그가 아니라 Secret Manager 또는 배포 환경의 secret으로
  주입합니다.
- 하나의 내부 키를 여러 서비스가 공유하면 한 서비스 유출의 영향 범위가 전체로 넓어집니다.
  MVP 이후에는 서비스별 자격 증명이나 OAuth Client Credentials와 scope로 전환합니다.
- 내부 호출은 현재 평문 HTTP이므로 배포 환경에서는 네트워크 정책과 TLS 적용 여부를 함께
  검토합니다.

### 10.3 높음: Enrollment 장애의 fail-open 정책 재검토

`EnrollmentServiceClient`는 timeout, `401`, `500`, 응답 파싱 오류를 구분하지 않고 빈
`activeCourseIds`를 반환합니다. 추천 기능의 가용성은 유지되지만 내부 키 설정 오류나 서비스
장애가 숨겨지고, 이미 수강한 강의가 다시 추천될 수 있습니다.

- 인증 실패(`401`)와 계약·응답 오류는 설정 장애로 보고 실패를 노출하거나 `503`으로 변환합니다.
- timeout·일시적 `5xx`에만 제한적으로 빈 이력 fallback을 허용할지 팀과 결정합니다.
- fallback을 허용한다면 로그·메트릭에 원인과 횟수를 남기되 내부 키와 개인정보는 기록하지
  않습니다.
- 정상 빈 이력과 장애로 만든 빈 이력을 구분하는 테스트를 추가합니다.

### 10.4 높음: 내부 서비스 장애 응답을 일관되게 변환

user-service 장애는 라우터에서 `503`으로 변환하지만 course-service의
`CourseServiceUnavailable`은 현재 명시적으로 처리하지 않아 `500`이 될 수 있습니다.
Enrollment는 반대로 빈 값으로 계속 진행합니다. 동일한 내부 의존성 실패가 서로 다른 정책으로
처리되므로, 예외 유형과 외부 응답을 `503 Service Unavailable` 기준으로 정리하고 로그에는
상관관계 ID를 남기는 방안을 검토합니다.

### 10.5 높음: LLM 비용·남용 제한

요청 본문 길이와 출력 토큰에는 상한이 있지만 사용자별·회사별 요청 횟수 제한은 없습니다.
인증된 사용자라도 반복 호출하면 비용과 지연을 유발할 수 있습니다.

- Gateway 또는 recommend-service에 사용자·회사 기준 rate limit과 일일 사용량 제한을 둡니다.
- 후보가 0개이면 OpenAI를 호출하지 않고 명확한 빈 결과 또는 도메인 오류를 반환합니다.
- `MAX_CANDIDATES=20`, `MAX_RECOMMEND_COUNT=3`, timeout 값은 의미 있는 도메인 상수이지만
  운영 조정이 필요하면 환경 설정으로 이동하고 허용 범위를 검증합니다.
- OpenAI에 전달되는 `job`, `goal`은 외부 전송 데이터이므로 개인정보 입력 금지 안내와
  최소 수집·보관 기준을 정합니다. 프롬프트 지시는 보안 경계로 보지 않고 현재처럼 구조화 출력과
  후보 ID 재검증을 유지합니다.

### 10.6 중간: 시작 실패와 상태 확인 정책

현재 DB 테이블 준비, Eureka 등록, Kafka 시작 실패를 경고만 남기고 애플리케이션 시작을
계속합니다. 로컬 개발에는 편리하지만 DB 저장이 필수인 추천 요청은 실행 후에야 실패할 수 있고
단순 `/health`는 이를 구분하지 못합니다.

- 필수 의존성(DB)은 운영 프로필에서 시작 실패로 처리합니다.
- 선택 의존성(Eureka·Kafka)은 readiness와 liveness를 분리해 상태를 표시합니다.
- health 응답에 비밀값이나 내부 오류 문자열을 노출하지 않고 의존성 준비 여부만 제공합니다.

### 10.7 중간: 의존성 취약점과 재현 가능한 빌드

최신 `dev` 병합 후 `npm ci`는 프론트엔드 의존성에서 moderate 1개와 high 5개를 보고했습니다.
이는 취약 코드가 실제로 실행 가능하다는 결론은 아니므로 담당자와 함께 advisory, runtime 도달
가능성, 수정 버전의 breaking change를 확인합니다. `npm audit fix --force`는 사용하지 않고
lockfile을 유지한 상태에서 개별 업데이트와 프로덕션 빌드를 검증합니다. Python·Gradle 의존성도
릴리스 전에 각 생태계의 보안 점검을 수행하고 결과와 보류 이유를 기록합니다.

### 10.8 정리 대상: 사용되지 않는 JWT 보안 코드

`recommend-service/app/config/security.py`의 JWT 검증 함수는 현재 추천 라우터에서 사용되지
않습니다. Gateway 단독 검증을 최종 선택한다면 오해를 만드는 미사용 코드를 제거하고 계약을
문서화합니다. 서비스에서도 JWT를 검증하기로 하면 라우터 의존성으로 실제 연결하고 issuer,
JWKS timeout·캐시 갱신, 오류 메시지의 내부 정보 노출 여부를 테스트합니다. 존재만 하는 보안
코드는 보호 기능으로 간주하지 않습니다.
