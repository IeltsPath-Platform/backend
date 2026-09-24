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
