"""Opt-in, internal-only OpenAI-compatible ordering stub. Never calls a provider."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import time


class OrderingStubHandler(BaseHTTPRequestHandler):
    def log_message(self, _format, *args):
        # Do not print requests, authorization or prompt data.
        pass

    def do_POST(self):
        if self.path != "/v1beta/openai/chat/completions":
            self.send_error(404)
            return
        mode = os.environ.get("LLM_STUB_MODE", "reverse")
        if mode == "slow":
            time.sleep(30)
        if mode == "error":
            self._json(500, {"error": {"message": "Synthetic provider failure", "type": "server_error"}})
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 1_000_000:
                self.send_error(413)
                return
            body = json.loads(self.rfile.read(size))
            learner_payload = json.loads(next(message["content"] for message in body["messages"]
                                              if message["role"] == "user"))
            modules = [
                {"id": module["id"],
                 "knowledge_point_ids": [point["id"] for point in reversed(module["knowledge_points"])]}
                for module in reversed(learner_payload["modules"])
            ]
            if mode == "unknown_kp":
                modules[0]["knowledge_point_ids"].append("unknown-stub-knowledge-point")
            proposal = {"modules": modules, "rationale": "Synthetic reversed curriculum for verification"}
            self._json(200, {
                "id": "stub-completion", "object": "chat.completion", "created": int(time.time()),
                "model": body.get("model", "stub-model"),
                "choices": [{"index": 0, "message": {"role": "assistant", "content": json.dumps(proposal)},
                             "finish_reason": "stop"}],
                "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
            })
        except (KeyError, TypeError, ValueError, StopIteration, IndexError):
            self._json(400, {"error": {"message": "Invalid synthetic test request", "type": "invalid_request"}})

    def _json(self, status, payload):
        encoded = json.dumps(payload).encode("utf-8")
        try:
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)
        except (BrokenPipeError, ConnectionResetError):
            # The slow case intentionally outlives the client's timeout.
            pass


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", int(os.environ.get("LLM_STUB_PORT", "8090"))),
                        OrderingStubHandler).serve_forever()
