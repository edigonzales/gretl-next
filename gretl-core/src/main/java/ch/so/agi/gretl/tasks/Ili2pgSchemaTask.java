package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

abstract class Ili2pgSchemaTask extends Ili2dbSchemaTask {
    @Input public abstract Property<String> getJdbcUrl();
    @Input @Optional public abstract Property<String> getUsername();
    @Internal public abstract Property<String> getPassword();

    @GretlDslMethod(required = true, description = "Configures the database connection with only a JDBC URL.")
    public void database(String jdbcUrl) {
        getJdbcUrl().set(jdbcUrl);
    }

    @GretlDslMethod(required = true, description = "Configures the database connection with JDBC URL, username and password.")
    public void database(String jdbcUrl, String username, String password) {
        getJdbcUrl().set(jdbcUrl);
        getUsername().set(username);
        getPassword().set(password);
    }

    protected DatabaseSpec databaseSpec() {
        if (!getJdbcUrl().isPresent()) {
            throw new GradleException("database is not configured");
        }
        return new DatabaseSpec(getJdbcUrl().get(), getUsername().getOrNull(), getPassword().getOrNull());
    }
}
