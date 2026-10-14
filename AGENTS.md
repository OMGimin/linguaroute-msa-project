# AGENTS.md

이 파일은 이 저장소에서 작업하는 Codex 및 기타 코딩 에이전트를 위한 프로젝트 지침입니다. 저장소 루트부터 모든 하위 경로에 적용합니다. 하위 폴더에 더 구체적인 `AGENTS.md` 또는 `AGENTS.override.md`가 있으면 해당 범위에서는 하위 지침을 함께 따릅니다.

## 1. 작업 원칙

- 사용자의 현재 요청을 가장 우선합니다.
- 작업을 시작하기 전에 `git status -sb`, 현재 브랜치, 관련 파일과 기존 문서를 확인합니다.
- 사용자가 만든 변경을 임의로 되돌리거나 덮어쓰지 않습니다.
- 요청 범위를 넘어서는 기능, 리팩터링, 의존성 또는 인프라 변경을 임의로 추가하지 않습니다.
- 현재 코드는 강사가 제공한 실습용 출발점입니다. 기존 코드가 최종 기획을 완전히 구현한다고 가정하지 않습니다.
- 코드나 문서를 변경했으면 관련 검증을 실제로 실행한 뒤 결과를 보고합니다.
- 컴파일 성공을 실행 성공으로 표현하지 않습니다. API 변경은 가능하면 실제 요청과 응답까지 확인합니다.
- 확인하지 않은 구현 상태나 테스트 결과를 완료된 것처럼 문서에 작성하지 않습니다.

## 2. 먼저 읽을 문서

작업 범위에 따라 다음 문서를 먼저 확인합니다.

- 프로젝트 실행 및 협업 규칙: [`README.md`](./README.md)
- 확정 MVP와 담당 영역: [`docs/product-spec.md`](./docs/product-spec.md)
- 기능별 진행 상황: [`docs/mvp-checklist.md`](./docs/mvp-checklist.md)
- API 설계: [`docs/api-spec.md`](./docs/api-spec.md)
- 데이터 소유권 및 ERD: [`docs/erd.md`](./docs/erd.md)

API, 데이터 모델 또는 실행 방식이 변경되면 관련 코드와 문서를 같은 작업 범위에서 함께 갱신합니다.

문서별 책임은 다음과 같습니다.

- `README.md`: 팀 협업과 로컬 실행 방법
- `docs/product-spec.md`: 기능 범위, 사용자 흐름, 담당자와 확정 구현 방침
- `docs/mvp-checklist.md`: 기획서의 기능 목록을 그대로 반영한 구현 진행 상태
- `docs/api-spec.md`: 외부·내부 API, 인증, 요청·응답, 오류와 Gateway 경로
- `docs/erd.md`: 테이블, 서비스별 데이터 소유권, 상태값과 Kafka 이벤트

같은 세부 명세를 여러 문서에 복사하지 말고 소유 문서로 연결합니다. 단, 기획서의 MVP 기능과 체크리스트는 진행 확인을 위해 의도적으로 1:1로 유지합니다.

## 3. 프로젝트 구성

- Java 21 / Spring Boot: `user-service`, `course-service`, `enrollment-service`, `payment-service`, `eureka-server`
- Python / FastAPI: `recommend-service`
- Vue 3 / Vite: `vue-frontend`
- 제공 Docker 이미지: Auth Server, API Gateway
- 로컬 통합 환경: 루트 `docker-compose.yml`
- 공통 API 진입점: API Gateway

### 팀원 로컬 인프라 최초 설정

팀원은 프로젝트 루트에 아래 세 분할 파일을 먼저 준비합니다.

```text
infra-images.tar.gz.part-aa
infra-images.tar.gz.part-ab
infra-images.tar.gz.part-ac
```

세 파일은 제공 Auth Server와 API Gateway 이미지 묶음입니다. 처음 한 번만 합쳐서 Docker에 불러옵니다.

```bash
cat infra-images.tar.gz.part-aa infra-images.tar.gz.part-ab infra-images.tar.gz.part-ac > infra-images.tar.gz
docker load -i infra-images.tar.gz
docker image inspect msa-lecture/auth-server:1.0
docker image inspect msa-lecture/api-gateway:1.0
```

현재 저장소의 소스 서비스를 처음 실행하거나 코드 변경을 반영할 때는 다음 명령을 순서대로 실행합니다.

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

이미 현재 소스로 서비스 이미지를 빌드한 뒤 단순히 다시 실행하는 경우에는 다운로드와 재빌드를 막을 수 있습니다.

```bash
docker compose up -d --no-build --pull never
docker compose ps
```

기동 확인은 Spring 서비스의 `/actuator/health`와 FastAPI 추천 서비스의 `/health`를 사용합니다.

기존 MariaDB 볼륨은 `init-db`가 다시 적용되지 않으므로 Compose의 `db-migration` 서비스가 `init-db/migrations/001_users_auth_compat.sql`을 Auth Server보다 먼저 매번 실행합니다. `lecture-db-migration`의 정상 상태는 계속 실행 중인 `Up`이 아니라 작업을 마친 `Exited (0)`입니다. 이 마이그레이션이 실패하면 Auth Server를 먼저 실행하거나 볼륨을 임의로 삭제하지 말고 마이그레이션 로그와 기존 스키마를 확인합니다.

- 위 이미지 묶음의 `msa-lecture/auth-server:1.0`을 사용하는 로컬 Compose 환경에서는 팀원이 `AUTH_WEB_CLIENT_SECRET`을 별도로 만들거나 `.env`에 입력하지 않습니다. `docker-compose.yml`이 배포 이미지의 로컬 실습용 `web-client` 등록값을 기본으로 `user-service`에 주입합니다.
- 다른 Auth Server 또는 운영 환경에서만 `AUTH_WEB_CLIENT_SECRET` 환경변수로 해당 환경의 등록값을 덮어씁니다.
- 이 인프라 묶음에는 현재 확인 기준 Auth Server와 API Gateway만 포함됩니다. 저장소 소스 서비스 이미지까지 포함된 강사 배포용 전체 묶음과 혼동하지 않습니다.
- 강사가 `docker-compose.local.yml`과 별도의 `msa-lecture-images.part.*` 전체 묶음을 함께 배포한 경우에는 그 배포 안내의 `--no-build --pull never` 절차를 우선합니다.
- 팀에 전달하는 완전 오프라인 ARM64 실행 파일은 `docker-compose.local.yml`, `msa-lecture-images-arm64.tar.part-aa`, `msa-lecture-images-arm64.tar.part-ab`, `msa-lecture-images-arm64.tar.part-ac` 네 개입니다. 세 조각을 `cat msa-lecture-images-arm64.tar.part-* > msa-lecture-images-arm64.tar`로 합친 뒤 `shasum -a 256 -c msa-lecture-images-arm64.tar.sha256`, `docker load -i msa-lecture-images-arm64.tar`, `docker compose -f docker-compose.local.yml up -d --no-build --pull never` 순서로 실행합니다.
- `docker-compose.local.yml`은 ARM64 사전 빌드 이미지 전용이므로 이 파일로 `docker compose build` 또는 `docker compose up --build`를 실행하지 않습니다.
- 현재 저장소 구성의 상세 실행 순서는 [`README.md`](./README.md)를 따릅니다.

서비스별 책임을 유지합니다. 다른 서비스의 내부 구현을 편의상 침범하지 않습니다.

- `course-service`는 강의와 차시를 소유합니다.
- `recommend-service`는 추천 요청과 추천 결과를 소유하며, `course-service` API로 `ACTIVE` 상태 및 요청 언어 일치 여부를 검증합니다.
- 기존 Auth Server는 공용 `users` 테이블의 호환 필드를 읽어 자체 이메일·비밀번호 로그인 후 OAuth2 Authorization Code 흐름으로 JWT Access Token을 발급합니다. 소셜 로그인은 구현하지 않습니다.
- `user-service`는 비밀번호 해시, 이메일 인증, 아이디 찾기, 비밀번호 변경·재설정, 기업·사용자 프로필, 비즈니스 역할·기업 소속, 초대·좌석과 약관 동의를 소유합니다.
- 서버 수를 늘리지 않습니다. 제공 Gateway 이미지는 수정하지 않고, `docker-compose.yml` 환경변수로 가능한 라우팅만 보정합니다. Gateway 이미지의 공개 허용 경로가 고정되어 있으면 외부 API 계약을 그 허용 경로에 맞추고 문서에 명시합니다.

### 3.1 목표 서비스

이 프로젝트의 목표는 AI 기반 기업 외국어교육 구독 플랫폼 `LinguaRoute`입니다.

```text
플랫폼 → 기업 구독 → 직원 초대 → 직원 강의 수강
```

- 기업 관리자는 월간·연간 구독, 결제, 좌석, 초대코드와 소속 직원을 관리합니다.
- 직원은 초대코드로 가입하고 강의를 검색·신청·학습하며 AI 추천을 사용합니다.
- 플랫폼 관리자는 기업, 사용자, 강의, 결제와 수강 상태를 운영합니다.
- AI는 직원의 언어, 수준, 직무, 상황과 목표를 분석하되 실제 등록된 `ACTIVE` 강의만 추천합니다.

### 3.2 요구사항 우선순위

구현 판단의 우선순위는 다음과 같습니다.

1. 사용자의 현재 명시적 요청
2. [`docs/product-spec.md`](./docs/product-spec.md)의 확정 MVP
3. [`docs/api-spec.md`](./docs/api-spec.md)의 API 계약
4. [`docs/erd.md`](./docs/erd.md)의 데이터 소유권과 제약조건
5. 기존 강사 제공 코드

- 문서와 기존 코드가 다르면 기존 코드에 맞춰 요구사항을 축소하지 않습니다.
- 먼저 현재 구현과 목표 설계의 차이를 확인하고, 담당 기능 단위로 최소 변경합니다.
- API 명세와 ERD는 목표 설계 초안입니다. 실제 구현 후 Swagger, 엔티티와 실행 결과를 대조해 문서 상태를 갱신합니다.
- 문서끼리 충돌하면 확정 MVP를 우선하고 충돌 내용을 사용자와 팀에 알립니다.

### 3.3 구현 범위

- `확정 MVP`: 현재 개발 대상입니다.
- `선택 기능`: 필수 MVP 완료 후 사용자가 요청한 경우에만 구현합니다.
- `제외 기능`: 현재 프로젝트에서 구현하지 않습니다.
- `추후 확장 기능`: 현재 작업 범위에서 제외합니다.

MVP 기능을 구현했을 때는 [`docs/mvp-checklist.md`](./docs/mvp-checklist.md)를 같은 Pull Request에서 갱신합니다. 기존 코드에 비슷한 기능이 있다는 이유만으로 완료 처리하지 않으며, 요구사항 일치·테스트·실행 확인·관련 문서 갱신까지 끝난 항목만 `[x]`로 표시합니다.

이메일 인증, 아이디 찾기, 회원 탈퇴와 기업 관리자의 직원별 진도율 조회는 확정 MVP이며 회원가입 시 필수 약관 동의를 저장합니다. 환불, AI 추천 거부·재추천, 언어별 강의 버전 관리와 최근 학습 위치 저장·이어보기는 제외 기능입니다. 주요 작업 감사 로그는 추후 확장 기능입니다.

`PaymentCompleted`, `PaymentFailed`, `SubscriptionCanceled`, `SubscriptionExpired`, `SubscriptionRenewed` 이벤트의 발행·소비는 확정 MVP입니다. 구독 해지는 현재 이용 기간까지 권한을 유지하고, 만료 이벤트에서 실제 권한을 종료합니다.

아이디 찾기는 이름과 기업 사업자번호를 조회 조건으로 사용하고, 등록된 로그인 이메일로만 안내 메일을 발송합니다. API 응답에는 이메일과 계정 존재 여부를 노출하지 않습니다.

확정 범위와 기술 선택은 [`docs/product-spec.md`](./docs/product-spec.md)의 「확정 구현 방침」을 따릅니다. 통신 세부값은 API 명세, 데이터·이벤트 세부값은 ERD를 기준으로 구현하며 다른 문서에 별도 값을 만들지 않습니다.

### 3.4 팀 담당 영역

담당자와 서비스별 책임의 원본은 [`docs/product-spec.md`](./docs/product-spec.md#10-팀-담당-영역)에서 관리합니다.

- 담당 서비스 밖의 API나 이벤트 계약을 변경하면 관련 담당자와 함께 확인합니다.
- 여러 서비스에 걸친 작업은 소유 서비스별 변경을 분리하고 통합 검증 계획을 작성합니다.

## 4. 브랜치 규칙

브랜치 흐름은 다음과 같습니다.

```text
feature/기능명 → dev → main
```

- `main`은 최종 결과물 브랜치입니다.
- `dev`는 기능 통합 및 검증 브랜치입니다.
- 기능과 문서 작업은 항상 최신 `dev`에서 만든 `feature/기능명` 브랜치에서 진행합니다.
- 기능명은 영문 소문자와 하이픈을 사용합니다. 예: `feature/user-login`, `feature/api-docs`
- `main`과 `dev`에 직접 커밋하거나 직접 푸시하지 않습니다.
- 기능 브랜치는 Pull Request를 통해 `dev`에 병합합니다.
- 최종 배포 또는 제출 시에만 `dev`에서 `main`으로 Pull Request를 생성합니다.
- 사용자가 특정 브랜치나 별도 절차를 명시하면 그 지시를 우선합니다.

새 기능 브랜치 생성 절차:

```bash
git checkout dev
git pull origin dev
git checkout -b feature/기능명
```

작업 중 최신 `dev` 동기화 절차:

```bash
git checkout feature/기능명
git fetch origin
git merge origin/dev
```

- 동기화 후 충돌과 테스트 결과를 확인합니다.
- `dev`가 존재하지 않으면 임의로 대체 브랜치를 만들거나 `main`에 직접 푸시하지 말고 저장소 관리자에게 확인합니다.
- `git push --force`, `git reset --hard`, 임의 rebase, 브랜치 삭제는 사용자의 명시적 요청 없이 실행하지 않습니다.

## 5. 커밋 규칙

Conventional Commits 형식을 사용합니다.

```text
타입(선택 범위): 한국어 제목

한국어 본문(선택)
```

규칙:

- 타입은 영문 소문자로 작성합니다.
- 타입 또는 선택 범위 뒤에 콜론과 공백을 넣습니다.
- 제목과 선택 본문은 한국어로 작성합니다.
- 제목 끝에 마침표를 붙이지 않습니다.
- 하나의 커밋에는 하나의 논리적 목적만 담습니다.
- 본문은 제목 다음 빈 줄 뒤에 작성하고, 무엇을 왜 변경했는지 설명합니다.
- 호환성을 깨는 변경은 `타입!:` 또는 `BREAKING CHANGE:` footer로 명시합니다.

사용 가능한 주요 타입:

- `feat`: 새로운 기능
- `fix`: 버그 수정
- `refactor`: 동작 변경 없는 구조 개선
- `docs`: 문서 변경
- `test`: 테스트 추가 또는 수정
- `style`: 동작과 무관한 서식 변경
- `chore`: 기타 설정 및 유지보수
- `build`: 빌드 시스템 또는 의존성 변경
- `ci`: CI 설정 변경
- `perf`: 성능 개선

예시:

```text
feat: 직원 초대코드 생성 기능 추가
fix(enrollment): 중복 수강신청 검증 오류 수정
docs: API 명세와 ERD 갱신
refactor: 결제 상태 변경 로직을 서비스로 분리
```

커밋 전 절차:

```bash
git status
git diff --check
git diff
git diff --cached
```

- `git add .` 또는 `git add -A`를 기본으로 사용하지 않습니다.
- 작업 범위에 포함되는 파일 경로만 명시적으로 스테이징합니다.
- 커밋과 푸시는 사용자가 명시적으로 요청한 경우에만 수행합니다.
- 기존 커밋 수정, amend 또는 커밋 이력 재작성은 사용자의 명시적 요청 없이 수행하지 않습니다.

## 6. Pull Request 규칙

- 기능 브랜치의 Base는 `dev`, Compare는 `feature/기능명`으로 설정합니다.
- `dev`에서 `main`으로 보내는 Pull Request는 최종 통합 검증 후 생성합니다.
- 제목은 커밋 규칙과 같은 형식의 한국어 제목을 사용합니다.
- 본문에는 변경 내용, 변경 이유, 영향 범위, 실행한 검증과 남은 제한사항을 작성합니다.
- 리뷰 의견과 실패한 필수 검사를 해결하기 전에는 병합하지 않습니다.
- 가능하면 `main`과 `dev`에 Pull Request 승인, 필수 상태 검사, 대화 해결을 요구하는 브랜치 보호 규칙을 적용합니다.

## 7. MSA 및 데이터 설계 규칙

- MVP 데이터베이스는 현재 구성대로 MariaDB 한 개와 공용 `lecture_db` 한 개를 유지합니다.
- 각 마이크로서비스는 공용 DB 안에서 자신의 테이블만 소유하고 접근합니다.
- 다른 서비스 소유 테이블을 직접 조회·조인하거나 서비스 사이 외래키를 만들지 않습니다.
- 다른 서비스의 `company_id`, `user_id`, `course_id`, `lesson_id` 등은 논리 참조 ID로만 저장합니다.
- 서비스 간 조회는 REST API, 비동기 상태 전달은 Kafka 이벤트를 사용합니다.
- Kafka 소비자는 `eventId`를 기준으로 중복 이벤트를 안전하게 처리하도록 설계합니다.
- 기업별 데이터 격리는 `company_id`와 인증 사용자의 소속을 함께 검증합니다.
- 클라이언트가 권한, 가격, 결제 금액, 진도율 같은 서버 책임 값을 결정하게 하지 않습니다.

서비스 경계를 바꾸거나 엔티티를 추가하면 [`docs/erd.md`](./docs/erd.md)를 갱신합니다.

## 8. API 규칙

- 외부 요청은 API Gateway와 `/api` 경로를 기준으로 설계합니다.
- 내부 API는 Gateway 외부 공개 경로에 노출하지 않고 `/internal/**` 경로로 분리합니다.
- Gateway는 외부 요청의 `/api/**/internal/**` 접근을 차단합니다.
- 서비스 간 내부 호출은 Gateway를 거치지 않고 대상 서비스를 직접 호출하며, 호출자는 `X-Internal-Api-Key` 헤더를 전달합니다.
- 내부 API 제공 서비스는 `X-Internal-Api-Key`를 실행 환경의 `INTERNAL_API_KEY`와 비교하여 검증합니다.
- 신규 내부 API를 `/api/{service}/internal/**` 형태로 만들지 않습니다. 기존 코드에 남은 `/api/**/internal/**` 경로는 담당 범위에서 `/internal/**`로 이전하고 호출 코드를 함께 수정합니다.
- 보호 API는 Bearer Token과 역할·기업 소속 권한을 모두 검증합니다.
- 브라우저 로그인은 Auth Server의 OAuth2 Authorization Code 흐름을 사용합니다. Auth Server가 Refresh Token을 반환하더라도 MVP 프론트엔드는 저장·갱신에 사용하지 않으며, Access Token 만료 시 재로그인합니다. 로그아웃은 Auth Server의 `POST /logout`과 클라이언트 Access Token 삭제를 함께 처리합니다.
- Access Token의 기존 로그인 역할은 비즈니스 권한으로 사용하지 않습니다. 보호 API는 `user-service`의 내부 권한 조회로 최신 `businessRole`, `companyId`, `status`를 확인합니다.
- 요청 DTO, 엔티티, 응답 DTO의 책임을 분리합니다.
- 정상 및 예외 HTTP 상태 코드를 구분하고 공통 오류 형식을 유지합니다.
- 생성 API는 `201 Created`, 일반 조회·수정은 `200 OK`, 본문 없는 삭제는 `204 No Content`를 우선 검토합니다.
- 결제 생성처럼 중복 위험이 있는 요청은 멱등성을 고려합니다.
- API Method, URL, Request, Response, 상태 코드 또는 권한이 바뀌면 [`docs/api-spec.md`](./docs/api-spec.md)를 함께 갱신합니다.
- 구현 후 Swagger UI와 실제 실행 요청으로 문서와 일치하는지 확인합니다.

## 9. 검증 기준

변경한 범위에 해당하는 검증만 선택해 실행하되, 실패를 숨기지 않습니다.

### 공통

```bash
git diff --check
```

### Spring Boot 서비스

변경한 서비스 폴더에서 실행합니다.

```bash
./gradlew test
```

- DB, Kafka, Eureka 등 외부 의존성 때문에 테스트가 실패하면 원인을 구분하여 보고합니다.
- 가능한 경우 `./gradlew compileJava`로 컴파일 여부도 분리해 확인합니다.
- Controller 매핑을 변경했다면 실행 중인 서비스 또는 Gateway를 통해 실제 경로와 상태 코드를 확인합니다.

### FastAPI 추천 서비스

```bash
cd recommend-service
python3 -m compileall -q .
```

관련 테스트가 추가되면 해당 테스트 명령을 우선 실행합니다.

### Vue 프론트엔드

```bash
cd vue-frontend
npm run build
```

화면 동작이 바뀌면 빌드 성공뿐 아니라 브라우저에서 실제 동작도 확인합니다.

### Docker Compose

```bash
docker compose config --quiet
docker compose config --services
```

실행 구성을 바꾼 경우 필요한 서비스의 상태와 로그도 확인합니다.

```bash
docker compose ps
docker compose logs --tail=200 서비스명
```

- `docker compose down -v`는 저장 데이터를 삭제하므로 사용자 승인 없이 실행하지 않습니다.

### 문서

- 링크 대상 파일이 실제로 존재하는지 확인합니다.
- Markdown 코드 블록이 닫혀 있는지 확인합니다.
- API와 ERD 문서는 구현 전 설계인지 실제 검증 완료 상태인지 명확히 표시합니다.

## 10. 보안 및 저장소 위생

- `.env`, 토큰, 비밀번호, API 키, 개인키 등 비밀정보를 커밋하지 않습니다.
- 실제 값 대신 `.env.example`과 설명 문서를 사용합니다.
- `node_modules`, `build`, `bin`, `dist`, `__pycache__`, 로그, IDE 설정, ZIP/TAR 같은 생성물과 대용량 파일을 커밋하지 않습니다.
- 새 파일을 추가하기 전에 `git status`와 `.gitignore` 적용 여부를 확인합니다.
- 민감정보가 이미 추적되었다면 단순히 `.gitignore`만 추가하지 말고 즉시 사용자에게 알립니다. 이력 삭제나 비밀값 교체는 승인 후 진행합니다.
- 의존성을 추가하거나 버전을 변경하기 전에 필요성과 영향을 확인합니다.

## 11. 완료 보고

작업을 마치면 다음을 간단히 보고합니다.

- 변경한 파일과 핵심 내용
- 실행한 검증과 결과
- 실행하지 못한 검증 및 이유
- 커밋·푸시·Pull Request 여부
- 사용자가 다음으로 해야 할 작업이 있는지 여부

## 12. 참고한 공식 문서

- [OpenAI 공식 AGENTS.md 안내](https://developers.openai.com/codex/guides/agents-md)
- [Git 공식 git-checkout 문서](https://git-scm.com/docs/git-checkout)
- [Conventional Commits 1.0.0 명세](https://www.conventionalcommits.org/en/v1.0.0/)
- [GitHub 공식 보호 브랜치 안내](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
