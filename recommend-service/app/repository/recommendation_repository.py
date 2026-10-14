import asyncio
from typing import Protocol

from app.config.database import SessionLocal
from app.model.entities import Recommendation, RecommendationItem

from app.model.schemas import (
    RecommendedCourse,
    RecommendationRequest,
    RecommendationSource,
    RecommendationStatus,
)


class RecommendationRepository(Protocol):
    async def save(
        self,
        *,
        user_id: int,
        company_id: int,
        request: RecommendationRequest,
        source: RecommendationSource,
        status: RecommendationStatus,
        courses: list[RecommendedCourse],
    ) -> int: ...


class InMemoryRecommendationRepository:
    """DB 연결 전에도 통합 흐름을 검증할 수 있는 개발용 저장소."""

    def __init__(self):
        self._next_id = 1
        self.records: list[dict] = []

    async def save(self, **record) -> int:
        recommendation_id = self._next_id
        self._next_id += 1
        self.records.append({"id": recommendation_id, **record})
        return recommendation_id


class SqlAlchemyRecommendationRepository:
    async def save(self, **record) -> int:
        return await asyncio.to_thread(self._save, record)

    @staticmethod
    def _save(record: dict) -> int:
        request = record["request"]
        recommendation = Recommendation(
            user_id=record["user_id"],
            company_id=record["company_id"],
            language=request.language,
            level=request.level,
            job=request.job,
            situation=request.situation,
            goal=request.goal,
            source=record["source"].value,
            status=record["status"].value,
            items=[
                RecommendationItem(
                    course_id=course.courseId,
                    rank=index,
                    reason=course.reason,
                )
                for index, course in enumerate(record["courses"], start=1)
            ],
        )
        with SessionLocal.begin() as session:
            session.add(recommendation)
            session.flush()
            return recommendation.id
