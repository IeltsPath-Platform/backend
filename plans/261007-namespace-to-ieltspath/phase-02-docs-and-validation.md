# Phase 2: update current documentation and validate

## Requirements

- Update current repository instructions, README, and architecture docs to use `com.ieltspath` and accurate source paths.
- Do not rewrite historical plan/report snapshots, generated artifacts, or third-party content.
- Confirm the migration does not change API routes, events, database schemas, environment variables, or Compose setup.

## Validation

- Search active source/build metadata and current maintainer docs for stale `com.group01`/`com/group01` references.
- Confirm Maven reactor compile and `git diff --check` pass.
- Review status/diff to ensure earlier Compose changes remain present and no unrelated file was modified.

## Risks / rollback

- Documentation can become inconsistent if only one of the package convention, Maven coordinate, or auto-configuration entry is updated. Keep the examples aligned with source and build metadata.
