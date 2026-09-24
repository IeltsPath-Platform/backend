"""HTTP client for User Service learner-owned learning goals."""

from typing import Any

import httpx


class UserServiceClient:
    def __init__(self, base_url: str, *, timeout: float = 5.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    async def get_active_goal(self, bearer_token: str) -> dict[str, Any]:
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            response = await client.get(
                f"{self._base_url}/api/users/me/learning-goals/active",
                headers={"Authorization": f"Bearer {bearer_token}"},
            )
        response.raise_for_status()
        result = response.json()
        if not isinstance(result, dict):
            raise ValueError("User Service returned an invalid active-goal response")
        return result
