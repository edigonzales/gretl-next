---
name: gretl-control-plane-change
description: Use when changing GRETL Control Plane manifests, shared DTOs, validation, server API or UI, scheduling, secrets, worker claiming, logging, cancellation, notifications, or server-worker contracts.
---

# GRETL Control Plane Change

## When To Use

Use this skill for changes touching `gretl-control-common`, `gretl-control-server`, `gretl-control-worker`, `gretl-server.yml`, or Control Plane documentation and examples.

## Sources Of Truth

- `AGENTS.md` for module rules, security boundaries, and required checks.
- `docs/control-plane.md` for manifest shape and runtime contract.
- `gretl-control-common` for shared API DTOs and manifest validation.
- `gretl-control-server` and `gretl-control-worker` for implementation details.

## Workflow

1. Identify whether the change affects shared contracts, server-only behavior, worker-only behavior, or operator documentation.
2. Read the nearest DTO, manifest, service, controller, worker client, and tests before editing.
3. Keep durable job configuration sourced from `gretl-server.yml`; the UI must not edit Git manifests.
4. Preserve pull-worker behavior: workers claim runs from the server and start a fresh external `gretl` process per run.
5. Update docs or examples when manifest fields, runtime contract, environment variables, or operator commands change.

## Guardrails

- Keep `gretl-control-common` free of Spring Boot server, persistence, and process execution logic.
- Keep worker endpoints token-protected and do not weaken secret encryption or log redaction.
- Do not widen manifest or API compatibility without documenting the behavior and migration impact.
- Stop before introducing server-to-worker network requirements, worker-side Docker/Kubernetes execution, or UI Git manifest editing unless explicitly requested.

## Verification

- Narrow: run the affected `gretl-control-*` tests first.
- Broad: run `./gradlew check :gretl-control-server:bootJar :gretl-control-worker:bootJar`.
- If packaging or Spring Boot dependencies changed, the broad command is required before handoff.

## Final Output

Report changed Control Plane behavior, affected contracts, docs updated, tests run, skipped checks, and any compatibility or operational risk left.
