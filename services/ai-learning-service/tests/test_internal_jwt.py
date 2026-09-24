import base64
import unittest
from datetime import datetime, timedelta, timezone
from uuid import UUID

from jose import jwt

from app.security.internal_jwt import (
    InvalidInternalToken,
    decode_hmac_secret,
    verify_internal_token,
)


ISSUER = "urn:code-base:api-gateway"
SECRET_BYTES = b"a-test-only-secret-that-is-at-least-32-bytes"
SECRET = base64.b64encode(SECRET_BYTES).decode("ascii")


def make_token(**overrides):
    now = datetime.now(timezone.utc)
    claims = {
        "iss": ISSUER,
        "sub": "487e6a20-6cc5-4e09-9f82-2eb5758076f2",
        "exp": now + timedelta(minutes=2),
        "roles": ["CUSTOMER"],
    }
    claims.update(overrides)
    return jwt.encode(claims, SECRET_BYTES, algorithm="HS256")


class InternalJwtTests(unittest.TestCase):
    def test_accepts_valid_gateway_token(self):
        user = verify_internal_token(
            make_token(), encoded_secret=SECRET, issuer=ISSUER
        )

        self.assertEqual(user.user_id, UUID("487e6a20-6cc5-4e09-9f82-2eb5758076f2"))
        self.assertEqual(user.roles, frozenset({"CUSTOMER"}))

    def test_rejects_expired_token(self):
        token = make_token(exp=datetime.now(timezone.utc) - timedelta(seconds=1))

        with self.assertRaises(InvalidInternalToken):
            verify_internal_token(token, encoded_secret=SECRET, issuer=ISSUER)

    def test_rejects_wrong_issuer(self):
        with self.assertRaises(InvalidInternalToken):
            verify_internal_token(make_token(), encoded_secret=SECRET, issuer="wrong")

    def test_rejects_non_uuid_subject(self):
        with self.assertRaises(InvalidInternalToken):
            verify_internal_token(
                make_token(sub="user-123"), encoded_secret=SECRET, issuer=ISSUER
            )

    def test_rejects_missing_or_noncanonical_roles(self):
        for roles in (None, [], ["CUSTOMER", "UNKNOWN"]):
            with self.subTest(roles=roles):
                token = make_token(roles=roles)
                with self.assertRaises(InvalidInternalToken):
                    verify_internal_token(token, encoded_secret=SECRET, issuer=ISSUER)

    def test_rejects_wrong_signing_algorithm(self):
        token = jwt.encode(
            {
                "iss": ISSUER,
                "sub": "487e6a20-6cc5-4e09-9f82-2eb5758076f2",
                "exp": datetime.now(timezone.utc) + timedelta(minutes=2),
                "roles": ["CUSTOMER"],
            },
            SECRET_BYTES,
            algorithm="HS384",
        )

        with self.assertRaises(InvalidInternalToken):
            verify_internal_token(token, encoded_secret=SECRET, issuer=ISSUER)

    def test_secret_requires_base64_and_minimum_length(self):
        with self.assertRaises(ValueError):
            decode_hmac_secret("not-base64!")
        with self.assertRaises(ValueError):
            decode_hmac_secret(base64.b64encode(b"short").decode("ascii"))


if __name__ == "__main__":
    unittest.main()
