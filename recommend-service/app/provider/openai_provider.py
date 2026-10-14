import json
from typing import Any

from openai import AsyncOpenAI
from pydantic import BaseModel, Field

from app.model.schemas import CourseCandidate, ProviderRecommendation, RecommendationRequest


class OpenAiRecommendationResult(BaseModel):
    recommendations: list[ProviderRecommendation] = Field(max_length=3)


class OpenAiRecommendationProvider:
    """OpenAI 구조화 출력을 내부 추천 계약으로 변환한다.

    문제: 자유 형식 JSON을 직접 파싱하면 필드 누락·잘못된 타입·허용되지 않은 형식이
    서비스 로직까지 전파될 수 있다.
    선택: Responses API의 Pydantic 기반 구조화 출력을 사용해 SDK 경계에서 계약을 검증한다.
    이유: 프롬프트만으로 JSON 형식을 강제하는 방식보다 실패 지점이 명확하며, 이후 서비스가
    ``ProviderRecommendation``이라는 내부 타입에만 의존할 수 있다.
    결과: 파싱 거부나 빈 결과는 예외가 되어 상위 서비스의 규칙 기반 fallback으로 연결된다.
    """

    SYSTEM_PROMPT = (
        "당신은 기업 임직원 언어 강의 추천기입니다. 제공된 후보 안에서만 최대 3개를 "
        "선택하세요. 사용자의 직무, 비즈니스 상황, 전문용어가 담긴 자연어 목표, 수준과의 관련성을 근거로 각 추천 이유를 "
        "한국어 한 문장으로 작성하세요. 후보에 없는 courseId는 절대 만들지 마세요."
    )
    MAX_CANDIDATES = 20

    def __init__(self, *, client: Any, model: str, max_output_tokens: int):
        self.client = client
        self.model = model
        self.max_output_tokens = max_output_tokens

    @classmethod
    def from_api_key(
        cls,
        *,
        api_key: str,
        model: str,
        timeout_seconds: float,
        max_retries: int,
        max_output_tokens: int,
    ):
        return cls(
            client=AsyncOpenAI(
                api_key=api_key,
                timeout=timeout_seconds,
                max_retries=max_retries,
            ),
            model=model,
            max_output_tokens=max_output_tokens,
        )

    async def recommend(
        self, request: RecommendationRequest, candidates: list[CourseCandidate]
    ) -> list[ProviderRecommendation]:
        payload = {
            "request": request.model_dump(mode="json"),
            "candidates": [
                course.model_dump(mode="json")
                for course in candidates[: self.MAX_CANDIDATES]
            ],
        }
        response = await self.client.responses.parse(
            model=self.model,
            input=[
                {"role": "system", "content": self.SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": json.dumps(payload, ensure_ascii=False),
                },
            ],
            text_format=OpenAiRecommendationResult,
            # 사용자 입력이 길더라도 응답 비용과 지연이 무한히 커지지 않도록 상한을 둔다.
            max_output_tokens=self.max_output_tokens,
        )
        parsed = response.output_parsed
        if parsed is None:
            raise ValueError("OpenAI가 구조화된 추천 결과를 반환하지 않았습니다")
        return parsed.recommendations
