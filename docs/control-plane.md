# GRETL Control Plane

The control plane is a small scheduler and run monitor for GRETL jobs. It keeps
GRETL itself as a Gradle-based runtime and adds a server plus pull workers.

## Modules

- `gretl-control-common`: shared manifest model and API DTOs.
- `gretl-control-server`: Spring Boot server with job catalog, run history,
  Quartz scheduling, worker API, secret storage, logs, notifications and static
  UI.
- `gretl-control-worker`: pull agent that registers with the server, claims runs
  and starts a fresh `gretl` process per run.

## Manifest

The server reads `gretl-server.yml` from the repository root by default. The
manifest is the source of truth for job code location and durable job settings:

```yaml
jobs:
  - id: import-colors
    name: Import colors
    projectDir: jobs/colors
    tasks: [importData]
    enabled: true
    cron: "0 0 2 * * ?"
    timezone: Europe/Zurich
    overlapPolicy: SKIP
    timeout: PT30M
    workerLabels: [small]
    jvm:
      maxHeap: 1g
      args: ["-Dexample=true"]
    parameters:
      - name: limit
        type: INTEGER
        defaultValue: 100
    secretRefs: [db-password]
    triggers:
      - jobId: upstream-job
        on: SUCCESS
    notifications:
      - on: [FAILED, TIMED_OUT]
        email: gis@example.org
        webhook: https://example.org/hooks/gretl
```

Cron expressions use Quartz syntax. Timeouts use ISO-8601 durations such as
`PT30M`.

## Running Locally

Start the server:

```bash
./gradlew :gretl-control-server:bootRun
```

Open `http://localhost:8080` for the UI. The default profile uses an in-memory
H2 database. For PostgreSQL, run with the `postgres` profile and set:

- `GRETL_CONTROL_JDBC_URL`
- `GRETL_CONTROL_JDBC_USERNAME`
- `GRETL_CONTROL_JDBC_PASSWORD`
- `GRETL_CONTROL_WORKER_TOKEN`
- `GRETL_CONTROL_SECRET_KEY`

The `postgres` profile enables Quartz JDBC storage. Provision the Quartz tables
for your PostgreSQL schema before starting the server, or temporarily set
`spring.quartz.jdbc.initialize-schema=always` for a disposable local database.

Start a worker:

```bash
GRETL_CONTROL_SERVER_URL=http://localhost:8080 \
GRETL_CONTROL_WORKER_TOKEN=dev-worker-token \
GRETL_CONTROL_WORKER_LABELS=geo \
GRETL_CONTROL_WORKSPACE_ROOT=/path/to/gretl-monorepo \
./gradlew :gretl-control-worker:bootRun
```

## Runtime Contract

- Workers pull work; the server does not need direct network access to workers.
- A run is executed as a fresh external `gretl` process in the configured
  `projectDir`.
- Manifest parameters become Gradle properties (`-Pname=value`) and environment
  variables (`GRETL_PARAM_NAME=value`).
- Secrets are stored encrypted by the server and are passed only to the claimed
  run as environment variables (`GRETL_SECRET_NAME=value`).
- Logs are streamed back to the server and written under
  `build/gretl-control/logs` by default.
- The default overlap policy is `SKIP`: a scheduled tick is recorded as skipped
  when the previous run of the same job is still active.
