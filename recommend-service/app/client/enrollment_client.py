import httpx
import logging
from app.config.settings import settings
from app.model.schemas import EnrollmentHistoryResponse

logger = logging.getLogger(__name__)


class EnrollmentServiceClient:
    """
    Enrollment Service REST 클라이언트
    - 추천 제외에 사용할 전체 수강 이력 강의 ID 목록 조회
    """

    def __init__(
        self,
        base_url: str | None = None,
        internal_api_key: str | None = None,
    ):
        self.base_url = base_url or settings.enrollment_service_url
        self.internal_api_key = internal_api_key or settings.internal_api_key

    async def get_enrollment_history(self, user_id: int) -> EnrollmentHistoryResponse:
        """
        GET /internal/enrollments/history/{userId}
        ENROLLED/LEARNING/COMPLETED 상태의 강의 ID 목록 조회
        """
        url = f"{self.base_url}/internal/enrollments/history/{user_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(
                    url,
                    headers={"X-Internal-Api-Key": self.internal_api_key},
                )
                response.raise_for_status()
                data = response.json()
                return EnrollmentHistoryResponse(**data)
        except httpx.HTTPError as e:
            logger.error(f"[EnrollmentClient] 수강 이력 조회 실패 - userId: {user_id}, error: {e}")
            # 실패 시 빈 이력 반환 (추천 서비스는 비핵심 기능)
            return EnrollmentHistoryResponse(userId=user_id, activeCourseIds=[])


enrollment_client = EnrollmentServiceClient()
