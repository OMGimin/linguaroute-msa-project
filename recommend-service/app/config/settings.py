# /docker-compose.yml (운영 환경 : 컨테이너 실행용 - 실제 적용값)
# /recommend-service/app/config/setting.py (아무 설정도 없을 경우 이 셋팅으로 동작 - 기본값)
# /recommend-service/.env (개발 환경 : 로컬 직접 실행용)

from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env")

    # 서버 설정
    app_port: int = 8085
    app_name: str = "recommend-service"

    # Eureka 설정
    eureka_server_url: str = "http://localhost:8761/eureka"
    eureka_instance_host: str = "localhost"

    # Auth Server
    jwt_issuer_uri: str = "http://localhost:8080"
    jwk_set_uri: str = "http://auth-server:9000/oauth2/jwks"

    # 서비스 URL
    enrollment_service_url: str = "http://localhost:8083"
    course_service_url: str = "http://localhost:8082"
    user_service_url: str = "http://localhost:8081"
    internal_api_key: str = "local-internal-api-key"
    database_url: str = "mysql+pymysql://manager:SqlDba-1@localhost:3379/lecture_db"

    # OpenAI 키가 없는 개발 환경에서는 로컬 제공자를 유지한다. 키의 존재 여부만으로
    # 제공자를 선택하면 별도 feature flag 없이도 로컬 테스트와 운영 설정을 분리할 수 있다.
    openai_api_key: str | None = None
    openai_model: str = "gpt-5.6-luna"
    openai_timeout_seconds: float = 15.0
    openai_max_retries: int = 1
    openai_max_output_tokens: int = 600

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group_id: str = "recommend-service"
    kafka_topic_enrollment_completed: str = "enrollment.completed"

settings = Settings()
