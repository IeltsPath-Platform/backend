# System monitoring API v1

## Endpoint

`GET /api/admin/system/services` requires an authenticated `ADMIN` role.

Responses:

- `200 OK`: monitoring snapshot.
- `401 Unauthorized`: missing or invalid token.
- `403 Forbidden`: authenticated user without `ADMIN`.

```json
{
  "checkedAt": "2026-10-22T08:40:00Z",
  "services": [
    {
      "serviceId": "CONTENT-SERVICE",
      "status": "UP",
      "instances": [
        {
          "instanceId": "content-service:8082",
          "uri": "http://content-service:8082",
          "status": "UP",
          "responseMs": 34
        }
      ]
    },
    { "serviceId": "ACCESS-SERVICE", "status": "DOWN", "instances": [] }
  ]
}
```

## Snapshot rules

- `services` is the sorted union of service IDs registered in Eureka and the configured expected-service list. IDs are returned uppercase. The default expected services are `USER-SERVICE`, `ACCESS-SERVICE`, `ASSESSMENT-SERVICE`, `CONTENT-SERVICE`, `LEARNING-SERVICE`, `LIBRARY-SERVICE`, `COMMUNITY-SERVICE`, `GAME-SERVICE`, and `NOTIFICATION-SERVICE`. An expected service without registered instances is `DOWN` with an empty `instances` array.
- Each discovered instance is checked with `GET {uri}/actuator/health`; checks run concurrently with at most 32 health requests in flight. The request timeout is two seconds. Only the health `status` is returned; component details such as database and disk health are omitted. Connection errors, timeouts, and non-JSON responses are reported as `DOWN`.
- Instance status is one of `UP`, `DOWN`, `OUT_OF_SERVICE`, or `UNKNOWN`. Unknown or absent health status is `UNKNOWN`.
- Service status is `UP` when at least one instance is `UP`; otherwise it is `DOWN`.
- `API-GATEWAY` is included exactly once as `UP` for the request serving this response, even when the gateway is also registered in Eureka. `CONFIG-SERVER` and `EUREKA-SERVER` are included and checked through the gateway's configured service URLs. These entries use the same instance and health fields as discovered services.
- `checkedAt` is the UTC timestamp when the snapshot was created. `responseMs` is the elapsed request time in milliseconds, including failed health checks.
- Health details are not logged at INFO level. This API is read-only and does not start or stop services.
