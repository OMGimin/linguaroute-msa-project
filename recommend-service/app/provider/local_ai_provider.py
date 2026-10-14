from typing import Protocol

from app.model.schemas import CourseCandidate, ProviderRecommendation, RecommendationRequest


class RecommendationProvider(Protocol):
    async def recommend(
        self, request: RecommendationRequest, candidates: list[CourseCandidate]
    ) -> list[ProviderRecommendation]: ...


class LocalAiRecommendationProvider:
    """외부 LLM 도입 전 사용하는 교체 가능한 로컬 추천 제공자."""

    async def recommend(
        self, request: RecommendationRequest, candidates: list[CourseCandidate]
    ) -> list[ProviderRecommendation]:
        ranked = sorted(
            candidates,
            key=lambda course: (
                course.level == request.level,
                course.situation == request.situation,
            ),
            reverse=True,
        )
        return [
            ProviderRecommendation(
                courseId=course.courseId,
                reason=self._reason(request, course),
            )
            for course in ranked[:3]
        ]

    @staticmethod
    def _reason(request: RecommendationRequest, course: CourseCandidate) -> str:
        matches = []
        if course.level == request.level:
            matches.append("현재 수준")
        if course.situation == request.situation:
            matches.append("사용 상황")
        context = "과 ".join(matches) if matches else "학습 목표"
        return f"{request.job} 직무의 {context}에 적합한 {course.language} 강의입니다."
