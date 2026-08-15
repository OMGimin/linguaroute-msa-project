# LinguaRoute 조별 발표 기획서

> 발표 흐름: 왜(Pain Point) → 무엇(AI 솔루션) → 우선순위(스프린트) → 구현(아키텍처·API) → 결과(화면)

## 발표 산출물

- 최종 발표자료: `output/presentation/LinguaRoute_최종발표.key`
- Chrome 시연 순서: [LinguaRoute 발표 시연 가이드](./demo-scenario.md)
- 자동 시연 스킬: `$linguaroute-demo`

발표자료는 25장으로 구성하며 슬라이드별 설명과 저장소 근거는 [발표 대본](../output/LinguaRoute_발표대본.md)에 정리합니다. 동작 화면은 2026-08-11 실제 Chrome에서 다시 캡처한 랜딩·직원·기업 관리자·플랫폼 관리자 화면 9장을 사용합니다.

## 1. 이해관계자 가치(Pain Point)

| 이해관계자 | Pain Point | 중요한 이유 |
| --- | --- | --- |
| 기업 관리자 | 구독·결제·좌석·초대·진도가 흩어져 교육 운영 상태를 한눈에 보기 어려움 | 운영 비용과 계약 좌석을 통제해야 함 |
| 직원 | 일반 회화 강의만으로 실제 직무 상황과 산업별 전문용어를 익히기 어려움 | 업무에 바로 적용할 비즈니스 언어 과정을 빠르게 찾아야 함 |
| 플랫폼 관리자 | 기업·사용자·강의·거래 상태가 나뉘어 전체 운영 위험을 빠르게 파악하기 어려움 | 서비스 안정성과 콘텐츠 품질을 유지해야 함 |

세 문제는 다음 흐름으로 연결됩니다.

```text
기업 구독·결제 → 좌석·초대 → 직원 수강·학습 → 기업 진도 확인 → 플랫폼 운영
```

## 2. 이를 해결하기 위한 AI 솔루션

LinguaRoute는 기업의 구독과 좌석, 직원의 강의 탐색과 학습, 기업·플랫폼 운영 결과를 한 경로로 연결합니다.

AI 추천의 목표 역할:

- 직원의 언어·수준·직무·실제 비즈니스 상황·목표 분석
- 목표에 포함된 산업 도메인과 전문용어 요구를 기존 강의 정보와 연결
- 적합한 강의와 추천 이유 제공
- AI 장애 시 같은 계약의 규칙 기반 대체 응답 제공

서버가 보장하는 기준:

- 실제 등록된 `ACTIVE` 강의만 반환
- 선택 언어 일치 여부 확인
- 기존 수강 강의 제외
- 최신 직원 역할·소속·상태 검증

현재 저장소는 OpenAI·로컬 Provider, 최신 직원 권한·수강 이력·활성 강의 조회, 결과 재검증, 규칙 기반 fallback, MariaDB 저장과 Vue 화면 연동을 구현했습니다. 실제 직원 OAuth 토큰을 사용한 Gateway 요청과 추천 항목 저장도 확인했으므로 완료 기능과 시연 범위에 포함합니다.

발표의 핵심 차별점은 단순한 외국어 강의 추천이 아닙니다. `해외 고객 미팅`, `업무 발표`처럼 일반 학습 서비스가 세밀하게 다루기 어려운 직무별 비즈니스 시나리오와 산업 전문용어 학습 목표를 입력받아 실제 수강 가능한 기업 강의로 연결합니다. MVP에서는 산업 도메인·전문용어를 별도 코드값으로 만들지 않고 자연어 `goal`과 강의 설명·상황 정보를 활용하므로, 구현하지 않은 도메인 분류 체계를 별도 기능처럼 주장하지 않습니다.

## 3. 스프린트 구분

강사 가이드의 `walking skeleton → 나머지 서비스 확장` 기준을 적용합니다.

| 구분 | 구현 서비스 | 구현 결과 | 현재 상태 |
| --- | --- | --- | --- |
| Sprint 1 | `course-service`, `enrollment-service`, Vue 프론트엔드 | 직원 로그인 → 강의 검색·필터 → 상세 조회 → 수강신청 → 내 수강 목록의 한 흐름 완성 | 구현·검증 완료 |
| Sprint 2 | `payment-service`, Kafka, `user-service` | 구독 결제·멱등 처리, 결제·구독 이벤트 5종 발행·소비, 기업 좌석 권한 자동 갱신과 운영 화면 연결 | 구현·검증 완료 |
| Sprint 2 | `recommend-service` | 직원 조건 기반 추천, 실제 `ACTIVE` 강의 검증, fallback, 추천 이력 저장과 Vue 연동 | 구현·검증 완료 |

따라서 Sprint 1과 Sprint 2의 확정 MVP는 현재 코드와 통합 검증으로 완료됐습니다. 운영 환경에서는 OpenAI 키·비용 제한·직접 서비스 포트 차단 같은 배포 보완이 별도로 필요합니다.

## 4. 아키텍처 구성도

```text
Vue 3 Frontend
       ↓ REST /api + Bearer Token
API Gateway ── OAuth2/JWT ── Auth Server
       │ Eureka에서 서비스 주소 조회
       ↓ REST /api 라우팅
user-service · course-service · enrollment-service · payment-service · recommend-service
       ↓ 공용 DB 안의 서비스별 소유 테이블
MariaDB lecture_db

payment-service ── 결제·구독 이벤트 5종 ── Kafka ── user-service 권한 갱신
```

| 구성요소 | LinguaRoute 책임 |
| --- | --- |
| Auth Server | 이메일·비밀번호 로그인, OAuth2 Authorization Code, JWT 발급 |
| API Gateway | 외부 `/api` 단일 진입점, 인증, 서비스 라우팅 |
| Eureka | 서비스 등록과 탐색 |
| `user-service` | 기업·사용자·역할·소속·초대·좌석·약관 |
| `course-service` | 강의와 차시, 검색, `ACTIVE` 상태 |
| `enrollment-service` | 수강신청, 학습 상태, 서버 진도율 계산 |
| `payment-service` | 요금제, 구독, 모의 결제, Kafka 이벤트 |
| `recommend-service` | 추천 Provider, 직원 권한·수강 이력·활성 강의 검증, fallback과 추천 이력 저장 |
| Kafka | 결제·구독 상태 이벤트 전달 |
| MariaDB | 하나의 DB 안에서 서비스별 소유 테이블 분리 |

## 5. API 명세

| 화면 | Method | Gateway URL | 권한 | 응답 핵심 |
| --- | --- | --- | --- | --- |
| 기업 대시보드 | `GET` | `/api/companies/me/enrollments/progress` | 기업 관리자 | 직원별 강의·진도율 |
| 구독·결제 | `GET` | `/api/subscriptions/me`, `/api/payments` | 기업 관리자 | 구독 상태·결제 내역 |
| 강의 찾기 | `GET` | `/api/courses?language=JAPANESE` | 직원 | `ACTIVE` 강의 페이지 |
| 내 학습 | `GET` | `/api/enrollments/me` | 직원 | 수강 상태·서버 진도율 |
| 운영 대시보드 | `GET` | `/api/admin/companies`, `/api/admin/users`, `/api/admin/payments` | 플랫폼 관리자 | 서비스별 운영 목록 |
| 강의 관리 | `GET` | `/api/admin/courses?status=INACTIVE` | 플랫폼 관리자 | 비활성 강의 포함 목록 |

요청·응답 예시:

```http
GET /api/courses?language=JAPANESE
Authorization: Bearer {accessToken}
```

```json
{
  "content": [
    {
      "id": 9103,
      "title": "일본 출장 회화",
      "status": "ACTIVE"
    }
  ]
}
```

전체 외부·내부 API, 오류 코드와 권한 계약은 [API 명세](./api-spec.md)가 원본입니다.

## 6. 동작 화면 스냅샷

발표자료에는 2026-08-11 로컬 Docker Compose와 Chrome에서 다시 확인한 다음 화면을 사용합니다.

| 흐름 | 요청 전·조건 | 요청 후·결과 |
| --- | --- | --- |
| 직원 강의 탐색 | `강의 찾기`에서 일본어 필터 선택 | 실제 `ACTIVE` 일본어 강의 1개 표시 |
| 직원 학습 | 강의 탐색·수강 데이터 | `COMPLETED 100%`, `LEARNING 33.33%` 표시 |
| 직원 AI 추천 | 영어·고급·글로벌 세일즈·업무 발표 조건 | 최대 3개 활성 강의, 추천 이유와 출처 표시 |
| 기업 운영 | 소속 직원·좌석·수강 데이터 | 좌석 `3 / 50`, 평균 진도율 `44%` 표시 |
| 플랫폼 운영 | 강의 상태 필터를 `INACTIVE`로 선택 | 비활성 강의와 `활성화` 작업 표시 |

발표 중에는 결제 생성, 초대 생성, 수강 상태 변경, 강의 활성·비활성처럼 데이터를 변경하는 동작을 실행하지 않습니다. 자세한 역할별 순서와 실패 복구 절차는 [시연 가이드](./demo-scenario.md)를 따릅니다.

## 발표 슬라이드 순서

1. 표지
2. 이해관계자별 Pain Point
3. 하나로 이어지는 핵심 가치 경로
4. AI 추천 Provider와 서버 재검증·저장 경계
5. Sprint 1·2 완료 범위
6. `recommend-service`를 포함한 MSA 아키텍처 구성도
7. 프론트 Gateway API 호출 원칙
8. 추천 API를 포함한 핵심 엔드포인트 목록
9. 랜딩 → 직원 로그인 화면
10. 직원 일본어 강의 필터 화면
11. 직원 내 학습 화면
12. 기업 대시보드 → 구독·결제 → 직원 진도 화면
13. 플랫폼 운영 대시보드 → 비활성 강의 화면
14. 기업 관리자 → 직원 AI 추천·학습 → 플랫폼 관리자 시연 전환
