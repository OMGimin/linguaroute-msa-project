# AI 추천·Gateway 의사결정 기록

## 1. 문서 목적

이 문서는 AI 추천 기능과 API Gateway 연동을 진행하면서 발견한 문제, 검토한 대안,
최종 선택과 검증 결과를 개인과제 보고서에 재사용할 수 있도록 기록한 문서입니다.
단순히 완성된 코드의 동작을 나열하지 않고 다음 순서로 판단 과정을 남깁니다.

```text
문제 발견 → 원인 분석 → 대안 비교 → 선택 → 선택 이유 → 결과와 남은 제약
```

구현 위치의 주석은 해당 코드가 필요한 이유를 설명하고, 이 문서는 여러 서비스에 걸친
판단과 대안을 한곳에서 설명하는 역할을 합니다.

## 2. 담당 범위

- AI 강의 추천을 담당하는 `recommend-service`
- 외부 추천 경로와 내부 API 노출을 관리하는 API Gateway 연동
- `user-service`, `course-service`와의 서비스 간 API 계약 협의
- OpenAI API 제공자와 장애 fallback 설계
- 추천 요청과 결과의 MariaDB 저장 구조

다른 서비스가 소유한 데이터 조회와 내부 API 검증 로직은 해당 담당자와 계약을 합의하고,
추천 서비스와 Gateway에 필요한 소비자 측 구현만 직접 담당했습니다.

## 3. 주요 의사결정

### 3.1 문서보다 실제 서비스 코드와 확정 계약을 우선

초기 개발 문서에는 실제 `dev` 코드와 다른 경로와 응답 구조가 일부 남아 있었습니다.
문서만 따라 구현하면 컴파일은 되더라도 서비스 통합 시 실패할 가능성이 있었습니다.

- 대안 1: 다른 담당자에게 기존 코드를 모두 문서에 맞춰 수정하도록 요청
- 대안 2: 실제 코드의 안정된 계약을 확인하고 추천 서비스 경계에서 차이를 변환
- 선택: 변경 비용이 작은 경우 추천 서비스에서 변환하고, 장기 비용이 큰 경우 팀 계약으로 재확정
- 이유: 다른 서비스의 공개 API를 불필요하게 깨지 않으면서도 계약 불일치를 코드와 테스트로 드러낼 수 있음
- 결과: Course enum과 추천 전용 내부 API는 팀의 확정 계약을 따르고, `id` 명칭 차이는 추천 클라이언트에서 변환

### 3.2 Course 도메인 값을 자유 문자열이 아닌 enum으로 제한

`language`, `level`, `situation`을 단순 문자열로 받으면 `SPANISH`, `ARCHIVED`와 같은
미지원 값도 추천 서비스 내부까지 들어올 수 있었습니다.

- 선택: course-service와 동일한 enum 문자열을 Pydantic 요청·후보 모델에 적용
- 이유: 잘못된 입력을 외부 서비스 호출 전에 API 경계에서 `422`로 거부할 수 있음
- 결과: 서비스 간 계약 변경이 타입 검증과 자동 테스트에서 빠르게 드러남
- 확정값: `ENGLISH/JAPANESE/CHINESE`, `BEGINNER/ELEMENTARY/INTERMEDIATE/ADVANCED`, 확정 Situation 5종

관련 코드: `recommend-service/app/model/schemas.py`

### 3.3 일반 강의 목록 대신 추천 전용 내부 API 사용

일반 강의 API는 페이지 응답인 `data.content`를 반환하고 인증이 필요하며, 추천 서비스는
특정 언어의 `ACTIVE` 후보 배열이 필요합니다. 일반 목록 API에 결합하면 페이지 순회와
사용자 토큰 전달까지 추천 서비스의 책임이 됩니다.

- 대안 1: `GET /api/courses`의 페이지를 순회하며 추천 후보 수집
- 대안 2: 추천 목적의 내부 API 사용
- 선택: `GET /internal/courses/recommend?language={language}&excludeIds={courseIds}`
- 이유: 강의 상태와 언어 필터는 데이터를 소유한 course-service가 책임지는 편이 MSA 경계에 적합
- 결과: 추천 서비스는 후보 소비와 AI 결과 검증에 집중
- 추가 반영: Enrollment 이력의 `activeCourseIds`를 `excludeIds`로 전달해 이미 수강한 강의를 후보에서 제외
- 남은 제약: 실제 Docker 환경에서 정상 키와 누락·불일치 키를 사용한 통합 검증 필요

관련 코드: `recommend-service/app/client/course_client.py`

### 3.4 `id`와 `courseId` 차이는 클라이언트 경계에서 한 번만 변환

course-service 응답은 `id`, 추천 API와 AI 결과는 `courseId`를 사용합니다.
두 이름을 서비스 전체에서 함께 허용하면 검증 코드가 복잡해지고 잘못된 ID를 놓칠 수 있습니다.

- 선택: `CourseServiceClient`가 응답을 받을 때 `id → courseId`로 정규화
- 이유: 외부 계약의 차이를 한 경계에 격리하면 추천 서비스 내부 모델이 일관됨
- 결과: OpenAI 결과의 ID를 실제 후보 ID와 단순하고 명확하게 비교 가능

### 3.5 `role` 대신 최신 `businessRole`과 `status`로 인가

기존 `role`은 Auth Server 호환을 위한 값이고, 실제 직원·기업 관리자 권한은
`businessRole`에 저장됩니다. 토큰이나 Gateway 헤더만 믿으면 퇴사·비활성 처리처럼
로그인 이후 변경된 상태를 즉시 반영하기 어렵습니다.

- 대안 1: Gateway의 `X-User-Role`만 검사
- 대안 2: user-service에서 최신 권한 컨텍스트 재조회
- 선택: `/internal/users/{id}/authorization-context`에서 `companyId`, `businessRole`, `status` 조회
- 이유: 사용자 데이터 소유자가 최신 소속과 상태를 판단해야 함
- 결과: `businessRole=EMPLOYEE`이면서 `status=ACTIVE`인 사용자만 추천 가능

관련 코드: `recommend-service/app/client/user_client.py`, `recommend-service/app/router/recommend_router.py`

### 3.6 추천 테이블은 초기 SQL에 명시하되 서비스 간 외래키는 만들지 않음

SQLAlchemy 자동 생성에만 의존하면 새 환경의 초기 DDL에서 추천 테이블이 누락되고,
서비스 간 DB 외래키를 만들면 한 서비스의 배포와 데이터 변경이 다른 서비스에 결합됩니다.

- 선택: `recommendations`, `recommendation_items`를 초기 SQL에 명시
- 선택: user/course 데이터에는 외래키를 두지 않고 추천 서비스 내부 관계에만 FK 사용
- 이유: 재현 가능한 초기화와 MSA 데이터 소유권을 함께 유지
- 결과: 추천 요청과 항목은 함께 정리되지만 다른 서비스의 테이블 생명주기에는 의존하지 않음

관련 코드: `init-db/01_init.sql`

### 3.7 OpenAI를 교체 가능한 제공자로 분리하고 fallback 유지

서비스 로직에서 OpenAI SDK를 직접 호출하면 단위 테스트가 네트워크와 API 키에 종속되고,
외부 장애가 추천 API 전체 장애로 이어집니다.

- 대안 1: 서비스에서 OpenAI 직접 호출
- 대안 2: `RecommendationProvider` 경계 뒤에 외부·로컬 구현 배치
- 선택: `OpenAiRecommendationProvider`와 `LocalAiRecommendationProvider` 분리
- 이유: API 키가 없는 로컬 환경과 외부 장애 상황에서도 서비스를 실행할 수 있음
- 결과: 키가 있으면 OpenAI, 없으면 로컬 제공자를 사용하고 OpenAI 오류 시 규칙 기반 fallback 반환

관련 코드: `recommend-service/app/provider/`, `recommend-service/app/service/recommend_service.py`

### 3.8 기본 모델로 `gpt-5.6-luna` 선택

추천 작업은 새로운 강의를 생성하는 작업이 아니라 제공된 후보 중 최대 3개를 선택하고
이유를 작성하는 제한된 작업입니다.

- 대안: 품질 중심 Sol, 비용·품질 균형 Terra, 비용 민감형 Luna
- 선택: `gpt-5.6-luna`
- 이유: 반복 호출 가능성이 있는 제한된 분류·선택 작업이므로 비용 효율을 우선
- 결과: 기본값은 Luna로 두되 `OPENAI_MODEL` 환경변수로 배포 시 교체 가능

### 3.9 자유 형식 JSON 대신 구조화 출력과 서버 재검증 사용

LLM은 후보에 없는 강의 ID, 중복 ID 또는 잘못된 형식의 응답을 만들 수 있습니다.
프롬프트 지시만으로 이를 보안·무결성 경계로 사용할 수는 없습니다.

- 선택: Responses API의 Pydantic 구조화 출력 사용
- 추가 검증: 실제 후보 ID, 중복, 최대 3개, `ACTIVE`, 요청 언어 일치 여부를 서버에서 재검사
- 이유: 모델 출력은 신뢰할 수 없는 외부 입력으로 취급해야 함
- 결과: 잘못된 결과는 제거되고 유효 결과가 없거나 API 호출이 실패하면 fallback 실행

관련 코드: `recommend-service/app/provider/openai_provider.py`, `recommend-service/app/service/recommend_service.py`

### 3.10 Gateway 차단과 내부 API 키를 함께 적용

Gateway의 `/api/courses/**` 같은 넓은 라우트는 `/api/courses/internal/**`도 포함합니다.
Gateway에서만 차단하면 내부 네트워크의 직접 호출자를 구분하지 못하고, API 키만 적용하면
외부에 내부 경로 자체가 노출됩니다.

- 대안 1: Gateway 차단만 적용
- 대안 2: 서비스 내부 키만 적용
- 대안 3: 서비스용 OAuth scope 적용
- 선택: MVP에서는 Gateway `404` 차단과 `X-Internal-Api-Key`를 함께 적용
- 이유: 기존 user-service와 같은 방식으로 구현 비용을 낮추면서 두 신뢰 경계를 모두 방어
- 추후 고도화: 서비스용 OAuth Client Credentials와 네트워크 정책 검토
- 결과: 외부에는 내부 경로를 숨기고, 직접 서비스 호출도 제공 서비스가 키로 검증 가능

관련 코드: `docker-compose.yml`, `recommend-service/app/client/course_client.py`

## 4. 테스트 중심 개발 기록

내부 API 보안 변경에서는 구현 전에 다음 실패를 먼저 확인했습니다.

- `CourseServiceClient`가 내부 API 키 생성자 인자를 받지 못함
- Course 내부 요청에 `X-Internal-Api-Key`가 없음
- Gateway에 내부 경로 차단 라우트가 없음
- 추천 공개 라우트의 우선순위 계약이 테스트로 고정되지 않음

최신 `main`과 `dev` 병합 후 추천 서비스 자동 테스트 16개, Course·Enrollment·User·Payment
서비스의 Gradle 테스트와 Vue 프로덕션 빌드가 통과했습니다. 실제 Gateway 이미지에서의
`404` 응답과 내부 API 키 검증은 전체 MSA 통합 환경에서 추가 확인해야 합니다.

## 5. 현재 완료 상태와 남은 검증

### 코드와 자동 테스트로 확인됨

- Course enum 입력 검증
- 추천 후보 내부 API 호출 및 `id → courseId` 변환
- user-service 최신 권한 컨텍스트 조회
- `EMPLOYEE + ACTIVE` 인가
- OpenAI 구조화 출력 파싱
- 후보 ID·중복·상태·언어 재검증
- OpenAI 오류 시 규칙 기반 fallback
- Gateway 내부 경로 차단 구성
- Course 내부 호출의 API 키 헤더
- Enrollment 내부 호출의 API 키 헤더와 `activeCourseIds → excludeIds` 연결
- 추천 DB 초기화 DDL

### 실제 실행 환경에서 남음

- 팀 API 키를 사용한 Luna 실제 호출 1회
- course-service 내부 API 키 검증
- Gateway를 통한 내부 경로 `404` 확인
- Gateway가 외부 `X-User-*` 헤더를 제거하고 인증 값으로 덮어쓰는지 확인
- 전체 MSA에서 정상 추천과 fallback의 DB 저장 확인
- Gateway가 외부 `X-User-Id`를 제거·덮어쓰기 전에는 recommend-service 직접 포트 노출을 제한할 필요
- 알려진 개발용 내부 키·DB 비밀번호가 운영 환경에 기본값으로 유입되지 않도록 fail-fast 설정 필요
- Enrollment 장애를 빈 이력으로 처리하는 fail-open 정책과 요청 rate limit 재검토

## 6. 개인과제 작성에 사용할 수 있는 핵심 서술

이 작업의 핵심은 OpenAI API를 한 번 호출하는 데 있지 않았습니다. 서로 다른 서비스가
소유한 사용자 권한과 강의 데이터를 어떤 계약으로 조회할지 정하고, 외부 모델의 결과를
신뢰하지 않도록 검증 경계를 두며, 외부 장애가 서비스 전체 장애로 이어지지 않게 만드는
과정이었습니다. 구현에서는 enum 검증, 전용 내부 API, 경계에서의 ID 변환, 최신 권한 조회,
구조화 출력, 서버 재검증, fallback, Gateway 차단과 내부 API 키를 조합했습니다. 각 선택은
단순히 동작하게 만드는 것보다 서비스 책임 분리, 변경 비용, 보안과 장애 대응을 기준으로
결정했습니다.
