from unittest.mock import AsyncMock

import pytest

from app.model.schemas import (
    CourseCandidate,
    EnrollmentHistoryResponse,
    RecommendationRequest,
    RecommendationSource,
)
from pydantic import ValidationError
from app.service.recommend_service import RecommendationService


def candidate(course_id: int, *, language: str = "ENGLISH", status: str = "ACTIVE"):
    return CourseCandidate(
        courseId=course_id,
        title=f"강의 {course_id}",
        language=language,
        level="INTERMEDIATE",
        situation="CUSTOMER_MEETING",
        status=status,
    )


@pytest.fixture
def recommendation_request():
    return RecommendationRequest(
        language="ENGLISH",
        level="INTERMEDIATE",
        job="GLOBAL_SALES",
        situation="CUSTOMER_MEETING",
        goal="제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 싶음",
    )


@pytest.mark.asyncio
async def test_ai_result_keeps_only_active_matching_unique_courses(recommendation_request):
    course_client = AsyncMock()
    course_client.get_candidates.return_value = [
        candidate(1),
        candidate(2, language="JAPANESE"),
        candidate(3, status="INACTIVE"),
    ]
    provider = AsyncMock()
    provider.recommend.return_value = [
        {"courseId": 1, "reason": "직무와 상황에 적합합니다."},
        {"courseId": 1, "reason": "중복 결과"},
        {"courseId": 999, "reason": "존재하지 않는 강의"},
    ]
    repository = AsyncMock()
    repository.save.return_value = 3001
    enrollment_client = AsyncMock()
    enrollment_client.get_enrollment_history.return_value = EnrollmentHistoryResponse(
        userId=7, activeCourseIds=[8, 9]
    )

    service = RecommendationService(course_client, enrollment_client, provider, repository)
    result = await service.recommend(
        user_id=7, company_id=10, request=recommendation_request
    )

    assert result.recommendationId == 3001
    assert result.source is RecommendationSource.AI
    assert [course.courseId for course in result.courses] == [1]
    course_client.get_candidates.assert_awaited_once_with(
        recommendation_request.language, exclude_ids=[8, 9]
    )
    repository.save.assert_awaited_once()


@pytest.mark.asyncio
async def test_provider_failure_returns_rule_based_fallback(recommendation_request):
    course_client = AsyncMock()
    course_client.get_candidates.return_value = [candidate(1), candidate(2)]
    provider = AsyncMock()
    provider.recommend.side_effect = RuntimeError("AI provider unavailable")
    repository = AsyncMock()
    repository.save.return_value = 3002
    enrollment_client = AsyncMock()
    enrollment_client.get_enrollment_history.return_value = EnrollmentHistoryResponse(
        userId=7, activeCourseIds=[]
    )

    service = RecommendationService(course_client, enrollment_client, provider, repository)
    result = await service.recommend(
        user_id=7, company_id=10, request=recommendation_request
    )

    assert result.source is RecommendationSource.RULE_BASED_FALLBACK
    assert [course.courseId for course in result.courses] == [1, 2]
    assert all(course.reason for course in result.courses)
    repository.save.assert_awaited_once()


def test_course_candidate_rejects_unknown_status():
    with pytest.raises(ValidationError):
        candidate(1, status="ARCHIVED")
