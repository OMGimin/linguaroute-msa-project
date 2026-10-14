from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from app.model.schemas import CourseCandidate, RecommendationRequest
from app.provider.openai_provider import (
    OpenAiRecommendationProvider,
    OpenAiRecommendationResult,
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


@pytest.fixture
def candidates():
    return [
        CourseCandidate(
            courseId=11,
            title="글로벌 세일즈 미팅 영어",
            language="ENGLISH",
            level="INTERMEDIATE",
            situation="CUSTOMER_MEETING",
            status="ACTIVE",
        )
    ]


@pytest.mark.asyncio
async def test_openai_provider_uses_structured_output(
    recommendation_request, candidates
):
    client = SimpleNamespace(responses=SimpleNamespace(parse=AsyncMock()))
    client.responses.parse.return_value = SimpleNamespace(
        output_parsed=OpenAiRecommendationResult(
            recommendations=[
                {"courseId": 11, "reason": "해외 고객 미팅 목표와 수준에 맞습니다."}
            ]
        )
    )
    provider = OpenAiRecommendationProvider(
        client=client, model="test-model", max_output_tokens=600
    )

    result = await provider.recommend(recommendation_request, candidates)

    assert [item.courseId for item in result] == [11]
    call = client.responses.parse.await_args.kwargs
    assert call["model"] == "test-model"
    assert call["max_output_tokens"] == 600
    assert call["text_format"] is OpenAiRecommendationResult
    assert "courseId" in call["input"][1]["content"]


@pytest.mark.asyncio
async def test_openai_provider_rejects_response_without_parsed_output(
    recommendation_request, candidates
):
    client = SimpleNamespace(responses=SimpleNamespace(parse=AsyncMock()))
    client.responses.parse.return_value = SimpleNamespace(output_parsed=None)
    provider = OpenAiRecommendationProvider(
        client=client, model="test-model", max_output_tokens=600
    )

    with pytest.raises(ValueError, match="구조화된 추천 결과"):
        await provider.recommend(recommendation_request, candidates)
