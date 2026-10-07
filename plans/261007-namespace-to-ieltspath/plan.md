# Java namespace migration to IELTSPath

Status: completed

## Scope

Move the repository-owned Java namespace and Maven group coordinates from `com.group01` to `com.ieltspath` across all 13 Maven modules. Keep service/module suffixes, artifact IDs, HTTP/event/database contracts, runtime behavior, and the unrelated Compose changes intact. Update current maintainer documentation. Exclude `third_party/`, generated/build artifacts, Graphify output, and historical plan/report snapshots.

## Phases

1. [Phase 1: migrate source packages and build metadata](phase-01-migrate-packages-and-build.md)
2. [Phase 2: update current documentation and validate](phase-02-docs-and-validation.md)

## Acceptance criteria

- All repository-owned main and test Java sources use `com.ieltspath.*`, and their directory paths match their package declarations.
- Every Maven module inherits or declares `com.ieltspath` as its group ID; internal dependencies resolve through the reactor.
- Spring auto-configuration metadata points to the renamed class.
- Current project instructions and architecture docs describe the new namespace; no old namespace remains in active source/build metadata.
- Maven reactor compile succeeds; no API, event, database, environment variable, or Compose behavior changes.
