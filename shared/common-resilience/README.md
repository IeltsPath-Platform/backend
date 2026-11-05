# common-resilience

Shared synchronous HTTP resilience policies for service-to-service calls. Callers select a target and a `CallKind`;
only connection failures and HTTP 5xx are counted or retried. READ and IDEMPOTENT_WRITE may retry; WRITE never does.
Circuit-breaker and bulkhead rejection is represented by `DownstreamUnavailableException` so the owning service can map
it to its established API error contract.
