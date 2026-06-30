---
name: gretl-runtime-image-release
description: Use when changing GRETL versioning, snapshot publishing, Docker runtime image assets, Gradle wrapper or runtime assumptions, staged runtime image contents, release prep, or publication workflows.
---

# GRETL Runtime Image Release

## When To Use

Use this skill for changes to root versioning, publishing, `docker/`, `stageRuntimeImage`, `buildRuntimeImage`, `publishSnapshots`, Gradle wrapper/runtime assumptions, release prep, or runtime image documentation.

## Sources Of Truth

- `AGENTS.md` for build, packaging, and dependency guidance.
- `README.md` for build, runtime image, and snapshot publishing commands.
- `build.gradle` for version, dependency versions, publishing repositories, and runtime image tasks.
- `docker/` for runtime image assets and the `gretl` runner.

## Workflow

1. Classify the change as versioning, dependency upgrade, image staging, Docker build, publishing, or documentation.
2. Read `build.gradle`, relevant subproject build files, `docker/` assets, and README sections before editing.
3. Keep Gradle wrapper and Java 17 assumptions stable unless explicitly requested.
4. Keep Spring Boot dependency management scoped to Control Plane modules.
5. Update docs when commands, image contents, credentials mechanism, or publishing behavior change.

## Guardrails

- Do not publish snapshots, push images, or run external release actions without explicit human approval.
- Do not commit generated runtime output under `build/`, local image contents, credentials, or local runtime files.
- Do not upgrade the Gradle wrapper as part of normal dependency work unless explicitly requested.
- Stop if release validation cannot run and report the missing prerequisite.

## Verification

- Run `./gradlew check`.
- Run `./gradlew stageRuntimeImage` for runtime image or publication staging changes.
- Run `./gradlew buildRuntimeImage` only when Docker is available or explicitly requested.
- For Spring Boot upgrades, also run `./gradlew :gretl-control-server:bootJar :gretl-control-worker:bootJar`.

## Final Output

Report version or image behavior changed, release/publishing impact, commands run, skipped checks, and any manual approval still required.
