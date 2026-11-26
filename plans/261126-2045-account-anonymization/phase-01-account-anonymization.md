# Phase 1 - approve deletion and retention policy

## Context

The account-deletion design is a proposal based on current migration schemas. Several records have legal, accounting, moderation, or user-generated-content implications.

## Requirements

- Legal/privacy owner approves retention of authentication audit events, assessment outcomes, and learning records.
- Product owner decides community post/comment handling and identifier reuse.
- Operations/security owner decides staff/admin deletion safeguards and recovery support.
- Confirm deployed schema and storage key conventions for every service.

## Files and implementation

- Review `docs/account-deletion-design.md` with the relevant owners.
- Record decisions and update the design before starting event or schema work.

## Validation

- Produce a retention matrix with a purpose and retention period for every retained personal or pseudonymous field.
- Verify the event payload contains no direct identifiers and each required service has an idempotent processing strategy.

## Risks and rollback

No data changes occur in this phase. Do not run production deletion or anonymization before approval.
