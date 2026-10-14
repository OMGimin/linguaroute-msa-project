import httpx

from app.config.settings import settings


class UserServiceUnavailable(RuntimeError):
    pass


class UserServiceClient:
    def __init__(
        self,
        base_url: str | None = None,
        internal_api_key: str | None = None,
    ):
        self.base_url = base_url or settings.user_service_url
        self.internal_api_key = internal_api_key or settings.internal_api_key

    async def get_authorization_context(self, user_id: int) -> dict:
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                response = await client.get(
                    f"{self.base_url}/internal/users/{user_id}/authorization-context",
                    # [설계 이유 - 권한 컨텍스트를 user-service에서 재조회]
                    # 문제: 초기 코드는 Gateway의 X-User-Role을 곧바로 신뢰했지만,
                    # 실제 사용자 모델에는 Auth 호환 role과 업무용 businessRole이
                    # 함께 있어 어떤 역할인지 모호했고 외부 헤더 위조 위험도 있었다.
                    # 선택: user-service의 내부 API에서 companyId, businessRole,
                    # status를 한 번에 조회하고 X-Internal-Api-Key로 호출자를 제한한다.
                    # 이유: 사용자 상태의 원본 소유자에게 최신 값을 물어보면 비활성화된
                    # 직원이나 변경된 기업 소속을 오래된 토큰 정보만으로 허용하지 않는다.
                    headers={"X-Internal-Api-Key": self.internal_api_key},
                )
                response.raise_for_status()
                payload = response.json()
                context = payload.get("data", payload)
                return {
                    "userId": int(context["userId"]),
                    "companyId": int(context["companyId"]),
                    "businessRole": str(context["businessRole"]),
                    "status": str(context["status"]),
                }
        except (httpx.HTTPError, KeyError, TypeError, ValueError) as error:
            raise UserServiceUnavailable("직원의 최신 권한 정보를 조회할 수 없습니다") from error


user_client = UserServiceClient()
