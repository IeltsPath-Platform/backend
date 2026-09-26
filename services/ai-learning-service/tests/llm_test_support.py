"""Isolated LLM settings and a loopback OpenAI-compatible HTTP server for tests."""

from __future__ import annotations

from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import threading
import time
from unittest.mock import patch

TEST_API_KEY = "synthetic-loopback-provider-key"
TEST_MODEL = "gemini-2.5-flash"


@contextmanager
def isolated_llm_env(base_url: str, *, configured: bool = True):
    """Point the LLM settings at a local stub; environment values override any ``.env`` file."""
    values = {
        "AI_LEARNING_LLM_BASE_URL": base_url,
        "AI_LEARNING_LLM_MODEL": TEST_MODEL if configured else "",
        "AI_LEARNING_LLM_API_KEY": TEST_API_KEY if configured else "",
        "AI_LEARNING_LLM_REASONING_EFFORT": "",
    }
    with patch.dict(os.environ, values):
        yield


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
