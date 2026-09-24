"""Validation for the short-lived internal JWT issued by API Gateway."""

from __future__ import annotations

import base64
import binascii
from dataclasses import dataclass
from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from app.config import Settings, get_settings

CANONICAL_ROLES = frozenset(
    {"ADMIN", "CUSTOMER", "CONTENT_AUTHOR", "EXAMINER", "SALES_STAFF"}
)
bearer_scheme = HTTPBearer(auto_error=False)


class InvalidInternalToken(ValueError):
    """The token is invalid under the Gateway internal-token contract."""


@dataclass(frozen=True, slots=True)
class AuthenticatedUser:
    user_id: UUID
    roles: frozenset[str]


def decode_hmac_secret(encoded_secret: str) -> bytes:
    """Decode a configured Base64 HMAC secret and enforce the shared minimum."""
    try:
        secret = base64.b64decode(encoded_secret, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise ValueError("Internal JWT secret must be valid Base64") from exc

    if len(secret) < 32:
        raise ValueError("Internal JWT secret must decode to at least 32 bytes")
    return secret


def verify_internal_token(
    token: str,
    *,
    encoded_secret: str,
    issuer: str,
) -> AuthenticatedUser:
    """Verify an internal HS256 token and extract its UUID subject and roles."""
    secret = decode_hmac_secret(encoded_secret)
    try:
        claims = jwt.decode(
            token,
            secret,
            algorithms=["HS256"],
            issuer=issuer,
            options={
                "verify_aud": False,
                "require_exp": True,
                "require_sub": True,
                "require_iss": True,
            },
        )
    except JWTError as exc:
        raise InvalidInternalToken("Invalid internal bearer token") from exc

    try:
        user_id = UUID(claims["sub"])
    except (KeyError, TypeError, ValueError, AttributeError) as exc:
        raise InvalidInternalToken("Internal token subject must be a UUID") from exc

    raw_roles = claims.get("roles")
    if isinstance(raw_roles, str):
        roles = {raw_roles}
    elif isinstance(raw_roles, list) and all(isinstance(role, str) for role in raw_roles):
        roles = set(raw_roles)
    else:
        roles = set()

    if not roles or not roles.issubset(CANONICAL_ROLES):
        raise InvalidInternalToken("Internal token must contain canonical roles")

    return AuthenticatedUser(user_id=user_id, roles=frozenset(roles))


async def require_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    settings: Settings = Depends(get_settings),
) -> AuthenticatedUser:
    """FastAPI dependency that authenticates Authorization only, never identity headers."""
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Bearer token required",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        return verify_internal_token(
            credentials.credentials,
            encoded_secret=settings.internal_jwt_secret.get_secret_value(),
            issuer=settings.internal_jwt_issuer,
        )
    except InvalidInternalToken as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc
