package ch.so.agi.gretl.control.server.persistence;

import ch.so.agi.gretl.control.api.WorkerStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkerRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JsonSupport jsonSupport;
    private final RowMapper<WorkerRecord> rowMapper = this::mapWorker;

    public WorkerRepository(JdbcTemplate jdbcTemplate, JsonSupport jsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = jsonSupport;
    }

    public void upsert(WorkerRecord worker) {
        int updated = jdbcTemplate.update("""
                        update workers
                        set display_name = ?, labels = ?, capacity = ?, active_runs = ?, last_heartbeat = ?, status = ?
                        where id = ?
                        """,
                worker.displayName(), jsonSupport.writeStringList(worker.labels()), worker.capacity(), worker.activeRuns(),
                Timestamp.from(worker.lastHeartbeat()), worker.status().name(), worker.id());
        if (updated == 0) {
            jdbcTemplate.update("""
                            insert into workers(id, display_name, labels, capacity, active_runs, last_heartbeat, status)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                    worker.id(), worker.displayName(), jsonSupport.writeStringList(worker.labels()), worker.capacity(),
                    worker.activeRuns(), Timestamp.from(worker.lastHeartbeat()), worker.status().name());
        }
    }

    public Optional<WorkerRecord> find(String id) {
        List<WorkerRecord> workers = jdbcTemplate.query("select * from workers where id = ?", rowMapper, id);
        return workers.stream().findFirst();
    }

    public List<WorkerRecord> findAll() {
        return jdbcTemplate.query("select * from workers order by id", rowMapper);
    }

    public void markOfflineBefore(Instant threshold) {
        jdbcTemplate.update("update workers set status = ? where last_heartbeat < ?", WorkerStatus.OFFLINE.name(), Timestamp.from(threshold));
    }

    private WorkerRecord mapWorker(ResultSet rs, int rowNum) throws SQLException {
        return new WorkerRecord(
                rs.getString("id"),
                rs.getString("display_name"),
                jsonSupport.readStringList(rs.getString("labels")),
                rs.getInt("capacity"),
                rs.getInt("active_runs"),
                rs.getTimestamp("last_heartbeat").toInstant(),
                WorkerStatus.valueOf(rs.getString("status")));
    }
}
