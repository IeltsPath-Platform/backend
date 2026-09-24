import unittest
from uuid import UUID, uuid4

from deeptutor.learning.models import ErrorType

from app.adapters.formal_evidence_adapter import ContractError, FormalEvidenceAdapter
from app.learning.formal_provenance import FORMAL_EVIDENCE_NAMESPACE, source_reference_id

from tests.formal_assessment_support import GRAMMAR_KP, VOCABULARY_KP, event, item, mapping


def valid_event(**overrides):
    payload = event(
        user_id=str(uuid4()),
        goal_id=str(uuid4()),
        attempt_id=str(uuid4()),
        items=[item([mapping(VOCABULARY_KP, weight=0.6), mapping(GRAMMAR_KP, weight=0.4)], is_correct=True)],
    )
    payload["data"].update(overrides)
    return payload


class FormalEvidenceAdapterContractTest(unittest.TestCase):
    def test_valid_event_becomes_a_command_with_deterministic_evidence_identities(self):
        payload = valid_event()
        data = payload["data"]

        command = FormalEvidenceAdapter.to_command(payload)

        self.assertEqual(command.learning_goal_id, data["learning_goal_id"])
        self.assertEqual(command.result_version, 1)
        observations = command.items[0].knowledge_points
        self.assertEqual([o.knowledge_point_id for o in observations], [VOCABULARY_KP, GRAMMAR_KP])
        self.assertEqual(
            observations[0].source_reference_id,
            source_reference_id(data["result_id"], 1, data["item_results"][0]["item_result_id"], VOCABULARY_KP),
        )
        self.assertIs(command.items[0].is_correct, True)

    def test_missing_learning_goal_is_rejected_instead_of_using_the_current_goal(self):
        payload = valid_event()
        del payload["data"]["learning_goal_id"]

        with self.assertRaises(ContractError):
            FormalEvidenceAdapter.to_command(payload)

    def test_unsupported_event_version_is_rejected(self):
        payload = valid_event()
        payload["event_type"] = "AssessmentCompleted.v1"

        with self.assertRaises(ContractError):
            FormalEvidenceAdapter.to_command(payload)

    def test_non_final_result_is_rejected(self):
        with self.assertRaises(ContractError):
            FormalEvidenceAdapter.to_command(valid_event(status="DRAFT"))

    def test_malformed_knowledge_point_mappings_are_rejected(self):
        malformed = [
            {"knowledge_point_id": "not-a-uuid", "weight": 1},
            {"knowledge_point_id": VOCABULARY_KP, "weight": -0.5},
            {"knowledge_point_id": VOCABULARY_KP, "weight": "heavy"},
            {"knowledge_point_id": VOCABULARY_KP, "weight": 1, "qualitative_judgment": "EXCELLENT"},
        ]
        for bad_mapping in malformed:
            with self.subTest(bad_mapping=bad_mapping):
                payload = valid_event(item_results=[item([bad_mapping], is_correct=True)])
                with self.assertRaises(ContractError):
                    FormalEvidenceAdapter.to_command(payload)

    def test_the_same_knowledge_point_mapped_twice_on_one_item_is_rejected(self):
        payload = valid_event(item_results=[item([mapping(VOCABULARY_KP), mapping(VOCABULARY_KP)], is_correct=True)])

        with self.assertRaises(ContractError):
            FormalEvidenceAdapter.to_command(payload)

    def test_score_outside_max_score_is_rejected(self):
        payload = valid_event(item_results=[item([mapping(VOCABULARY_KP)], is_correct=True, score=2, max_score=1)])

        with self.assertRaises(ContractError):
            FormalEvidenceAdapter.to_command(payload)

    def test_only_deeptutor_error_categories_are_mapped(self):
        payload = valid_event(item_results=[item(
            [mapping(VOCABULARY_KP, error_type="APPLICATION"), mapping(GRAMMAR_KP, error_type="LEXICAL")],
            is_correct=False,
        )])

        observations = FormalEvidenceAdapter.to_command(payload).items[0].knowledge_points

        self.assertEqual(observations[0].error_type, ErrorType.APPLICATION_ERROR)
        self.assertIsNone(observations[1].error_type)


class SourceReferenceIdentityTest(unittest.TestCase):
    def test_same_result_version_item_and_knowledge_point_give_the_same_uuid5(self):
        result_id, item_result_id = str(uuid4()), str(uuid4())

        first = source_reference_id(result_id, 1, item_result_id, VOCABULARY_KP)
        second = source_reference_id(result_id.upper(), 1, item_result_id, VOCABULARY_KP)

        self.assertEqual(first, second)
        self.assertEqual(UUID(first).version, 5)

    def test_different_result_version_gives_a_different_uuid5(self):
        result_id, item_result_id = str(uuid4()), str(uuid4())

        self.assertNotEqual(
            source_reference_id(result_id, 1, item_result_id, VOCABULARY_KP),
            source_reference_id(result_id, 2, item_result_id, VOCABULARY_KP),
        )

    def test_namespace_is_fixed(self):
        self.assertEqual(str(FORMAL_EVIDENCE_NAMESPACE), "6f0c1b2e-3d7a-4e59-9b8a-2c4d5e6f7a81")


if __name__ == "__main__":
    unittest.main()
