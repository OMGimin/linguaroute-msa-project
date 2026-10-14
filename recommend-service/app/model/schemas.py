from datetime import datetime, timezone
from enum import Enum, StrEnum
from typing import List

from pydantic import BaseModel, Field


class RecommendationSource(str, Enum):
    AI = "AI"
    RULE_BASED_FALLBACK = "RULE_BASED_FALLBACK"


class RecommendationStatus(str, Enum):
    SUCCESS = "SUCCESS"
    FALLBACK = "FALLBACK"
    FAILED = "FAILED"


class CourseLanguage(StrEnum):
    ENGLISH = "ENGLISH"
    JAPANESE = "JAPANESE"
    CHINESE = "CHINESE"


class CourseLevel(StrEnum):
    BEGINNER = "BEGINNER"
    ELEMENTARY = "ELEMENTARY"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"


class CourseSituation(StrEnum):
    CUSTOMER_MEETING = "CUSTOMER_MEETING"
    PRESENTATION = "PRESENTATION"
    EMAIL = "EMAIL"
    BUSINESS_TRIP = "BUSINESS_TRIP"
    DAILY_CONVERSATION = "DAILY_CONVERSATION"


class CourseStatus(StrEnum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"


class RecommendationRequest(BaseModel):
    # [설계 이유 - Course 도메인 enum 재사용]
    # 문제: 단순 str 타입은 SPANISH, ARCHIVED 같은 미지원 값도 통과시켜
    # course-service 호출 뒤에야 실패 원인을 알 수 있었다.
    # 선택: 팀에서 확정한 Course enum 문자열을 요청 스키마에도 동일하게 고정한다.
    # 결과: 잘못된 값은 FastAPI/Pydantic 경계에서 즉시 422로 거부되고,
    # 추천 서비스와 course-service의 계약 변경도 테스트에서 빠르게 드러난다.
    language: CourseLanguage
    level: CourseLevel
    job: str = Field(min_length=1, max_length=50)
    situation: CourseSituation
    goal: str = Field(min_length=1, max_length=500)


class CourseCandidate(BaseModel):
    courseId: int
    title: str
    language: CourseLanguage
    level: CourseLevel
    situation: CourseSituation | None = None
    status: CourseStatus


class EnrollmentHistoryResponse(BaseModel):
    userId: int
    activeCourseIds: list[int]


class ProviderRecommendation(BaseModel):
    courseId: int
    reason: str = Field(min_length=1, max_length=500)


class RecommendedCourse(BaseModel):
    courseId: int
    title: str
    language: CourseLanguage
    level: CourseLevel
    reason: str


class RecommendationData(BaseModel):
    recommendationId: int
    source: RecommendationSource
    courses: List[RecommendedCourse]


class RecommendationResponse(BaseModel):
    data: RecommendationData
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
