# Adapted from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/tests/test_grading.py @ da856ad.
# Cases and expected values are kept verbatim; only imports changed.
# Omitted: TestGradeAndRecordFailClosed (six cases of LearningService.grade_and_record over the SQLite store; the service does not port that pipeline).
"""Tests for the grading module and the unified post-answer pipeline.

``grade_answer`` is the pure correctness check. ``classify_error`` is the
coarse wrong-answer tagger. ``LearningService.grade_and_record`` folds both of
those through the full record -> mastery -> spaced-repetition pipeline and is
fail-closed: with no stored expected answer the attempt is recorded wrong.
"""

from app.mastery.grading import classify_error, grade_answer
from app.mastery.models import ErrorType


class TestChoiceGrading:
    def test_choice_exact_match(self):
        assert grade_answer("A", "A", "choice") is True

    def test_choice_case_insensitive(self):
        assert grade_answer("b", "B", "choice") is True

    def test_choice_with_spaces(self):
        assert grade_answer("A ", " A", "choice") is True

    def test_choice_wrong(self):
        assert grade_answer("C", "A", "choice") is False


class TestShortGrading:
    def test_short_exact_match(self):
        assert grade_answer("photosynthesis", "photosynthesis", "short") is True

    def test_short_fuzzy_pass(self):
        # "photosynthesi" vs "photosynthesis" — high similarity
        assert grade_answer("photosynthesi", "photosynthesis", "short") is True

    def test_short_fuzzy_fail(self):
        assert grade_answer("completely different", "photosynthesis", "short") is False

    def test_short_long_expected_no_fuzzy(self):
        long_expected = "a" * 31  # >30 chars, no fuzzy
        assert grade_answer(long_expected, long_expected, "short") is True
        assert grade_answer("something else entirely", long_expected, "short") is False


class TestOpenGrading:
    def test_open_keywords_pass(self):
        expected = "cell membrane, nucleus, mitochondria"
        user = "The cell has a cell membrane and nucleus, with mitochondria for energy"
        assert grade_answer(user, expected, "open") is True

    def test_open_keywords_fail(self):
        expected = "cell membrane, nucleus, mitochondria"
        user = "I don't know anything about cells"
        assert grade_answer(user, expected, "open") is False

    def test_open_chinese_separators(self):
        expected = "光合作用；叶绿体；二氧化碳"
        user = "光合作用发生在叶绿体中，需要二氧化碳"
        assert grade_answer(user, expected, "open") is True


class TestEdgeCases:
    def test_empty_expected_returns_false(self):
        assert grade_answer("anything", "", "short") is False
        assert grade_answer("anything", "  ", "short") is False

    def test_empty_user_answer(self):
        assert grade_answer("", "expected", "short") is False

    def test_substring_no_longer_matches(self):
        """Regression: 'expected in user' substring match must not cause false positive."""
        user = "I do not know electromagnetic induction but maybe something else"
        expected = "electromagnetic induction"
        assert grade_answer(user, expected, "short") is False

    def test_unknown_type_returns_false(self):
        assert grade_answer("a", "a", "unknown") is False


class TestClassifyError:
    """Coarse wrong-answer tagging used by the post-answer pipeline.

    Blank means "I didn't know" (metacognitive); anything else is treated as a
    wrong application. The richer taxonomy is assigned later by the LLM.
    """

    def test_blank_answer_is_metacognitive(self):
        assert classify_error("") is ErrorType.METACOGNITIVE

    def test_whitespace_only_answer_is_metacognitive(self):
        assert classify_error("   \n\t  ") is ErrorType.METACOGNITIVE

    def test_nonblank_answer_is_application_error(self):
        assert classify_error("the answer is 42") is ErrorType.APPLICATION_ERROR
