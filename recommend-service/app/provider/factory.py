from app.config.settings import settings
from app.provider.local_ai_provider import (
    LocalAiRecommendationProvider,
    RecommendationProvider,
)
from app.provider.openai_provider import OpenAiRecommendationProvider


def build_recommendation_provider() -> RecommendationProvider:
    """환경에 맞는 추천 제공자를 조립한다.

    외부 AI를 서비스 본문에서 직접 생성하면 테스트가 네트워크와 API 키에 종속된다. 따라서
    조립 시점에만 설정을 확인하고, 서비스에는 동일한 Protocol 구현체를 주입한다. API 키가
    없는 팀원의 로컬 환경도 기존 규칙 제공자로 실행되며, 운영에서는 설정만으로 전환된다.
    """
    if not settings.openai_api_key:
        return LocalAiRecommendationProvider()
    return OpenAiRecommendationProvider.from_api_key(
        api_key=settings.openai_api_key,
        model=settings.openai_model,
        timeout_seconds=settings.openai_timeout_seconds,
        max_retries=settings.openai_max_retries,
        max_output_tokens=settings.openai_max_output_tokens,
    )
