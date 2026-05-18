package ch.so.agi.gretl.control.server.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class SecretRepository {
    private final JdbcTemplate jdbcTemplate;

    public SecretRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void put(String name, String cipherText, Instant updatedAt) {
        int updated = jdbcTemplate.update("update secrets set cipher_text = ?, updated_at = ? where name = ?",
                cipherText, Timestamp.from(updatedAt), name);
        if (updated == 0) {
            jdbcTemplate.update("insert into secrets(name, cipher_text, updated_at) values (?, ?, ?)",
                    name, cipherText, Timestamp.from(updatedAt));
        }
    }

    public Optional<String> findCipherText(String name) {
        List<String> values = jdbcTemplate.query("select cipher_text from secrets where name = ?",
                (rs, rowNum) -> rs.getString("cipher_text"), name);
        return values.stream().findFirst();
    }

    public List<String> findNames() {
        return jdbcTemplate.query("select name from secrets order by name", (rs, rowNum) -> rs.getString("name"));
    }
}
