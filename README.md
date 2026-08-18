# LinguaRoute MSA

> 이 프로젝트는 교육 목적으로 작성된 실습 코드입니다. 상용 서비스에 적용하려면 배포 목적에 맞는 보완이 필요합니다. 문의: audit@korea.ac.kr, Sungryel Lim Ph.D

## 프로젝트 문서

- [에이전트 작업 지침](./AGENTS.md)
- [서비스 기획서](./docs/product-spec.md)
- [MVP 체크리스트](./docs/mvp-checklist.md)
- [API 명세서](./docs/api-spec.md)
- [ERD](./docs/erd.md)
- [AI 추천 개발 회고](./docs/learning-notes/ai-recommendation-development-retrospective.md)

## 팀 Git 협업 규칙

처음 참여하는 팀원도 같은 방식으로 작업할 수 있도록 아래 규칙을 지켜 주세요.

### 브랜치 역할

브랜치 흐름은 다음과 같습니다.

```text
feature/기능명 → dev → main
```

- `main`: 최종 결과물만 관리하는 브랜치입니다. 직접 작업하거나 직접 푸시하지 않습니다.
- `dev`: 각 기능을 모아 함께 테스트하는 개발 통합 브랜치입니다. 기능 브랜치의 Pull Request는 `dev`로 병합합니다.
- `feature/기능명`: 개인 기능 작업 브랜치입니다. 항상 최신 `dev`에서 생성합니다.
- 최종 배포 또는 제출 시에만 `dev`에서 `main`으로 Pull Request를 생성합니다.

브랜치 이름의 기능명은 영문 소문자와 하이픈을 사용합니다.

```text
feature/user-login
feature/course-list
feature/payment-cancel
```

### 최초 `dev` 브랜치 생성

저장소 관리자가 한 번만 실행합니다.

```bash
git checkout main
git pull origin main
git checkout -b dev
git push -u origin dev
```

### 기능 개발 순서

#### 1. 최신 `dev` 받기

새 작업을 시작하기 전에 로컬 `dev`를 최신 상태로 맞춥니다.

```bash
git checkout dev
git pull origin dev
```

#### 2. 기능 브랜치 생성하기

```bash
git checkout -b feature/기능명
```

예시:

```bash
git checkout -b feature/user-login
```

#### 3. 변경 사항 커밋하기

```bash
git status
git add 변경한파일
git commit -m "feat: 로그인 기능 추가"
```

`git add .`을 바로 사용하지 말고, `git status`로 확인한 뒤 작업한 파일만 추가하는 것을 권장합니다.

#### 4. 작업 중 최신 `dev`와 동기화하기

기능 개발 중에도 수시로 최신 `dev`를 병합합니다. 특히 Pull Request를 만들기 전에는 반드시 동기화합니다.

```bash
git checkout feature/기능명
git fetch origin
git merge origin/dev
```

충돌이 발생하면 충돌 파일을 수정하고 테스트한 뒤 커밋합니다. 해결하기 어렵다면 혼자 강제로 처리하지 말고 팀원과 함께 확인합니다.

#### 5. 기능 브랜치 푸시하기

처음 푸시할 때:

```bash
git push -u origin feature/기능명
```

이후 같은 브랜치에서 다시 푸시할 때:

```bash
git push
```

#### 6. `dev`로 Pull Request 만들기

- Base 브랜치는 `dev`로 지정합니다.
- Compare 브랜치는 본인의 `feature/기능명`으로 지정합니다.
- 변경 내용과 테스트 결과를 설명합니다.
- 리뷰가 끝난 뒤 `dev`에 병합합니다.
- `main`과 `dev`에는 직접 푸시하지 않습니다.

### 커밋 메시지 규칙

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 사용하며 제목과 본문은 한국어로 작성합니다.

```text
타입: 한국어 제목

한국어 본문(선택)
```

기본 규칙:

- 타입 뒤에 콜론과 공백을 입력합니다. 예: `feat: 로그인 기능 추가`
- 제목은 변경 내용을 알 수 있도록 짧고 명확한 한국어로 작성합니다.
- 제목 끝에는 마침표를 붙이지 않습니다.
- 하나의 커밋에는 하나의 목적만 담습니다.
- 본문은 선택 사항이며 변경 이유나 주의사항이 필요할 때 한국어로 작성합니다.

주요 타입:

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 기능 변경 없는 코드 구조 개선
- `docs`: README 등 문서 수정
- `test`: 테스트 추가 또는 수정
- `style`: 코드 동작과 무관한 서식 수정
- `chore`: 설정, 의존성 등 기타 작업
- `build`: 빌드 설정 또는 빌드 시스템 변경
- `ci`: CI 설정 변경

예시:

```text
feat: 강의 목록 조회 기능 추가
fix: 결제 취소 시 상태가 변경되지 않는 문제 수정
refactor: 수강 신청 검증 로직을 서비스로 분리
docs: 브랜치 및 커밋 컨벤션 추가
test: 사용자 회원가입 서비스 테스트 추가
chore: 불필요한 로그 파일 제외 설정 추가
```

본문이 필요한 경우:

```bash
git commit -m "fix: 중복 수강 신청 문제 수정" \
  -m "같은 사용자가 동일한 강의를 여러 번 신청하지 못하도록 검증 로직을 추가했습니다."
```

## 실행 방법

### 1. 사전 준비

다음 프로그램과 파일이 필요합니다.

- Docker Desktop
- `infra-images.tar`

`infra-images.tar`에는 API Gateway와 Auth Server 이미지가 들어 있습니다. 용량이 커서 Git에는 포함하지 않으므로 팀에서 별도로 전달받아 프로젝트 최상위 폴더에 넣습니다.

Auth Server 이미지는 LinguaRoute에서도 그대로 사용하고 추가 인증 기능은 `user-service`에 구현합니다. API Gateway 서버는 추가하지 않지만, 현재 이미지의 경로가 고정되어 있으므로 동일한 Gateway 한 대를 목표 라우팅·보안 설정이 반영된 이미지로 다시 빌드해야 합니다.

```text
linguaroute-msa/
├── docker-compose.yml
├── infra-images.tar
├── course-service/
├── enrollment-service/
└── ...
```

Docker Desktop을 실행한 뒤 터미널에서 다음 명령으로 정상 동작을 확인합니다.

```bash
docker --version
docker compose version
docker info
```

`docker info`에서 Docker 서버 정보가 출력되면 실행 준비가 된 것입니다. `Cannot connect to the Docker daemon` 오류가 나오면 Docker Desktop이 완전히 실행될 때까지 기다린 뒤 다시 확인합니다.

### 2. 프로젝트 폴더로 이동

모든 Docker Compose 명령은 `docker-compose.yml`이 있는 프로젝트 최상위 폴더에서 실행합니다.

```bash
cd linguaroute-msa
```

현재 위치와 필수 파일을 확인합니다.

```bash
pwd
ls docker-compose.yml infra-images.tar
```

### 3. 공통 이미지 불러오기

API Gateway와 Auth Server 이미지를 Docker에 불러옵니다. 처음 실행할 때 한 번만 하면 됩니다.

```bash
docker load -i infra-images.tar
```

다음 명령으로 두 이미지가 있는지 확인합니다.

```bash
docker images
```

이미지 목록에 아래 태그가 있어야 합니다.

```text
msa-lecture/api-gateway:1.0
msa-lecture/auth-server:1.0
```

### 4. Docker Compose 설정 확인

실행 전에 Compose 파일에 문법 오류가 없는지 확인합니다.

```bash
docker compose config --quiet
docker compose config --services
```

서비스 목록이 출력되고 오류 메시지가 없으면 정상입니다.

### 5. 전체 서비스 실행

처음 실행하거나 소스 코드가 변경된 경우 이미지를 빌드하면서 컨테이너를 실행합니다.

```bash
docker compose up -d --build
```

`-d`는 컨테이너를 백그라운드에서 실행한다는 의미이고, `--build`는 로컬 서비스 이미지를 다시 빌드한다는 의미입니다.

처음 실행할 때는 이미지 다운로드와 Gradle 빌드 때문에 시간이 걸릴 수 있습니다. 서비스는 다음 순서로 기동됩니다.

```text
MariaDB / Kafka
  → Eureka
    → Auth Server
      → API Gateway + 4개 서비스
        → Recommend Service
```

### 6. 실행 상태 확인

모든 컨테이너의 상태를 확인합니다.

```bash
docker compose ps
```

`STATUS`가 `Up` 또는 `healthy`이면 정상입니다. MariaDB, Kafka, Eureka, Auth Server의 상태 확인이 끝날 때까지 1~3분 정도 걸릴 수 있습니다.

주요 접속 주소와 포트는 다음과 같습니다.

```text
API Gateway        http://localhost:8080
Eureka Server      http://localhost:8761
Auth Server        http://localhost:9000
User Service       http://localhost:8081
Course Service     http://localhost:8082
Enrollment Service http://localhost:8083
Payment Service    http://localhost:8084
Recommend Service  http://localhost:8085
MariaDB            localhost:3379
Kafka              localhost:9092
```

Eureka 화면(<http://localhost:8761/>)에서 각 서비스가 등록되었는지도 확인합니다.

### 7. 로그 확인

전체 서비스의 최근 로그를 확인합니다.

```bash
docker compose logs --tail=100
```

새 로그를 계속 확인하려면 다음 명령을 사용합니다. 종료할 때는 `Ctrl+C`를 누릅니다.

```bash
docker compose logs -f
```

특정 서비스의 로그만 확인할 수도 있습니다.

```bash
docker compose logs -f --tail=100 서비스명
```

사용할 수 있는 서비스명은 다음과 같습니다.

```text
mariadb
kafka
eureka-server
auth-server
api-gateway
user-service
course-service
enrollment-service
payment-service
recommend-service
```

예를 들어 Course Service 로그만 보려면 다음과 같이 실행합니다.

```bash
docker compose logs -f --tail=100 course-service
```

### 8. 변경된 서비스만 다시 빌드

특정 서비스의 코드를 수정한 경우 전체를 다시 빌드하지 않고 해당 서비스만 재빌드할 수 있습니다.

```bash
docker compose up -d --build 서비스명
```

예시:

```bash
docker compose up -d --build course-service
```

변경 내용이 반영되지 않을 때만 캐시 없이 다시 빌드합니다.

```bash
docker compose build --no-cache course-service
docker compose up -d course-service
```

### 9. 전체 서비스 종료

컨테이너와 네트워크를 종료합니다. MariaDB와 Kafka 데이터 볼륨은 유지됩니다.

```bash
docker compose down
```

컨테이너를 다시 실행할 때는 다음 명령을 사용합니다.

```bash
docker compose up -d
```

데이터까지 완전히 초기화해야 할 때만 다음 명령을 사용합니다.

```bash
docker compose down -v
```

`docker compose down -v`는 MariaDB와 Kafka의 저장 데이터를 삭제하므로 팀원과 확인한 뒤 사용합니다.

### 10. 자주 발생하는 오류

#### Docker 서버에 연결할 수 없는 경우

```text
Cannot connect to the Docker daemon
```

Docker Desktop을 실행하고 `docker info`가 정상 출력되는지 확인합니다.

#### Auth Server 또는 API Gateway 이미지를 찾지 못하는 경우

```text
pull access denied
No such image
```

`infra-images.tar`가 프로젝트 최상위 폴더에 있는지 확인한 뒤 이미지를 다시 불러옵니다.

```bash
docker load -i infra-images.tar
docker images
```

#### 특정 서비스가 실행되지 않는 경우

먼저 상태와 해당 서비스 로그를 확인합니다.

```bash
docker compose ps
docker compose logs --tail=200 서비스명
```

### 프론트엔드 실행

현재 `vue-frontend`는 `docker-compose.yml`에 포함되어 있지 않으므로 백엔드 실행 후 별도 터미널에서 로컬로 실행합니다.

```bash
cd vue-frontend
npm install
npm run dev
```

브라우저 접속 주소: <http://localhost:3000/>
