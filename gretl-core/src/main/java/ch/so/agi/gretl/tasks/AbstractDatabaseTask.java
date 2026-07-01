package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import java.util.List;

abstract class AbstractDatabaseTask extends AbstractCoreGretlTask {

    @Input
    public abstract Property<String> getJdbcUrl();

    @Input
    @Optional
    public abstract Property<String> getUsername();

    @Internal
    public abstract Property<String> getPassword();

    @GretlDslMethod(required = true, description = "Configures the database connection using a JDBC URL.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Datenbankverbindung mit einer JDBC-URL.") })
    public void database(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new GradleException("database jdbcUrl must not be null or blank");
        }
        getJdbcUrl().set(jdbcUrl);
    }

    @GretlDslMethod(required = true, description = "Configures the database connection using a JDBC URL, username and password.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Datenbankverbindung mit JDBC-URL, Benutzername und Passwort.") })
    public void database(String jdbcUrl, String username, String password) {
        database(jdbcUrl);
        getUsername().set(username);
        getPassword().set(password);
    }

    public void setDatabase(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new GradleException("database must not be empty");
        }
        if (values.size() == 1) {
            database(values.get(0));
        } else if (values.size() >= 3) {
            database(values.get(0), values.get(1), values.get(2));
        } else {
            throw new GradleException("database list must contain jdbcUrl or jdbcUrl, username and password");
        }
    }

    protected DatabaseSpec databaseSpec() {
        if (!getJdbcUrl().isPresent()) {
            throw new GradleException("database is not configured");
        }
        return new DatabaseSpec(getJdbcUrl().get(), getUsername().getOrNull(), getPassword().getOrNull());
    }
}
