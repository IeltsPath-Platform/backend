# Phase 1: migrate source packages and build metadata

## Context

- Repository instructions: [`AGENTS.md`](../../AGENTS.md)
- Build overview: [`README.md`](../../README.md)
- Existing graph: `graphify-out/graph.json` (queried before source changes)

## Requirements

- Rename every repository-owned Java source/test package from `com.group01` to `com.ieltspath`.
- Move `src/main/java/com/group01` and `src/test/java/com/group01` trees to `com/ieltspath` throughout `infra/`, `services/`, and `shared/`.
- Change root and module Maven `groupId` values and internal Maven dependency coordinates.
- Update Spring Boot auto-configuration imports metadata.
- Preserve class names, artifact IDs, dependency versions, behavior, and all unrelated user changes.

## Validation

- Search Java, POM, and Spring metadata for old coordinates/package names.
- Run `mvn -q compile -DskipTests` from repository root.
- Run `git diff --check`.

## Risks / rollback

- A missed package path, import, Maven coordinate, or metadata entry can break compilation or Spring auto-configuration. Revert only this migration's edits if the focused reactor compile fails; do not revert unrelated pre-existing work.
