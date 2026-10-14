# LinguaRoute AI 추천 개인과제 진행 프롬프트

나는 SKALA의 LinguaRoute MSA 팀 프로젝트에서 `feature/ai-recommendation` 브랜치와 `recommend-service`를 담당했다. 지금부터 팀 프로젝트 개발 자체보다 **개인과제 제출물 완성**을 최우선 목표로 작업해줘.

## 개인과제의 기준

강사님 지시는 다음과 같이 이해하고 적용한다.

- `코드이해_개인과제_템플릿1.docx`와 `코드이해_개인과제_템플릿2.docx`는 무엇을, 얼마나 구체적으로 분석해야 하는지 보여주는 참고 자료다.
- 최종 제출물은 `코드이해_개인과제_템플릿3.docx`의 고객 중심 서비스 보고서 형태로 완성한다.
- 단순히 코드를 나열하거나 주석을 복사하지 않는다.
- 내가 담당한 기능의 고객 가치, 시스템 구조, 코드 처리 흐름, 기술 선택 이유, 대안과 트레이드오프, 트러블슈팅, 실행 결과, 한계를 하나의 자연스러운 보고서로 연결한다.

## 먼저 확인할 자료

작업을 시작할 때 다음 자료를 직접 읽고 실제 내용만 근거로 사용해줘.

### 팀 프로젝트

- 저장소: `C:\Users\MNW\Desktop\SKALA\linguaroute-msa`
- 작업 브랜치: `feature/ai-recommendation`
- 핵심 서비스: `recommend-service`
- 핵심 문서:
  - `README.md`
  - `AGENTS.md`
  - `docs/product-spec.md`
  - `docs/api-spec.md`
  - `docs/erd.md`
  - `docs/mvp-checklist.md`
  - `docs/ai-recommendation-development-guide.md`
  - `C:\Users\MNW\Desktop\AI추천_Gateway_의사결정_기록.md`
- 관련 코드:
  - `recommend-service/app/client/`
  - `recommend-service/app/model/`
  - `recommend-service/app/provider/`
  - `recommend-service/app/repository/`
  - `recommend-service/app/router/`
  - `recommend-service/app/service/`
  - `recommend-service/tests/`
  - `docker-compose.yml`
  - `init-db/01_init.sql`

### 강의 및 과제 참고 자료

- `C:\Users\MNW\Downloads\KDT Cloud_Agile 방법론 및 MSA.pdf`
- `C:\Users\MNW\Downloads\msa-practice_scenario.pdf`
- `C:\Users\MNW\Downloads\Agile_MSA_실습_가이드_수정_v2.docx`
- `C:\Users\MNW\Downloads\코드이해_개인과제_템플릿1.docx`
- `C:\Users\MNW\Downloads\코드이해_개인과제_템플릿2.docx`
- `C:\Users\MNW\Downloads\코드이해_개인과제_템플릿3.docx`

## 반드시 지킬 원칙

1. 코드와 Git 이력, 테스트 결과, 문서에서 확인되지 않은 사실은 만들지 않는다.
2. 구현 완료, 통합 검증 완료, 계획 단계, 미검증 상태를 명확하게 구분한다.
3. “FastAPI가 빠르다”, “MSA가 좋다” 같은 일반론만 쓰지 말고 내 코드에서 그 기술이 어떤 문제를 해결했는지 연결한다.
4. 모든 주요 코드 분석은 아래 흐름으로 정리한다.

```text
기능 또는 코드의 역할
→ 기존 문제 상황
→ 문제의 원인
→ 고려한 대안
→ 실제 선택한 해결 방법
→ 그 방법을 선택한 이유
→ 코드의 처리 흐름
→ 테스트 또는 실행 결과
→ 고객·팀·시스템에 생긴 가치
→ 아직 남은 한계와 다음 작업
```

5. 팀원이 소유한 `course-service`, `user-service`, Auth Server와 내가 소유한 `recommend-service`, Gateway 연동 범위를 구분한다.
6. 팀원의 코드를 내가 구현한 것처럼 서술하지 않는다. 다른 서비스의 API를 소비하거나 통합한 작업은 “연동 및 계약 반영”으로 표현한다.
7. 코드 주석은 근거 자료로 활용하되 그대로 복사하지 말고 보고서 문장으로 재구성한다.
8. 개인과제이므로 “팀이 했다”보다 내가 맡은 판단·수정·검증을 주어로 명확히 쓴다.
9. 테스트가 통과하지 않은 항목이나 Docker 환경에서 확인하지 못한 항목은 성공했다고 표현하지 않는다.
10. 자연스러운 한국어로 작성하고, 과장된 표현이나 근거 없는 성과 수치는 사용하지 않는다.

## 집중적으로 분석할 내 작업

다음 항목을 개인과제의 주요 코드 이해 사례로 사용해줘.

1. `course-service` 추천 후보 내부 API를 호출하도록 `CourseServiceClient`를 변경한 이유
2. 강의 서비스의 `id`를 추천 도메인의 `courseId`로 경계에서 변환한 이유
3. Course의 `Language`, `Level`, `Situation`, `Status` enum을 요청 스키마에 반영한 이유
4. 문서가 아닌 실제 코드 계약을 우선 적용하면서도 서비스 담당 경계를 침범하지 않은 방식
5. `role`과 `businessRole`을 구분하고 `EMPLOYEE`, `ACTIVE`를 검사한 이유
6. `companyId`, `businessRole`, `status`를 `user-service` 내부 API에서 조회하도록 한 이유
7. 내부 API 호출에 `X-Internal-Api-Key`를 사용한 이유
8. Gateway 라우팅에서 추천 경로를 일반 강의 경로보다 먼저 평가한 이유
9. course-service가 ACTIVE와 언어를 필터링해도 recommend-service가 다시 검증하는 이유
10. AI 제공자의 결과에서 존재하지 않는 강의, 중복 강의, 빈 추천 이유를 검증한 방식
11. AI 제공자 실패 시 규칙 기반 추천으로 폴백하도록 한 이유
12. `recommendations`, `recommendation_items`의 소유권과 초기 DDL을 추가한 이유
13. 서비스 간 ID에는 외래키를 만들지 않고 추천 서비스 내부 관계에만 외래키를 둔 이유
14. 계약 테스트를 먼저 실패시킨 뒤 구현한 테스트 주도 진행 과정
15. 실제 OpenAI API와 Gateway 전체 통합 테스트가 아직 남아 있다는 한계
16. 넓은 Gateway 라우트가 내부 API까지 노출했던 원인과 우선순위 `-100` 차단 라우트를 선택한 이유
17. Gateway의 `404` 차단과 서비스의 `X-Internal-Api-Key` 검증을 함께 적용한 이유
18. Sol·Terra·Luna 중 비용 민감형 `gpt-5.6-luna`를 선택한 기준
19. Enrollment 이력의 `activeCourseIds`를 Course 조회의 `excludeIds`로 연결한 이유
20. 최신 `main`·`dev` 병합 충돌에서 추천 DDL과 팀원의 최신 Enrollment 스키마를 함께 보존한 과정
21. `Authorization` 존재 확인과 실제 JWT 검증의 차이, Gateway 헤더 덮어쓰기 검증이 필요한 이유
22. 개발용 내부 키·DB 비밀번호 기본값, Enrollment fail-open, 요청 제한 부재를 운영 보완점으로 판단한 근거

## 개인과제 보고서 구성

최종 보고서는 템플릿 3의 형식을 유지하면서 내 프로젝트에 맞게 다음 구조로 작성해줘.

### Chapter 1. 고객 중심 서비스 이해

- LinguaRoute와 AI 강의 추천 기능 개요
- 기업 외국어교육에서 직원이 겪는 문제
- AI 추천이 직원과 기업에 제공하는 가치
- 현재 구현의 고객 관점 한계
- 이후 OpenAI API 및 통합 기능 확장 제안

### Chapter 2. 시스템 기술 구조 및 설계 이유

- Agile과 MSA가 이 프로젝트에 필요한 이유
- `recommend-service`를 FastAPI로 구성한 이유
- 추천 서비스의 책임과 다른 서비스와의 경계
- 전체 추천 요청 처리 흐름
- Request Schema, Router, Client, Service, Provider, Repository의 역할
- Course enum 검증 구조
- Gateway, user-service, course-service 연동 구조
- 추천 결과 검증과 규칙 기반 폴백
- 추천 데이터 저장 구조

이 장에서는 핵심 코드 일부를 짧게 인용하고, 코드 아래에 “무엇을 하는가”뿐 아니라 “왜 이 구조로 만들었는가”를 설명한다.

### Chapter 3. 트러블슈팅 사례 정리

각 사례는 반드시 다음 형식을 사용한다.

```text
문제 상황
원인 분석
검토한 대안
선택한 해결 방법
선택 이유
수정한 코드 위치
테스트 및 확인 결과
배운 점
```

우선 다룰 사례:

- 문서의 강의 API와 실제 `dev` 코드 API가 달랐던 문제
- `id`와 `courseId` 불일치
- `role`과 `businessRole` 혼동 가능성
- `companyId`를 어디에서 얻을지 결정한 과정
- 초기 SQL에 추천 테이블이 없었던 문제
- 문자열 스키마가 미지원 Course enum을 허용했던 문제
- Python 의존성 및 로컬 테스트 환경 구성 문제
- Docker가 없어 Gateway·DB 통합 테스트를 수행하지 못한 제한
- 최신 `dev` 병합 과정에서 `docker-compose.yml`, `init-db/01_init.sql` 충돌을 계약 기준으로 해결한 사례
- 자동 테스트는 통과했지만 Gateway 직접 우회와 헤더 위조 방지는 아직 실제 환경에서 증명하지 못한 제한

### Chapter 4. Lesson Learned

- API 문서와 실제 코드 계약을 함께 검증해야 한다는 점
- MSA에서는 다른 서비스 코드를 편의상 수정하기보다 경계에서 계약을 맞춰야 한다는 점
- 인증(Authentication)과 인가(Authorization), 기존 `role`과 업무용 `businessRole`의 차이
- 외부 AI 결과를 그대로 신뢰하지 않고 서버에서 재검증해야 한다는 점
- 폴백이 AI 서비스의 가용성을 높이는 방식
- 테스트를 먼저 작성하면 계약 변경을 안전하게 반영할 수 있다는 점
- 구현하지 못했거나 확인하지 못한 내용을 정직하게 제한사항으로 기록해야 한다는 점

## 작업 순서

1. 템플릿 1·2·3과 강의 자료를 분석해 평가자가 기대하는 내용을 요약한다.
2. 현재 Git 상태와 커밋 이력을 확인해 내가 실제로 작업한 범위를 확정한다.
3. 관련 코드와 테스트에서 보고서에 사용할 근거를 수집한다.
4. 보고서에 들어갈 처리 흐름도와 MSA 연동 흐름을 먼저 설계한다.
5. 템플릿 3 구조로 초안을 작성한다.
6. 모든 기술 설명에 실제 코드 파일과 테스트 근거가 있는지 검토한다.
7. 팀원의 구현과 내 구현을 잘못 귀속한 문장이 없는지 확인한다.
8. 미검증 항목을 완료로 표현하지 않았는지 확인한다.
9. 초안을 나에게 먼저 보여주고 사실관계·개인 경험을 확인받는다.
10. 확인받은 내용만 반영해 최종 DOCX를 작성한다.
11. 템플릿 3의 레이아웃과 스타일을 최대한 유지하고, 모든 페이지를 렌더링해 표·코드·이미지 잘림을 검사한다.

## 상호작용 방식

- 처음부터 사실을 추측해 보고서를 완성하지 말고, 코드에서 알 수 없는 개인 경험만 질문한다.
- 질문은 한 번에 너무 많이 하지 말고 보고서 완성에 꼭 필요한 항목부터 묻는다.
- 내가 답하지 않은 개인적인 경험, 느낀 점, 담당 과정은 임의로 만들지 않는다.
- 각 단계에서 “확인된 사실”, “해석”, “추가 확인 필요”를 구분한다.
- 코드 변경은 개인과제 근거를 확인하는 데 꼭 필요한 경우에만 제안하고, 내 승인 없이 커밋·푸시하지 않는다.

위 기준으로 먼저 현재 자료와 코드 상태를 분석한 뒤, **개인과제 보고서 목차, 확보된 근거, 부족한 정보, 나에게 물어볼 질문**을 제시해줘.
