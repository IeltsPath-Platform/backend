"""Daily limit settings: their defaults, and the values that must stop the service from starting."""

import base64
import os
import unittest
from unittest.mock import patch

from pydantic import ValidationError

from app.config import Settings

REQUIRED = {
    "internal_jwt_secret": base64.b64encode(b"s" * 32).decode(),
    "database_url": "postgresql://unused",
    "user_service_base_url": "http://user-service",
    "content_service_base_url": "http://content-service",
}
QUOTA_VARIABLES = ("AI_LEARNING_TUTOR_TURNS_PER_DAY", "AI_LEARNING_MEMORY_SUMMARIES_PER_DAY",
                   "AI_LEARNING_QUOTA_TIMEZONE")


class QuotaSettingsTest(unittest.TestCase):
    def settings(self, **values):
        with patch.dict(os.environ):
            for name in QUOTA_VARIABLES:
                os.environ.pop(name, None)
            return Settings(_env_file=None, **REQUIRED, **values)

    def test_defaults_are_fifty_turns_ten_summaries_on_vietnam_days(self):
        settings = self.settings()

        self.assertEqual((settings.tutor_turns_per_day, settings.memory_summaries_per_day, settings.quota_timezone),
                         (50, 10, "Asia/Ho_Chi_Minh"))

    def test_zero_is_accepted_as_unlimited(self):
        settings = self.settings(tutor_turns_per_day=0, memory_summaries_per_day=0)

        self.assertEqual((settings.tutor_turns_per_day, settings.memory_summaries_per_day), (0, 0))

    def test_negative_limits_and_a_blank_zone_are_rejected(self):
        for values in ({"tutor_turns_per_day": -1}, {"memory_summaries_per_day": -1}, {"quota_timezone": " "}):
            with self.subTest(values=values), self.assertRaises(ValidationError):
                self.settings(**values)


if __name__ == "__main__":
    unittest.main()
