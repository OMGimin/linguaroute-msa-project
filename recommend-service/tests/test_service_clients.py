from unittest.mock import Mock

import pytest

from app.client.course_client import CourseServiceClient
from app.client.enrollment_client import EnrollmentServiceClient
from app.client.user_client import UserServiceClient


class FakeResponse:
    def __init__(self, payload):
        self.payload = payload

    def raise_for_status(self):
        return None

    def json(self):
        return self.payload


class FakeAsyncClient:
    def __init__(self, response, request_spy, **_kwargs):
        self.response = response
        self.request_spy = request_spy

    async def __aenter__(self):
        return self

    async def __aexit__(self, *_args):
        return None

    async def get(self, url, **kwargs):
        self.request_spy(url, **kwargs)
        return self.response


@pytest.mark.asyncio
async def test_course_client_uses_internal_recommendation_contract(monkeypatch):
    request_spy = Mock()
    response = FakeResponse(
        [
            {
                "id": 12,
                "title": "해외 고객 미팅 영어",
                "language": "ENGLISH",
                "level": "INTERMEDIATE",
                "situation": "CUSTOMER_MEETING",
                "status": "ACTIVE",
            }
        ]
    )
    monkeypatch.setattr(
        "app.client.course_client.httpx.AsyncClient",
        lambda **kwargs: FakeAsyncClient(response, request_spy, **kwargs),
    )

    courses = await CourseServiceClient(
        "http://course-service:8082", internal_api_key="test-internal-key"
    ).get_candidates("ENGLISH", exclude_ids=[3, 5])

    assert courses[0].courseId == 12
    request_spy.assert_called_once_with(
        "http://course-service:8082/internal/courses/recommend",
        params={"language": "ENGLISH", "excludeIds": [3, 5]},
        headers={"X-Internal-Api-Key": "test-internal-key"},
    )


@pytest.mark.asyncio
async def test_enrollment_client_uses_secured_internal_history_contract(monkeypatch):
    request_spy = Mock()
    response = FakeResponse({"userId": 7, "activeCourseIds": [3, 5]})
    monkeypatch.setattr(
        "app.client.enrollment_client.httpx.AsyncClient",
        lambda **kwargs: FakeAsyncClient(response, request_spy, **kwargs),
    )

    history = await EnrollmentServiceClient(
        "http://enrollment-service:8083", internal_api_key="test-internal-key"
    ).get_enrollment_history(7)

    assert history.activeCourseIds == [3, 5]
    request_spy.assert_called_once_with(
        "http://enrollment-service:8083/internal/enrollments/history/7",
        headers={"X-Internal-Api-Key": "test-internal-key"},
    )


@pytest.mark.asyncio
async def test_user_client_reads_business_authorization_context(monkeypatch):
    request_spy = Mock()
    response = FakeResponse(
        {
            "data": {
                "userId": 7,
                "companyId": 10,
                "businessRole": "EMPLOYEE",
                "status": "ACTIVE",
            }
        }
    )
    monkeypatch.setattr(
        "app.client.user_client.httpx.AsyncClient",
        lambda **kwargs: FakeAsyncClient(response, request_spy, **kwargs),
    )

    context = await UserServiceClient(
        "http://user-service:8081", internal_api_key="test-internal-key"
    ).get_authorization_context(7)

    assert context["businessRole"] == "EMPLOYEE"
    assert context["companyId"] == 10
    request_spy.assert_called_once_with(
        "http://user-service:8081/internal/users/7/authorization-context",
        headers={"X-Internal-Api-Key": "test-internal-key"},
    )
