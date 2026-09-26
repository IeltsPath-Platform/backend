"""Runtime settings shared by the AI Learning service."""

import base64
import binascii
from functools import lru_cache

from pydantic import SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

DEFAULT_INTERNAL_JWT_ISSUER = "urn:code-base:api-gateway"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="AI_LEARNING_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    internal_jwt_secret: SecretStr
    internal_jwt_issuer: str = DEFAULT_INTERNAL_JWT_ISSUER
    database_url: SecretStr
    user_service_base_url: str
    content_service_base_url: str

    @field_validator("internal_jwt_issuer", "user_service_base_url", "content_service_base_url")
    @classmethod
    def reject_blank_values(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("Configuration value must not be blank")
        return value

    @field_validator("user_service_base_url", "content_service_base_url")
    @classmethod
    def normalize_service_base_urls(cls, value: str) -> str:
        return value.rstrip("/")

    @field_validator("internal_jwt_secret")
    @classmethod
    def validate_internal_jwt_secret(cls, value: SecretStr) -> SecretStr:
        try:
            secret = base64.b64decode(value.get_secret_value(), validate=True)
        except (binascii.Error, ValueError) as exc:
            raise ValueError("Internal JWT secret must be valid Base64") from exc
        if len(secret) < 32:
            raise ValueError("Internal JWT secret must decode to at least 32 bytes")
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()


DEFAULT_LLM_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"


class LlmSettings(BaseSettings):
    """OpenAI-compatible chat completions provider used for path ordering.

    Read per call so a changed key or model takes effect without a restart. Without a
    model and an API key the service keeps its deterministic ordering.
    """

    model_config = SettingsConfigDict(
        env_prefix="AI_LEARNING_LLM_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    base_url: str = DEFAULT_LLM_BASE_URL
    model: str = ""
    api_key: SecretStr | None = None
    # Empty uses the model family's default (thinking off for Gemini 2.5 and 3).
    reasoning_effort: str = ""
    timeout_seconds: float = 20.0

    @property
    def configured(self) -> bool:
        return bool(self.model.strip() and self.api_key and self.api_key.get_secret_value().strip())

    @field_validator("timeout_seconds")
    @classmethod
    def require_positive_timeout(cls, value: float) -> float:
        if value <= 0:
            raise ValueError("Configuration value must be positive")
        return value


class ConsumerSettings(BaseSettings):
    """Settings for the AssessmentCompleted.v2 consumer process."""

    model_config = SettingsConfigDict(
        env_prefix="AI_LEARNING_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: SecretStr
    amqp_url: SecretStr
    assessment_exchange: str = "assessment.events"
    # Transient failures wait this long in the retry queue before redelivery.
    retry_delay_ms: int = 30_000
    # After this many failed deliveries the message is parked in the dead-letter queue.
    max_delivery_attempts: int = 5

    @field_validator("assessment_exchange")
    @classmethod
    def reject_blank_exchange(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("Configuration value must not be blank")
        return value

    @field_validator("retry_delay_ms", "max_delivery_attempts")
    @classmethod
    def require_positive(cls, value: int) -> int:
        if value < 1:
            raise ValueError("Configuration value must be positive")
        return value
