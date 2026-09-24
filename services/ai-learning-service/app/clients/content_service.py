"""HTTP client for canonical curriculum owned by Content Service."""

from typing import Any

import httpx
from uuid import UUID


class ContentServiceClient:
    def __init__(self, base_url: str, *, timeout: float = 10.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    async def get_curriculum(self, bearer_token: str) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        headers = {"Authorization": f"Bearer {bearer_token}"}
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.get(f"{self._base_url}/api/content/topics", headers=headers)
            response.raise_for_status()
            roots = response.json()
            if not isinstance(roots, list):
                raise ValueError("Content Service returned an invalid topic tree")

            topics = list(self._flatten_topics(roots))
            if len({str(topic["id"]) for topic in topics}) != len(topics):
                raise ValueError("Content Service returned duplicate topic ids")
            active_topics = [topic for topic in topics if str(topic.get("status", "ACTIVE")).upper() == "ACTIVE"]
            points_response = await client.get(
                f"{self._base_url}/api/content/knowledge-points",
                headers=headers,
            )
            points_response.raise_for_status()
            body = points_response.json()
            if not isinstance(body, list):
                raise ValueError("Content Service returned invalid knowledge points")
            active_topic_ids = {str(topic["id"]) for topic in active_topics}
            knowledge_points: list[dict[str, Any]] = []
            seen_point_ids: set[str] = set()
            for point in body:
                if not isinstance(point, dict):
                    raise ValueError("Content Service returned an invalid knowledge point")
                try:
                    point_id = str(UUID(str(point["id"])))
                    topic_id = str(UUID(str(point["topicId"])))
                    point_status = str(point["status"]).upper()
                except (KeyError, TypeError, ValueError) as exc:
                    raise ValueError("Content Service returned invalid knowledge-point identity") from exc
                if point_status not in {"ACTIVE", "INACTIVE"}:
                    raise ValueError("Content Service returned an invalid knowledge-point status")
                if point_status == "INACTIVE":
                    continue
                if topic_id not in active_topic_ids:
                    raise ValueError("Active knowledge point references a missing or inactive topic")
                if point_id in seen_point_ids:
                    raise ValueError("Content Service returned duplicate knowledge-point ids")
                seen_point_ids.add(point_id)
                point["id"] = point_id
                point["topicId"] = topic_id
                knowledge_points.append(point)
        return active_topics, knowledge_points

    @classmethod
    def _flatten_topics(cls, topics: list[dict[str, Any]]) -> list[dict[str, Any]]:
        flattened: list[dict[str, Any]] = []
        if any(not isinstance(topic, dict) for topic in topics):
            raise ValueError("Content Service returned an invalid topic")
        try:
            ordered = sorted(
                topics,
                key=lambda item: (int(item["sortOrder"]), str(UUID(str(item["id"]))),),
            )
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError("Content Service returned invalid topic identity or ordering") from exc
        for topic in ordered:
            if str(topic.get("status", "")).upper() not in {"ACTIVE", "INACTIVE"}:
                raise ValueError("Content Service returned an invalid topic status")
            if not str(topic.get("name", "")).strip():
                raise ValueError("Content Service returned a topic with a blank name")
            topic["id"] = str(UUID(str(topic["id"])))
            flattened.append(topic)
            children = topic.get("children", [])
            if not isinstance(children, list):
                raise ValueError("Content Service returned invalid topic children")
            flattened.extend(cls._flatten_topics(children))
        return flattened
