---
name: gretl-task-dsl-docs
description: Use when changing public GRETL task classes, task DSL annotations, task examples, migration documentation, Kotlin DSL guidance, or generated task reference inputs.
---

# GRETL Task DSL Docs

## When To Use

Use this skill for changes to public task classes, `@GretlTaskDoc`, `@GretlDslMethod`, task properties, job-facing syntax, examples, `docs/task-reference.md`, `docs/migration-from-gretl.md`, or `docs/kotlin-dsl.md`.

## Sources Of Truth

- `AGENTS.md` for public DSL stability and documentation alignment.
- `README.md` for top-level task usage examples.
- `docs/migration-from-gretl.md` and `docs/kotlin-dsl.md` for job-facing migration guidance.
- `generateTaskDocs` in `build.gradle` for generated task documentation inputs.

## Workflow

1. Determine whether the change affects public DSL, implementation only, examples, generated documentation, or migration guidance.
2. Read the task class, annotations, nearest tests, and relevant docs before editing.
3. Preserve public GRETL task DSL unless a breaking migration is explicitly requested.
4. Update examples and docs when users must change build scripts or task configuration.
5. Generate task docs when annotation or public task reference inputs change.

## Guardrails

- Treat generated task documentation as output; do not hand-edit generated output as the source of truth.
- Do not expose implementation details that are not part of the job-facing API.
- Keep documentation and examples aligned with actual command names, module names, and Gradle plugin IDs.
- Stop and call out any breaking DSL change before implementing broad migration fallout.

## Verification

- Narrow: run tests for the changed task behavior or doclet behavior.
- Run `./gradlew generateTaskDocs` when task annotations or public task reference inputs change.
- Run `./gradlew check` for broad task, doclet, or documentation changes.

## Final Output

Report public DSL impact, documentation updated, generated-doc command status, tests run, skipped checks, and migration notes.
