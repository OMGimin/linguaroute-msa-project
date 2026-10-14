import logging

from app.client.course_client import CourseServiceClient, course_client
from app.client.enrollment_client import EnrollmentServiceClient, enrollment_client
from app.model.schemas import (
    CourseCandidate,
    CourseStatus,
    ProviderRecommendation,
    RecommendedCourse,
    RecommendationData,
    RecommendationRequest,
    RecommendationSource,
    RecommendationStatus,
)
from app.provider.factory import build_recommendation_provider
from app.provider.local_ai_provider import RecommendationProvider
from app.repository.recommendation_repository import (
    RecommendationRepository,
    SqlAlchemyRecommendationRepository,
)

logger = logging.getLogger(__name__)


class RecommendationService:
    MAX_RECOMMEND_COUNT = 3

    def __init__(
        self,
        course_client: CourseServiceClient,
        enrollment_client: EnrollmentServiceClient,
        provider: RecommendationProvider,
        repository: RecommendationRepository,
    ):
        self.course_client = course_client
        self.enrollment_client = enrollment_client
        self.provider = provider
        self.repository = repository

    async def recommend(
        self, *, user_id: int, company_id: int, request: RecommendationRequest
    ) -> RecommendationData:
        enrollment_history = await self.enrollment_client.get_enrollment_history(user_id)
        # activeCourseIds라는 이름은 기존 소비자 호환을 위해 유지되지만 실제로는
        # ENROLLED/LEARNING/COMPLETED 강의 전체다. 이미 접한 강의를 다시 추천하지 않도록
        # course-service의 excludeIds 계약으로 경계에서 명시적으로 변환한다.
        excluded_course_ids = enrollment_history.activeCourseIds
        candidates = await self.course_client.get_candidates(
            request.language, exclude_ids=excluded_course_ids
        )
        # [방어적 재검증]
        # course-service가 ACTIVE와 언어 필터를 적용하지만, AI 추천 결과의 신뢰성과
        # 저장 데이터의 무결성은 recommend-service의 책임이기도 하다. 상대 서비스의
        # 버그나 계약 변경이 있어도 비활성·타 언어 강의가 추천/저장되지 않도록
        # 서비스 경계 안에서 같은 핵심 조건을 한 번 더 검사한다.
        valid_candidates = [
            course
            for course in candidates
            if course.status is CourseStatus.ACTIVE
            and course.language == request.language
        ]

        try:
            provider_results = await self.provider.recommend(request, valid_candidates)
            courses = self._validate_results(provider_results, valid_candidates)
            if not courses:
                raise ValueError("유효한 추천 결과가 없습니다")
            source = RecommendationSource.AI
            status = RecommendationStatus.SUCCESS
        except Exception as error:
            logger.warning("추천 제공자 호출 실패, 규칙 기반 추천으로 전환: %s", error)
            courses = self._fallback(request, valid_candidates)
            source = RecommendationSource.RULE_BASED_FALLBACK
            status = RecommendationStatus.FALLBACK

        recommendation_id = await self.repository.save(
            user_id=user_id,
            company_id=company_id,
            request=request,
            source=source,
            status=status,
            courses=courses,
        )
        return RecommendationData(
            recommendationId=recommendation_id,
            source=source,
            courses=courses,
        )

    def _validate_results(
        self,
        results: list[ProviderRecommendation] | list[dict],
        candidates: list[CourseCandidate],
    ) -> list[RecommendedCourse]:
        candidates_by_id = {course.courseId: course for course in candidates}
        selected: list[RecommendedCourse] = []
        seen: set[int] = set()
        for raw_result in results:
            result = (
                raw_result
                if isinstance(raw_result, ProviderRecommendation)
                else ProviderRecommendation.model_validate(raw_result)
            )
            course = candidates_by_id.get(result.courseId)
            if course is None or result.courseId in seen:
                continue
            seen.add(result.courseId)
            selected.append(self._to_recommended_course(course, result.reason))
            if len(selected) == self.MAX_RECOMMEND_COUNT:
                break
        return selected

    def _fallback(
        self, request: RecommendationRequest, candidates: list[CourseCandidate]
    ) -> list[RecommendedCourse]:
        ranked = sorted(
            candidates,
            key=lambda course: (
                course.level == request.level,
                course.situation == request.situation,
            ),
            reverse=True,
        )
        return [
            self._to_recommended_course(
                course, "선택한 언어, 수준과 상황을 기준으로 추천한 강의입니다."
            )
            for course in ranked[: self.MAX_RECOMMEND_COUNT]
        ]

    @staticmethod
    def _to_recommended_course(course: CourseCandidate, reason: str) -> RecommendedCourse:
        return RecommendedCourse(
            courseId=course.courseId,
            title=course.title,
            language=course.language,
            level=course.level,
            reason=reason,
        )


recommend_service = RecommendationService(
    course_client=course_client,
    enrollment_client=enrollment_client,
    provider=build_recommendation_provider(),
    repository=SqlAlchemyRecommendationRepository(),
)
