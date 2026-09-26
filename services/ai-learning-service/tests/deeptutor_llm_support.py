"""Isolated DeepTutor catalogs and a loopback OpenAI-compatible HTTP server."""

from __future__ import annotations

import atexit
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import tempfile
import threading
import time
from unittest.mock import patch

TEST_API_KEY = "synthetic-loopback-provider-key"
TEST_MODEL = "gemini-2.5-flash"
_suite_home: tempfile.TemporaryDirectory | None = None


def isolate_deeptutor_home() -> None:
    """Run before test collection imports DeepTutor and creates runtime files."""
    global _suite_home
    if _suite_home is None:
        _suite_home = tempfile.TemporaryDirectory(prefix="ieltspath-deeptutor-tests-")
        os.environ["DEEPTUTOR_HOME"] = _suite_home.name
        atexit.register(_suite_home.cleanup)


def reset_deeptutor_caches() -> None:
    from deeptutor.services.config.model_catalog import ModelCatalogService
    from deeptutor.services.llm.config import clear_llm_config_cache
    from deeptutor.services.path_service import PathService

    clear_llm_config_cache()
    ModelCatalogService._instances.clear()
    PathService.reset_instance()


@contextmanager
def isolated_llm_catalog(base_url: str, *, configured: bool = True):
    """Select Gemini using only a disposable catalog pointing to the stub."""
    with tempfile.TemporaryDirectory(prefix="ieltspath-llm-catalog-") as home:
        with patch.dict(os.environ, {"DEEPTUTOR_HOME": home}):
            path = Path(home) / "data/user/settings/model_catalog.json"
            path.parent.mkdir(parents=True)
            service = {"active_profile_id": None, "active_model_id": None, "profiles": []}
            if configured:
                service = {
                    "active_profile_id": "test-gemini-profile",
                    "active_model_id": "test-gemini-model",
                    "profiles": [{
                        "id": "test-gemini-profile", "name": "Test Gemini", "binding": "gemini",
                        "base_url": base_url, "api_key": TEST_API_KEY, "api_version": "",
                        "extra_headers": {},
                        "models": [{"id": "test-gemini-model", "name": "Test model", "model": TEST_MODEL}],
                    }],
                }
            path.write_text(json.dumps({"version": 1, "services": {"llm": service}}), encoding="utf-8")
            reset_deeptutor_caches()
            try:
                yield path
            finally:
                reset_deeptutor_caches()


class OpenAiStub:
    """Record real HTTP calls without ever contacting an external provider."""

    def __init__(self, *, content='{"modules": [{"id": "module-1"}]}', status=200,
                 delay_seconds=0.0, on_request=None):
        self.content = content
        self.status = status
        self.delay_seconds = delay_seconds
        self.requests: list[dict] = []
        owner = self

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                request = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                owner.requests.append({"path": self.path,
                                       "headers": {key.lower(): value for key, value in self.headers.items()},
                                       "body": request})
                if on_request is not None:
                    on_request(request)
                if owner.delay_seconds:
                    time.sleep(owner.delay_seconds)
                if owner.status == 200:
                    body = {"id": "loopback-completion", "object": "chat.completion", "created": 0,
                            "model": TEST_MODEL,
                            "choices": [{"index": 0, "message": {"role": "assistant", "content": owner.content},
                                         "finish_reason": "stop"}],
                            "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}}
                else:
                    body = {"error": {"message": "Synthetic provider failure", "type": "server_error"}}
                data = json.dumps(body).encode()
                try:
                    self.send_response(owner.status)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Content-Length", str(len(data)))
                    self.end_headers()
                    self.wfile.write(data)
                except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                    pass  # A timed-out caller has already closed its connection.

            def log_message(self, format, *args):
                pass

        self._server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self._thread = threading.Thread(target=self._server.serve_forever, kwargs={"poll_interval": 0.01}, daemon=True)
        self.base_url = f"http://127.0.0.1:{self._server.server_port}/"

    def __enter__(self):
        self._thread.start()
        return self

    def __exit__(self, *exc):
        self._server.shutdown()
        self._server.server_close()
        self._thread.join(timeout=2)
