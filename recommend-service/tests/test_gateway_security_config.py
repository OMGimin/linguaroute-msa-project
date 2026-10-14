from pathlib import Path


COMPOSE_FILE = Path(__file__).resolve().parents[2] / "docker-compose.yml"


def test_gateway_blocks_internal_api_paths_before_public_routes():
    compose = COMPOSE_FILE.read_text(encoding="utf-8")

    block_id = "SPRING_CLOUD_GATEWAY_ROUTES_1_ID=block-internal-api"
    block_paths = (
        "SPRING_CLOUD_GATEWAY_ROUTES_1_PREDICATES_0="
        "Path=/internal/**,/api/courses/internal/**,/api/enrollments/internal/**,"
        "/api/payments/internal/**"
    )

    assert block_id in compose
    assert block_paths in compose
    assert "SPRING_CLOUD_GATEWAY_ROUTES_1_FILTERS_0=SetStatus=404" in compose
    assert "SPRING_CLOUD_GATEWAY_ROUTES_1_ORDER=-100" in compose
    assert compose.index(block_id) < compose.index(
        "SPRING_CLOUD_GATEWAY_ROUTES_4_ID=course-service"
    )


def test_gateway_keeps_recommendation_public_route_separate():
    compose = COMPOSE_FILE.read_text(encoding="utf-8")

    assert "SPRING_CLOUD_GATEWAY_ROUTES_3_ID=recommend-service" in compose
    assert (
        "Path=/api/courses/recommendations,/api/courses/recommendations/**"
        in compose
    )


def test_internal_api_key_is_injected_into_calling_and_providing_services():
    compose = COMPOSE_FILE.read_text(encoding="utf-8")

    assert compose.count(
        "INTERNAL_API_KEY=${INTERNAL_API_KEY:-local-internal-api-key}"
    ) >= 4

    enrollment_section = compose.split("\n  enrollment-service:", 1)[1].split(
        "\n  payment-service:", 1
    )[0]
    assert "INTERNAL_API_KEY=${INTERNAL_API_KEY:-local-internal-api-key}" in enrollment_section
