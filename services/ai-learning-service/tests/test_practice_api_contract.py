"""Public practice response schemas include the stable question identity."""

from datetime import datetime, timezone
from uuid import uuid4

from app.api.dto.practice import PracticeAnswerResponse, PracticeEntryResponse, PracticeReviewResponse


def test_entry_answer_and_review_responses_expose_question_id():
    question_id = str(uuid4())
    base = {
        "entryId": 7,
        "questionId": question_id,
        "sessionId": uuid4(),
        "knowledgePointId": uuid4(),
        "knowledgePointName": "Past tense",
        "prompt": "Complete the sentence.",
        "questionType": "short",
        "options": [],
        "difficulty": "easy",
        "createdAt": datetime.now(timezone.utc),
        "answeredAt": None,
        "userAnswer": "",
        "isCorrect": False,
        "resolved": False,
    }
    entry = PracticeEntryResponse(**base)
    answer = PracticeAnswerResponse(
        entryId=7, questionId=question_id, isCorrect=True, correctAnswer="went", explanation="Past tense."
    )
    review = PracticeReviewResponse(
        entryId=7, questionId=question_id, isCorrect=True, rating="good",
        dueAt=datetime.now(timezone.utc), resolved=False, correctAnswer="went", explanation="Past tense.",
    )

    assert entry.model_dump(by_alias=True)["questionId"] == question_id
    assert answer.model_dump(by_alias=True)["questionId"] == question_id
    assert review.model_dump(by_alias=True)["questionId"] == question_id
