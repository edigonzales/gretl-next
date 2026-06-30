---
name: gretl-pr-review
description: Use when reviewing, preparing, or summarizing a GRETL repository pull request, including module boundary impact, public DSL impact, test evidence, risky files, and generated or runtime artifacts.
---

# GRETL PR Review

## When To Use

Use this skill when reviewing a diff, preparing a PR summary, checking readiness before handoff, or explaining change risk in this repository.

## Sources Of Truth

- `AGENTS.md` for module rules, test commands, dependency guidance, and finish criteria.
- `docs/architecture.md` for module and worker-isolation boundaries.
- `docs/control-plane.md` for Control Plane runtime contracts.
- The local diff against the chosen base branch.

## Workflow

1. Inspect the changed files and classify affected modules.
2. Read nearby code, tests, and docs before judging behavior.
3. Identify public DSL/API impact, Control Plane contract impact, dependency boundary impact, generated artifact impact, and runtime image impact.
4. Check whether tests match the behavior being changed rather than only the implementation.
5. Summarize only risks grounded in the diff or nearby code.

## Guardrails

- For review requests, put findings first, ordered by severity, with file and line references.
- Do not invent risk, test coverage, or commands that were not inspected or run.
- Treat build files, CI/workflow files, security config, suppressions, generated docs, and runtime image files as higher risk.
- Preserve unrelated user changes and do not recommend broad refactors unless required by the reviewed change.

## Verification

- Prefer the narrowest meaningful test evidence for the affected module.
- `./gradlew check` is the default broad repository verification.
- Control Plane packaging or Spring Boot dependency changes also need `./gradlew :gretl-control-server:bootJar :gretl-control-worker:bootJar`.
- PostgreSQL/PostGIS behavior needs `./gradlew :gretl-core:integrationTest`.

## Final Output

For reviews, list findings first, then questions or assumptions, then brief test gaps. For PR prep, include behavior changed, module boundary impact, public API/DSL impact, tests run, skipped checks, and residual risk.
