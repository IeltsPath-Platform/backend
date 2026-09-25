"""Goal-scoped curriculum: which canonical knowledge points belong in a learner's path."""

import unittest
from decimal import Decimal

from app.adapters.curriculum_scope import CurriculumScope, KnowledgePointBand, NoCurriculumInScope, target_band_of

BASIC, COMPLEX, ACADEMIC, EMPTY = (
    "4ed3d7e1-7529-4572-921d-2e54403f7d01",
    "4ed3d7e1-7529-4572-921d-2e54403f7d02",
    "4ed3d7e1-7529-4572-921d-2e54403f7d03",
    "4ed3d7e1-7529-4572-921d-2e54403f7d04",
)


def topic(topic_id, order):
    return {"id": topic_id, "name": f"Topic {order}", "sortOrder": order, "status": "ACTIVE"}


def point(point_id, topic_id, band_min=None, band_max=None):
    return {"id": point_id, "topicId": topic_id, "name": point_id[-4:], "learningType": "PROCEDURE",
            "status": "ACTIVE", "effectiveBandMin": band_min, "effectiveBandMax": band_max}


TOPICS = [topic(BASIC, 0), topic(COMPLEX, 1), topic(ACADEMIC, 2), topic(EMPTY, 3)]
POINTS = [
    point("c5b2641f-28c8-467d-9d64-f52c8bdc1501", BASIC, 4.0, 5.0),
    point("c5b2641f-28c8-467d-9d64-f52c8bdc1502", COMPLEX, 6.0, 7.0),
    point("c5b2641f-28c8-467d-9d64-f52c8bdc1503", ACADEMIC, 7.0, 8.0),
    point("c5b2641f-28c8-467d-9d64-f52c8bdc1504", ACADEMIC),  # no band: every learner
]


class CurriculumScopeTest(unittest.TestCase):
    def ids(self, scoped):
        return [p["id"][-4:] for p in scoped.knowledge_points]

    def test_points_above_the_target_band_are_left_out(self):
        scoped = CurriculumScope.select(TOPICS, POINTS, Decimal("5.5"))

        self.assertEqual(self.ids(scoped), ["1501", "1504"])
        self.assertEqual(scoped.excluded_count, 2)

    def test_a_topic_whose_points_were_all_left_out_is_dropped(self):
        scoped = CurriculumScope.select(TOPICS, POINTS[:3], Decimal("5.5"))

        self.assertEqual([t["id"] for t in scoped.topics], [BASIC, EMPTY])

    def test_a_topic_that_never_had_points_is_kept_as_before(self):
        scoped = CurriculumScope.select(TOPICS, POINTS, Decimal("9.0"))

        self.assertIn(EMPTY, [t["id"] for t in scoped.topics])

    def test_the_highest_target_band_keeps_every_point_in_order(self):
        scoped = CurriculumScope.select(TOPICS, POINTS, Decimal("9.0"))

        self.assertEqual(self.ids(scoped), ["1501", "1502", "1503", "1504"])
        self.assertEqual([t["id"] for t in scoped.topics], [BASIC, COMPLEX, ACADEMIC, EMPTY])
        self.assertEqual(scoped.excluded_count, 0)

    def test_a_point_starting_exactly_at_the_target_band_is_kept(self):
        self.assertIn("1502", self.ids(CurriculumScope.select(TOPICS, POINTS, Decimal("6.0"))))

    def test_bands_of_the_kept_points_are_recorded(self):
        scoped = CurriculumScope.select(TOPICS, POINTS, Decimal("5.5"))

        self.assertEqual(scoped.bands["c5b2641f-28c8-467d-9d64-f52c8bdc1501"],
                         KnowledgePointBand(Decimal("4.0"), Decimal("5.0")))
        self.assertEqual(scoped.bands["c5b2641f-28c8-467d-9d64-f52c8bdc1504"], KnowledgePointBand(None, None))
        self.assertNotIn("c5b2641f-28c8-467d-9d64-f52c8bdc1502", scoped.bands)

    def test_content_without_band_fields_reaches_every_learner(self):
        legacy = [{k: v for k, v in p.items() if not k.startswith("effectiveBand")} for p in POINTS]

        self.assertEqual(len(CurriculumScope.select(TOPICS, legacy, Decimal("4.0")).knowledge_points), 4)

    def test_nothing_in_scope_is_reported(self):
        with self.assertRaises(NoCurriculumInScope):
            CurriculumScope.select(TOPICS, POINTS[1:3], Decimal("5.0"))

    def test_invalid_band_values_are_contract_errors(self):
        for bad in ("abc", True, 9.5, 4.3, -1):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                CurriculumScope.select(TOPICS, [point(POINTS[0]["id"], BASIC, bad, None)], Decimal("9.0"))


class TargetBandTest(unittest.TestCase):
    def test_reads_the_goal_target_band(self):
        self.assertEqual(target_band_of({"targetBand": 6.5}), Decimal("6.5"))
        self.assertEqual(target_band_of({"targetBand": "7.0"}), Decimal("7.0"))

    def test_missing_or_invalid_target_band_is_a_contract_error(self):
        for goal in ({}, {"targetBand": None}, {"targetBand": "x"}, {"targetBand": 10}, {"targetBand": True}):
            with self.subTest(goal=goal), self.assertRaises(ValueError):
                target_band_of(goal)


if __name__ == "__main__":
    unittest.main()
