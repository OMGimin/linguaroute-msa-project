from fastapi import APIRouter, Header, HTTPException, status

from app.client.user_client import UserServiceUnavailable, user_client
from app.model.schemas import RecommendationRequest, RecommendationResponse
from app.service.recommend_service import recommend_service

router = APIRouter(prefix="/api/courses/recommendations", tags=["recommendation"])


@router.post("", response_model=RecommendationResponse)
async def create_recommendation(
    request: RecommendationRequest,
    x_user_id: int = Header(alias="X-User-Id"),
    authorization: str = Header(alias="Authorization"),
):
    try:
        context = await user_client.get_authorization_context(x_user_id)
    except UserServiceUnavailable as error:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(error),
        ) from error

    # [설계 이유 - role이 아니라 businessRole로 인가]
    # 문제: role은 기존 Auth Server와의 호환을 위한 STUDENT/INSTRUCTOR 값이라
    # LinguaRoute의 직원·기업 관리자·플랫폼 관리자를 표현하지 못한다.
    # 선택: 실제 업무 권한인 businessRole과 최신 계정 상태를 함께 검사한다.
    # 결과: EMPLOYEE여도 INACTIVE이면 거부되어 기업에서 비활성화한 직원이
    # 기존 토큰이나 헤더만으로 추천 기능을 계속 사용하는 것을 막는다.
    if context["businessRole"] != "EMPLOYEE" or context["status"] != "ACTIVE":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="활성 상태의 직원만 강의 추천을 요청할 수 있습니다",
        )

    # [현재 제약 - Gateway 인증 경계]
    # Gateway가 토큰을 검증하는 구조라 여기서 토큰을 다시 해석하지는 않는다.
    # 다만 Authorization을 필수로 남겨 보호 API 계약이 코드 변경 중 우연히
    # 사라지는 것을 막는다. 실제 통합 테스트에서는 Gateway가 클라이언트의
    # X-User-Id를 제거하고 인증된 값으로 덮어쓰는지도 별도로 확인해야 한다.
    _ = authorization

    data = await recommend_service.recommend(
        user_id=x_user_id,
        company_id=context["companyId"],
        request=request,
    )
    return RecommendationResponse(data=data)


@router.get("/health", include_in_schema=False)
async def health_check():
    return {"status": "UP", "service": "recommend-service"}
