"""Pure learner-memory prompt and sanitizing behavior."""

from app.tutor.memory import build_prompt, sanitize_memory


def test_sanitize_memory_truncates_and_removes_contact_or_long_number_lines():
    sanitized = sanitize_memory(
        "Keeps examples first.\nContact learner@example.com for notes.\n"
        "Phone 090-123 4567.\nStrong at band 6.5; studies 20 minutes."
    )

    assert sanitized == "Keeps examples first.\nStrong at band 6.5; studies 20 minutes."
    assert len(sanitize_memory("x" * 2100)) == 2000


def test_build_prompt_keeps_memory_and_messages_in_tagged_oldest_first_sections():
    messages = [
        (10, "user", "first learner message"),
        (11, "assistant", "first tutor reply"),
        (12, "user", "x" * 1100),
    ]

    prompt = build_prompt("Uses examples first.", messages)

    assert "<memory>\nUses examples first.\n</memory>" in prompt
    assert "<messages>\n[learner] first learner message\n[tutor] first tutor reply" in prompt
    assert prompt.index("first learner message") < prompt.index("first tutor reply")
    assert "x" * 1001 not in prompt
    assert "x" * 1000 in prompt
