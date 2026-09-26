"""Contract and real-provider-path checks for the DeepTutor ordering adapter."""

import asyncio
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import AsyncMock, patch

from app.learning.deeptutor_llm import DeepTutorOrderingLlm, LlmProposal
from tests.deeptutor_llm_support import OpenAiStub, TEST_API_KEY, TEST_MODEL, isolated_llm_catalog


class DeepTutorOrderingLlmTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.enterContext(isolated_llm_catalog("http://127.0.0.1:9/"))

    async def test_valid_json_uses_deeptutor_call_contract(self):
        expected = {"modules": [{"id": "module-1", "knowledgePoints": ["kp-2", "kp-1"]}]}
        complete = AsyncMock(return_value=json.dumps(expected))

        result = await DeepTutorOrderingLlm(complete=complete).propose("ordering instructions", "curriculum payload")

        self.assertEqual(result, LlmProposal(payload=expected, reason=None, model=TEST_MODEL))
        complete.assert_awaited_once_with(
            prompt="curriculum payload", system_prompt="ordering instructions",
            response_format={"type": "json_object"}, temperature=0.2,
            max_tokens=8192, max_retries=0, reasoning_effort=None,
        )

    async def test_api_and_unexpected_errors_return_safe_failure(self):
        from deeptutor.services.llm.exceptions import LLMAPIError

        for error in (LLMAPIError(f"provider detail {TEST_API_KEY}", status_code=500),
                      OSError(f"catalog detail {TEST_API_KEY}")):
            with self.subTest(error=type(error).__name__):
                complete = AsyncMock(side_effect=error)
                with self.assertLogs("app.learning.deeptutor_llm", level="DEBUG") as logs:
                    result = await DeepTutorOrderingLlm(complete=complete).propose("private instructions", "private payload")
                self.assertEqual(result, LlmProposal(payload=None, reason="llm_error", model=TEST_MODEL))
                complete.assert_awaited_once()
                self.assert_safe_warning(logs, "llm_error", type(error).__name__)

    async def test_catalog_io_error_does_not_call_complete(self):
        complete = AsyncMock()
        with patch("deeptutor.services.llm.config.get_llm_config", side_effect=OSError(TEST_API_KEY)):
            with self.assertLogs("app.learning.deeptutor_llm", level="DEBUG") as logs:
                result = await DeepTutorOrderingLlm(complete=complete).propose("private instructions", "private payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_error", model=None))
        complete.assert_not_awaited()
        self.assert_safe_warning(logs, "llm_error", "OSError", configured=False)

    async def test_timeout_cancels_the_in_flight_call(self):
        cancelled = asyncio.Event()

        async def slow_complete(**kwargs):
            try:
                await asyncio.sleep(10)
            finally:
                cancelled.set()

        with self.assertLogs("app.learning.deeptutor_llm", level="DEBUG") as logs:
            result = await DeepTutorOrderingLlm(timeout_seconds=0.02, complete=slow_complete).propose("system", "payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_timeout", model=TEST_MODEL))
        self.assertTrue(cancelled.is_set())
        self.assert_safe_warning(logs, "llm_timeout", "TimeoutError")

    async def test_reasoning_retry_recovers_a_usable_second_response(self):
        from deeptutor.services.llm.reasoning_params import RETRY_REASONING_EFFORT

        expected = {"modules": [{"id": "module-1"}]}
        complete = AsyncMock(side_effect=["not JSON", json.dumps(expected)])
        result = await DeepTutorOrderingLlm(complete=complete).propose("system", "payload")
        self.assertEqual(result.payload, expected)
        self.assertIsNone(result.reason)
        self.assertEqual(complete.await_count, 2)
        self.assertEqual([call.kwargs["reasoning_effort"] for call in complete.await_args_list],
                         [None, RETRY_REASONING_EFFORT])

    async def test_unusable_response_retries_once_then_returns_safe_failure(self):
        complete = AsyncMock(return_value=f"private payload {TEST_API_KEY}")
        with self.assertLogs("app.learning.deeptutor_llm", level="DEBUG") as logs:
            result = await DeepTutorOrderingLlm(complete=complete).propose("private instructions", "private payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_unusable_response", model=TEST_MODEL))
        self.assertEqual(complete.await_count, 2)
        self.assert_safe_warning(logs, "llm_unusable_response")

    async def test_timeout_budget_covers_both_reasoning_attempts(self):
        calls = 0

        async def complete(**kwargs):
            nonlocal calls
            calls += 1
            if calls == 1:
                await asyncio.sleep(0.03)
                return "not JSON"
            await asyncio.sleep(0.03)
            return '{"modules": [{"id": "module-1"}]}'

        with self.assertLogs("app.learning.deeptutor_llm", level="WARNING"):
            result = await DeepTutorOrderingLlm(timeout_seconds=0.05, complete=complete).propose("system", "payload")
        self.assertEqual(result.reason, "llm_timeout")
        self.assertEqual(calls, 2)

    def assert_safe_warning(self, logs, reason, error_type=None, *, configured=True):
        warnings = [record for record in logs.records if record.levelname == "WARNING"]
        self.assertEqual(len(warnings), 1)
        output = "\n".join(logs.output)
        self.assertIn(reason, output)
        if configured:
            self.assertIn(TEST_MODEL, output)
        if error_type:
            self.assertIn(error_type, output)
        self.assertRegex(output, r"(?:elapsed|duration|time)[^\d]*\d")
        for private in (TEST_API_KEY, "private payload", "private instructions", "provider detail", "catalog detail"):
            self.assertNotIn(private, output)
        self.assertTrue(all(record.exc_info is None for record in logs.records))


class DeepTutorOrderingLlmIntegrationTest(unittest.IsolatedAsyncioTestCase):
    async def test_real_deeptutor_uses_gemini_catalog_and_openai_json_contract(self):
        expected = {"modules": [{"id": "module-1", "knowledgePoints": ["kp-2", "kp-1"]}]}
        with OpenAiStub(content=json.dumps(expected)) as server, isolated_llm_catalog(server.base_url):
            result = await DeepTutorOrderingLlm().propose("ordering instructions", "curriculum payload")
        self.assertEqual(result, LlmProposal(payload=expected, reason=None, model=TEST_MODEL))
        self.assertEqual(len(server.requests), 1)
        request = server.requests[0]
        self.assertEqual(request["path"], "/chat/completions")
        self.assertEqual(request["headers"].get("authorization"), f"Bearer {TEST_API_KEY}")
        self.assertEqual(request["body"]["model"], TEST_MODEL)
        self.assertEqual(request["body"]["response_format"], {"type": "json_object"})
        self.assertEqual(request["body"]["messages"], [
            {"role": "system", "content": "ordering instructions"},
            {"role": "user", "content": "curriculum payload"},
        ])

    async def test_http_failure_is_not_retried(self):
        with OpenAiStub(status=500) as server, isolated_llm_catalog(server.base_url):
            with self.assertLogs("app.learning.deeptutor_llm", level="WARNING") as logs:
                result = await DeepTutorOrderingLlm().propose("system", "payload")
        self.assertEqual(result.reason, "llm_error")
        self.assertIsNone(result.payload)
        self.assertEqual(len(server.requests), 1)
        self.assertNotIn(TEST_API_KEY, "\n".join(logs.output))

    async def test_missing_active_model_does_not_contact_provider(self):
        with OpenAiStub() as server, isolated_llm_catalog(server.base_url, configured=False):
            with self.assertLogs("app.learning.deeptutor_llm", level="WARNING"):
                result = await DeepTutorOrderingLlm().propose("system", "payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_not_configured", model=None))
        self.assertEqual(server.requests, [])

    async def test_real_unusable_response_makes_only_two_requests(self):
        with OpenAiStub(content="not JSON") as server, isolated_llm_catalog(server.base_url):
            with self.assertLogs("app.learning.deeptutor_llm", level="WARNING"):
                result = await DeepTutorOrderingLlm().propose("system", "payload")
        self.assertEqual(result.reason, "llm_unusable_response")
        self.assertEqual(len(server.requests), 2)


class DeepTutorImportIsolationTest(unittest.TestCase):
    def test_adapter_and_consumer_import_without_loading_deeptutor_llm(self):
        source = """
import importlib.abc
import sys

class RejectLlmImport(importlib.abc.MetaPathFinder):
    def find_spec(self, fullname, path=None, target=None):
        if fullname == 'deeptutor.services.llm' or fullname.startswith('deeptutor.services.llm.'):
            raise AssertionError('consumer or adapter eagerly imported the LLM layer')

sys.meta_path.insert(0, RejectLlmImport())
import app.learning.deeptutor_llm
import app.messaging.assessment_consumer
assert not any(name.startswith('deeptutor.services.llm') for name in sys.modules)
"""
        with tempfile.TemporaryDirectory(prefix="ieltspath-import-test-") as home:
            env = dict(os.environ, DEEPTUTOR_HOME=home, PYTHONDONTWRITEBYTECODE="1")
            result = subprocess.run([sys.executable, "-c", source], cwd=Path(__file__).parents[1],
                                    env=env, capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertFalse((Path(home) / "data/user/settings/model_catalog.json").exists())
            self.assertFalse((Path(home) / "data/user/usage.sqlite3").exists())
