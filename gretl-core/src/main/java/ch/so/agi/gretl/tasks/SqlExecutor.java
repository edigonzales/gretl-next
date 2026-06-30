package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import ch.so.agi.gretl.internal.sql.SqlExecutionEngine;
import ch.so.agi.gretl.internal.sql.SqlExecutionRequest;
import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;

@GretlTaskDoc(
        name = "SqlExecutor",
        description = "Executes one or more SQL files against a database.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Führt eine oder mehrere SQL-Dateien gegen eine Datenbank aus.") }
)
public abstract class SqlExecutor extends AbstractCoreGretlTask {
    private final ConfigurableFileCollection sqlFiles;
    private final GretlLogger log;

    /**
     * Datenbank-URL, z.B. {@code jdbc:sqlite:/tmp/example.db}.
     */
    @Input
    public abstract Property<String> getJdbcUrl();

    /**
     * Datenbank-Benutzer.
     */
    @Input
    @Optional
    public abstract Property<String> getUsername();

    /**
     * Datenbank-Passwort. Das Passwort ist bewusst kein normaler Task-Input, damit es nicht in Gradle-Metadaten landet.
     */
    @Internal
    public abstract Property<String> getPassword();

    /**
     * SQL-Dateien, deren Statements ausgeführt werden.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getSqlFiles() {
        return sqlFiles;
    }

    /**
     * Parameter für einen einzelnen SQL-Durchlauf.
     */
    @Input
    @Optional
    public abstract MapProperty<String, String> getSqlParameters();

    /**
     * Parameter für mehrere SQL-Durchläufe. Pro Parameter-Set werden alle SQL-Dateien in Reihenfolge ausgeführt.
     */
    @Input
    @Optional
    public abstract ListProperty<Map<String, String>> getSqlParameterSets();

    @Inject
    public SqlExecutor() {
        this.sqlFiles = getProject().files();
        this.log = LogEnvironment.getLogger(SqlExecutor.class);
        getSqlParameters().convention(Collections.emptyMap());
        getSqlParameterSets().convention(Collections.emptyList());
    }

    /**
     * Konfiguriert die Datenbank nur mit JDBC-URL.
     */
    @GretlDslMethod(required = true, description = "Configures the database connection with a JDBC URL.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Datenbankverbindung mit einer JDBC-URL.") })
    public void database(String jdbcUrl) {
        getJdbcUrl().set(jdbcUrl);
    }

    /**
     * Konfiguriert die Datenbank mit JDBC-URL, Benutzer und Passwort.
     */
    @GretlDslMethod(required = true, description = "Configures the database connection with JDBC URL, username and password.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Datenbankverbindung mit JDBC-URL, Benutzername und Passwort.") })
    public void database(String jdbcUrl, String username, String password) {
        getJdbcUrl().set(jdbcUrl);
        getUsername().set(username);
        getPassword().set(password);
    }

    /**
     * Fügt SQL-Dateien hinzu. Pfade werden relativ zum Gradle-Projekt aufgelöst.
     */
    @GretlDslMethod(required = true, description = "Specifies SQL files. Paths are resolved relative to the Gradle project.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt SQL-Dateien an. Pfade werden relativ zum Gradle-Projekt aufgelöst.") })
    public void sqlFiles(Object... paths) {
        getSqlFiles().from(paths);
    }

    /**
     * Setzt ein einzelnes SQL-Parameter-Set.
     */
    @GretlDslMethod(description = "Specifies one SQL parameter map used for a single execution of all SQL files.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt eine SQL-Parameter-Map für eine einzelne Ausführung aller SQL-Dateien fest.") })
    public void sqlParameters(Map<String, ?> parameters) {
        getSqlParameters().set(toStringMap(parameters));
    }

    /**
     * Setzt mehrere SQL-Parameter-Sets. Pro Set werden alle SQL-Dateien in Reihenfolge ausgeführt.
     */
    @GretlDslMethod(description = "Specifies multiple SQL parameter maps. For each map, all SQL files are executed in order.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt mehrere SQL-Parameter-Maps an. Für jede Map werden alle SQL-Dateien in Reihenfolge ausgeführt.") })
    @SafeVarargs
    public final void sqlParameterSets(Map<String, ?>... parameterSets) {
        getSqlParameterSets().set(Stream.of(parameterSets)
                .map(SqlExecutor::toStringMap)
                .toList());
    }

    @TaskAction
    public void executeSQLExecutor() {
        try {
            new SqlExecutionEngine().execute(createRequest());
        } catch (Exception e) {
            log.error("Exception while executing SqlExecutor.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private SqlExecutionRequest createRequest() {
        if (!getJdbcUrl().isPresent()) {
            throw new GradleException("database is not configured");
        }
        if (getSqlFiles().isEmpty()) {
            throw new GradleException("sqlFiles is empty");
        }

        List<Path> files = getSqlFiles().getFiles().stream()
                .map(File::toPath)
                .toList();

        List<Map<String, String>> parameterSets = resolveParameterSets();
        return new SqlExecutionRequest(
                getName(),
                new DatabaseSpec(getJdbcUrl().get(), getUsername().getOrNull(), getPassword().getOrNull()),
                files,
                parameterSets
        );
    }

    private List<Map<String, String>> resolveParameterSets() {
        Map<String, String> parameters = getSqlParameters().get();
        List<Map<String, String>> parameterSets = getSqlParameterSets().get();
        boolean hasParameters = parameters != null && !parameters.isEmpty();
        boolean hasParameterSets = parameterSets != null && !parameterSets.isEmpty();

        if (hasParameters && hasParameterSets) {
            throw new GradleException("Use either sqlParameters or sqlParameterSets, not both.");
        }
        if (hasParameterSets) {
            return parameterSets.stream()
                    .<Map<String, String>>map(LinkedHashMap::new)
                    .toList();
        }
        if (hasParameters) {
            return List.of(new LinkedHashMap<>(parameters));
        }
        return List.of(Collections.emptyMap());
    }

    private static Map<String, String> toStringMap(Map<String, ?> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        Map<String, String> converted = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("sql parameter names must not be null or blank");
            }
            Object value = entry.getValue();
            if (value == null) {
                throw new IllegalArgumentException("sql parameter values must not be null");
            }
            converted.put(key, value.toString());
        }
        return converted;
    }
}
