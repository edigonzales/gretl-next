package ch.so.agi.gretl.control.server.persistence;

import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.api.RunTriggerType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class RunRepository {
    private static final Set<RunStatus> ACTIVE_STATUSES = Set.of(RunStatus.QUEUED, RunStatus.CLAIMED, RunStatus.RUNNING);

    private final JdbcTemplate jdbcTemplate;
    private final JsonSupport jsonSupport;
    private final RowMapper<RunRecord> rowMapper = this::mapRun;

    public RunRepository(JdbcTemplate jdbcTemplate, JsonSupport jsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = jsonSupport;
    }

    public void insert(RunRecord run) {
        jdbcTemplate.update("""
                        insert into runs(id, job_id, status, trigger_type, triggered_by, worker_id, queued_at, claimed_at,
                            started_at, finished_at, exit_code, cancel_requested, parameters_json, message, log_path)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                run.id(), run.jobId(), run.status().name(), run.triggerType().name(), run.triggeredBy(), run.workerId(),
                timestamp(run.queuedAt()), timestamp(run.claimedAt()), timestamp(run.startedAt()), timestamp(run.finishedAt()),
                run.exitCode(), run.cancelRequested(), jsonSupport.writeMap(run.parameters()), run.message(), run.logPath());
    }

    public Optional<RunRecord> find(String id) {
        List<RunRecord> runs = jdbcTemplate.query("select * from runs where id = ?", rowMapper, id);
        return runs.stream().findFirst();
    }

    public List<RunRecord> findRecent(int limit) {
        return jdbcTemplate.query("select * from runs order by queued_at desc limit ?", rowMapper, limit);
    }

    public List<RunRecord> findRecentForJob(String jobId, int limit) {
        return jdbcTemplate.query("select * from runs where job_id = ? order by queued_at desc limit ?", rowMapper, jobId, limit);
    }

    public List<RunRecord> findActive() {
        return jdbcTemplate.query("""
                        select * from runs
                        where status in (?, ?, ?)
                        order by queued_at desc
                        """,
                rowMapper,
                RunStatus.QUEUED.name(),
                RunStatus.CLAIMED.name(),
                RunStatus.RUNNING.name());
    }

    public boolean hasActiveRun(String jobId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from runs where job_id = ? and status in (?, ?, ?)",
                Integer.class,
                jobId,
                RunStatus.QUEUED.name(),
                RunStatus.CLAIMED.name(),
                RunStatus.RUNNING.name());
        return count != null && count > 0;
    }

    public List<RunRecord> findQueued() {
        return jdbcTemplate.query("select * from runs where status = ? order by queued_at asc", rowMapper, RunStatus.QUEUED.name());
    }

    public boolean claim(String runId, String workerId, Instant now) {
        return jdbcTemplate.update("""
                        update runs
                        set status = ?, worker_id = ?, claimed_at = ?
                        where id = ? and status = ?
                        """,
                RunStatus.CLAIMED.name(), workerId, timestamp(now), runId, RunStatus.QUEUED.name()) == 1;
    }

    public void updateStatus(String runId, RunStatus status, Integer exitCode, String message, Instant now) {
        if (status == RunStatus.RUNNING) {
            jdbcTemplate.update("""
                            update runs
                            set status = ?, started_at = coalesce(started_at, ?), message = ?
                            where id = ?
                            """,
                    status.name(), timestamp(now), message, runId);
            return;
        }
        boolean terminal = !ACTIVE_STATUSES.contains(status);
        jdbcTemplate.update("""
                        update runs
                        set status = ?, exit_code = ?, message = ?, finished_at = case when ? then ? else finished_at end
                        where id = ?
                        """,
                status.name(), exitCode, message, terminal, timestamp(now), runId);
    }

    public void requestCancel(String runId) {
        jdbcTemplate.update("update runs set cancel_requested = true where id = ?", runId);
    }

    public void updateQueuedMessage(String runId, String message) {
        jdbcTemplate.update("update runs set message = ? where id = ? and status = ?",
                message, runId, RunStatus.QUEUED.name());
    }

    public boolean skipQueued(String runId, String message, Instant now) {
        return jdbcTemplate.update("""
                        update runs
                        set status = ?, message = ?, finished_at = ?
                        where id = ? and status = ?
                        """,
                RunStatus.SKIPPED.name(), message, timestamp(now), runId, RunStatus.QUEUED.name()) == 1;
    }

    public void setLogPath(String runId, String logPath) {
        jdbcTemplate.update("update runs set log_path = ? where id = ?", logPath, runId);
    }

    public void deleteOlderThan(Instant threshold) {
        jdbcTemplate.update("delete from runs where finished_at is not null and finished_at < ?", timestamp(threshold));
    }

    private RunRecord mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new RunRecord(
                rs.getString("id"),
                rs.getString("job_id"),
                RunStatus.valueOf(rs.getString("status")),
                RunTriggerType.valueOf(rs.getString("trigger_type")),
                rs.getString("triggered_by"),
                rs.getString("worker_id"),
                instant(rs.getTimestamp("queued_at")),
                instant(rs.getTimestamp("claimed_at")),
                instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("finished_at")),
                (Integer) rs.getObject("exit_code"),
                rs.getBoolean("cancel_requested"),
                jsonSupport.readMap(rs.getString("parameters_json")),
                rs.getString("message"),
                rs.getString("log_path"));
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
