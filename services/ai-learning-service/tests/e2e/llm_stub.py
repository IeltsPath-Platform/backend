"""Opt-in, internal-only OpenAI-compatible stub for path ordering and the tutor. Never calls a provider.

Modes (LLM_STUB_MODE): reverse, unknown_kp, slow, error for path ordering; tutor answers tutor requests
(those carrying ``tools``) from the status the API sends, and orders paths like ``reverse``.
"""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
from threading import Lock
import time

_SCRIPT_LOCK = Lock()
_SCRIPT_INDEX = 0
_SCRIPT_REQUESTS = []


class OrderingStubHandler(BaseHTTPRequestHandler):
    def log_message(self, _format, *args):
        # Do not print requests, authorization or prompt data.
        pass

    def do_GET(self):
        if self.path != "/stub/requests" or os.environ.get("LLM_STUB_MODE") != "script":
            self.send_error(404)
            return
        with _SCRIPT_LOCK:
            requests = list(_SCRIPT_REQUESTS)
        self._json(200, {"requests": requests})

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
            if mode == "script" and body.get("tools"):
                try:
                    reply, delay = scripted_tutor_reply(body)
                except (KeyError, TypeError, ValueError, OSError):
                    self._json(400, {"error": {"message": "Invalid stub script", "type": "invalid_request"}})
                    return
                time.sleep(delay)
                self._reply(body, reply)
                return
            if mode == "tutor" and body.get("tools"):
                time.sleep(float(os.environ.get("LLM_STUB_TUTOR_DELAY", "0")))
                self._reply(body, tutor_reply(body["messages"]))
                return
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

    def _reply(self, body, message):
        finish = "tool_calls" if message.get("tool_calls") else "stop"
        self._json(200, {
            "id": "stub-completion", "object": "chat.completion", "created": int(time.time()),
            "model": body.get("model", "stub-model"),
            "choices": [{"index": 0, "message": {"role": "assistant", **message}, "finish_reason": finish}],
            "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
        })

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


def _tool_call(name, arguments):
    return {"id": f"stub-{name}-{int(time.time() * 1000)}", "type": "function",
            "function": {"name": name, "arguments": json.dumps(arguments)}}


def _current_status(messages):
    context = next(message["content"] for message in messages if message.get("role") == "system"
                   and str(message.get("content", "")).startswith("Current status"))
    return json.loads(context.split("\n", 1)[1])


def scripted_tutor_reply(body):
    """Read a test script and record only request shape, never conversation text or tokens."""
    global _SCRIPT_INDEX
    source = os.environ["LLM_STUB_SCRIPT"]
    script = json.loads(source if source.lstrip().startswith("[") else Path(source).read_text(encoding="utf-8"))
    if not isinstance(script, list) or not script:
        raise ValueError("The stub script must be a non-empty list")
    with _SCRIPT_LOCK:
        step = script[_SCRIPT_INDEX % len(script)]
        _SCRIPT_INDEX += 1
        _SCRIPT_REQUESTS.append({"roles": [message.get("role") for message in body["messages"]],
                                 "tools": [tool["function"]["name"] for tool in body["tools"]]})
    if not isinstance(step, dict):
        raise ValueError("Each script step must be an object")
    status = _current_status(body["messages"])
    substitutions = {
        "${knowledge_point_id}": (status.get("objective") or {}).get("knowledge_point_id", ""),
        "${question_id}": (status.get("pending_interaction") or {}).get("question_id", ""),
    }
    encoded = json.dumps(step)
    for placeholder, value in substitutions.items():
        encoded = encoded.replace(placeholder, str(value))
    step = json.loads(encoded)
    delay = float(step.get("delay_seconds", 0))
    if not 0 <= delay <= 60:
        raise ValueError("delay_seconds must be from 0 to 60")
    calls = [_tool_call(call["name"], call.get("arguments", {})) for call in step.get("tool_calls", [])]
    return {"content": step.get("content"), **({"tool_calls": calls} if calls else {})}, delay


def tutor_reply(messages):
    """A deterministic tutor: grade an answered card, quiz memory/procedure, pass concept/design."""
    if messages and messages[-1].get("role") == "tool":
        return {"content": "Stub tutor: noted. Let's keep going."}
    status = _current_status(messages)
    pending = status.get("pending_interaction") or {}
    objective = status.get("objective") or {}
    if pending.get("status") == "answered":
        return {"content": None, "tool_calls": [_tool_call("mastery_grade", {"answer": pending["learner_answer"]})]}
    if pending:
        return {"content": "Stub tutor: answer the open question on your card when you are ready."}
    kind = objective.get("knowledge_point_type")
    if objective.get("action") in ("probe", "practice", "review") and kind in ("memory", "procedure"):
        return {"content": "Stub tutor: let's check this objective.", "tool_calls": [_tool_call("mastery_quiz", {
            "knowledge_point_id": objective["knowledge_point_id"], "question": "Which option is correct?",
            "question_type": "choice", "expected_answer": "B", "explanation": "Stub explanation.",
            "options": [{"label": "A", "body": "Wrong option"}, {"label": "B", "body": "Right option"},
                        {"label": "C", "body": "Another wrong option"}]})]}
    if objective.get("action") in ("probe", "assess") and kind in ("concept", "design"):
        return {"content": None, "tool_calls": [_tool_call("mastery_assess", {
            "knowledge_point_id": objective["knowledge_point_id"], "passed": True, "feedback": "Stub pass."})]}
    return {"content": "Stub tutor: everything here is mastered."}


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", int(os.environ.get("LLM_STUB_PORT", "8090"))),
                        OrderingStubHandler).serve_forever()
