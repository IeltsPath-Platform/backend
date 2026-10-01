"""Contract and real-HTTP-path checks for the ordering LLM adapter."""

import asyncio
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import AsyncMock

from app.config import LlmSettings
from app.learning.ordering_llm import LlmProposal, OrderingLlm
from app.llm.client import (
    RETRY_REASONING_EFFORT, LlmApiError, resolve_reasoning_effort, supports_temperature,
)
from app.llm.json_payload import parse_json_payload
from tests.llm_test_support import OpenAiStub, TEST_API_KEY, TEST_MODEL, isolated_llm_env


class OrderingLlmTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.enterContext(isolated_llm_env("http://127.0.0.1:9/"))

    async def test_valid_json_uses_the_completion_contract(self):
        expected = {"modules": [{"id": "module-1", "knowledgePoints": ["kp-2", "kp-1"]}]}
        complete = AsyncMock(return_value=json.dumps(expected))

        result = await OrderingLlm(complete=complete).propose("ordering instructions", "curriculum payload")

        self.assertEqual(result, LlmProposal(payload=expected, reason=None, model=TEST_MODEL))
        complete.assert_awaited_once_with(
            prompt="curriculum payload", system_prompt="ordering instructions",
            response_format={"type": "json_object"}, temperature=0.2,
            max_tokens=8192, reasoning_effort=None,
        )

    async def test_api_and_unexpected_errors_return_safe_failure(self):
        for error in (LlmApiError(500), OSError(f"settings detail {TEST_API_KEY}")):
            with self.subTest(error=type(error).__name__):
                complete = AsyncMock(side_effect=error)
                with self.assertLogs("app.learning.ordering_llm", level="DEBUG") as logs:
                    result = await OrderingLlm(complete=complete).propose("private instructions", "private payload")
                self.assertEqual(result, LlmProposal(payload=None, reason="llm_error", model=TEST_MODEL))
                complete.assert_awaited_once()
                self.assert_safe_warning(logs, "llm_error", type(error).__name__)

    async def test_settings_error_does_not_call_complete(self):
        complete = AsyncMock()

        def broken_settings():
            raise OSError(TEST_API_KEY)

        with self.assertLogs("app.learning.ordering_llm", level="DEBUG") as logs:
            result = await OrderingLlm(complete=complete, settings=broken_settings).propose(
                "private instructions", "private payload")
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

        with self.assertLogs("app.learning.ordering_llm", level="DEBUG") as logs:
            result = await OrderingLlm(timeout_seconds=0.02, complete=slow_complete).propose("system", "payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_timeout", model=TEST_MODEL))
        self.assertTrue(cancelled.is_set())
        self.assert_safe_warning(logs, "llm_timeout", "TimeoutError")

    async def test_reasoning_retry_recovers_a_usable_second_response(self):
        expected = {"modules": [{"id": "module-1"}]}
        complete = AsyncMock(side_effect=["not JSON", json.dumps(expected)])
        result = await OrderingLlm(complete=complete).propose("system", "payload")
        self.assertEqual(result.payload, expected)
        self.assertIsNone(result.reason)
        self.assertEqual(complete.await_count, 2)
        self.assertEqual([call.kwargs["reasoning_effort"] for call in complete.await_args_list],
                         [None, RETRY_REASONING_EFFORT])

    async def test_unusable_response_retries_once_then_returns_safe_failure(self):
        complete = AsyncMock(return_value=f"private payload {TEST_API_KEY}")
        with self.assertLogs("app.learning.ordering_llm", level="DEBUG") as logs:
            result = await OrderingLlm(complete=complete).propose("private instructions", "private payload")
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

        with self.assertLogs("app.learning.ordering_llm", level="WARNING"):
            result = await OrderingLlm(timeout_seconds=0.05, complete=complete).propose("system", "payload")
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
        for private in (TEST_API_KEY, "private payload", "private instructions", "settings detail"):
            self.assertNotIn(private, output)
        self.assertTrue(all(record.exc_info is None for record in logs.records))


class OrderingLlmHttpTest(unittest.IsolatedAsyncioTestCase):
    async def test_real_client_uses_the_openai_json_contract(self):
        expected = {"modules": [{"id": "module-1", "knowledgePoints": ["kp-2", "kp-1"]}]}
        with OpenAiStub(content=json.dumps(expected)) as server, isolated_llm_env(server.base_url):
            result = await OrderingLlm().propose("ordering instructions", "curriculum payload")
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
        # Gemini 2.5 thinks by default; with no configured effort it is switched off and
        # temperature is still sent.
        self.assertEqual(request["body"]["reasoning_effort"], "none")
        self.assertEqual(request["body"]["temperature"], 0.2)

    async def test_http_failure_is_not_retried(self):
        with OpenAiStub(status=500) as server, isolated_llm_env(server.base_url):
            with self.assertLogs("app.learning.ordering_llm", level="WARNING") as logs:
                result = await OrderingLlm().propose("system", "payload")
        self.assertEqual(result.reason, "llm_error")
        self.assertIsNone(result.payload)
        self.assertEqual(len(server.requests), 1)
        output = "\n".join(logs.output)
        self.assertNotIn(TEST_API_KEY, output)
        self.assertNotIn("Synthetic provider failure", output)

    async def test_missing_model_or_key_does_not_contact_provider(self):
        with OpenAiStub() as server, isolated_llm_env(server.base_url, configured=False):
            with self.assertLogs("app.learning.ordering_llm", level="WARNING"):
                result = await OrderingLlm().propose("system", "payload")
        self.assertEqual(result, LlmProposal(payload=None, reason="llm_not_configured", model=None))
        self.assertEqual(server.requests, [])

    async def test_real_unusable_response_makes_only_two_requests(self):
        with OpenAiStub(content="not JSON") as server, isolated_llm_env(server.base_url):
            with self.assertLogs("app.learning.ordering_llm", level="WARNING"):
                result = await OrderingLlm().propose("system", "payload")
        self.assertEqual(result.reason, "llm_unusable_response")
        self.assertEqual([request["body"].get("reasoning_effort") for request in server.requests],
                         ["none", RETRY_REASONING_EFFORT])
        # An explicit effort replaces temperature, as in DeepTutor's provider.
        self.assertEqual(["temperature" in request["body"] for request in server.requests], [True, False])

    async def test_configured_effort_is_sent_and_retry_lowers_it(self):
        with OpenAiStub(content="not JSON") as server, isolated_llm_env(server.base_url):
            settings = LlmSettings(reasoning_effort="high")
            with self.assertLogs("app.learning.ordering_llm", level="WARNING"):
                await OrderingLlm(settings=lambda: settings).propose("system", "payload")
        self.assertEqual([request["body"]["reasoning_effort"] for request in server.requests],
                         ["high", RETRY_REASONING_EFFORT])
        self.assertFalse(any("temperature" in request["body"] for request in server.requests))


class ReasoningEffortTest(unittest.TestCase):
    def test_thinking_models_default_off_and_non_thinking_models_send_nothing(self):
        self.assertEqual(resolve_reasoning_effort("gemini-2.5-flash", None), "none")
        self.assertEqual(resolve_reasoning_effort("gemini-3.8-flash", None), "minimal")
        self.assertEqual(resolve_reasoning_effort("gemini-2.5-pro", "none"), "minimal")
        self.assertEqual(resolve_reasoning_effort("gemini-3.8-flash", "low"), "low")
        self.assertIsNone(resolve_reasoning_effort("gpt-4o-mini", None))

    def test_temperature_is_dropped_for_explicit_effort_and_reasoning_model_families(self):
        self.assertTrue(supports_temperature("gemini-2.5-flash", None))
        self.assertTrue(supports_temperature("gemini-2.5-flash", "none"))
        self.assertFalse(supports_temperature("gemini-3.8-flash", "low"))
        self.assertFalse(supports_temperature("o3-mini", None))
        self.assertFalse(supports_temperature("gpt-5", None))


class JsonPayloadTest(unittest.TestCase):
    def test_extracts_fenced_prefixed_and_embedded_payloads(self):
        self.assertEqual(parse_json_payload('{"modules": []}'), {"modules": []})
        self.assertEqual(parse_json_payload('```json\n{"modules": [1]}\n```'), {"modules": [1]})
        self.assertEqual(parse_json_payload('<think>use {"x": 1}</think>{"modules": [2]}'), {"modules": [2]})
        self.assertEqual(parse_json_payload('Here: {"a": 1} and {"modules": [1, 2, 3]} done'),
                         {"modules": [1, 2, 3]})

    def test_unparseable_or_empty_text_is_none(self):
        self.assertIsNone(parse_json_payload(""))
        self.assertIsNone(parse_json_payload("not JSON"))
        self.assertIsNone(parse_json_payload('{"modules": [1, 2'))


class ConsumerImportIsolationTest(unittest.TestCase):
    def test_consumer_imports_without_llm_settings_and_writes_nothing(self):
        source = """
import os, sys
for name in [key for key in os.environ if key.startswith('AI_LEARNING_LLM_')]:
    del os.environ[name]
import app.messaging.assessment_consumer
loaded = [name for name in sys.modules if name == 'deeptutor' or name.startswith('deeptutor.')]
assert not loaded, loaded
"""
        with tempfile.TemporaryDirectory(prefix="ieltspath-import-test-") as workdir:
            env = dict(os.environ, PYTHONPATH=str(Path(__file__).parents[1]), PYTHONDONTWRITEBYTECODE="1")
            result = subprocess.run([sys.executable, "-c", source], cwd=workdir, env=env,
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(os.listdir(workdir), [])


if __name__ == "__main__":
    unittest.main()
