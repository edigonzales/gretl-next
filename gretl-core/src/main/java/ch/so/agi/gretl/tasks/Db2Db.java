package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.internal.db2db.DbTransferEngine;
import ch.so.agi.gretl.internal.db2db.DbTransferRequest;
import ch.so.agi.gretl.internal.db2db.DbTransferSpec;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.Action;
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

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@GretlTaskDoc(
        name = "Db2Db",
        description = "Copies selected rows from a source database into a target table.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Kopiert ausgewählte Zeilen aus einer Quell-Datenbank in eine Zieltabelle.") }
)
public abstract class Db2Db extends AbstractCoreGretlTask {

    private final List<DbTransferSpec> transfers;
    private final ConfigurableFileCollection transferSqlFiles;
    private final GretlLogger log;

    @Input
    public abstract Property<String> getSourceJdbcUrl();

    @Input
    @Optional
    public abstract Property<String> getSourceUsername();

    @Internal
    public abstract Property<String> getSourcePassword();

    @Input
    public abstract Property<String> getTargetJdbcUrl();

    @Input
    @Optional
    public abstract Property<String> getTargetUsername();

    @Internal
    public abstract Property<String> getTargetPassword();

    @Input
    public abstract Property<Integer> getBatchSize();

    @Input
    public abstract Property<Integer> getFetchSize();

    @Input
    @Optional
    public abstract MapProperty<String, String> getSqlParameters();

    @Input
    @Optional
    public abstract ListProperty<Map<String, String>> getSqlParameterSets();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getTransferSqlFiles() {
        return transferSqlFiles;
    }

    @Input
    public List<String> getTransferConfiguration() {
        return transfers.stream()
                .map(DbTransferSpec::inputSignature)
                .toList();
    }

    @Inject
    public Db2Db() {
        this.transfers = new ArrayList<>();
        this.transferSqlFiles = getProject().files();
        this.log = LogEnvironment.getLogger(Db2Db.class);
        getBatchSize().convention(5000);
        getFetchSize().convention(5000);
        getSqlParameters().convention(Collections.emptyMap());
        getSqlParameterSets().convention(Collections.emptyList());
    }

    @GretlDslMethod(required = true, description = "Configures the source database connection with a JDBC URL.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Quell-Datenbankverbindung mit einer JDBC-URL.") })
    public void sourceDatabase(String jdbcUrl) {
        getSourceJdbcUrl().set(jdbcUrl);
    }

    @GretlDslMethod(required = true, description = "Configures the source database connection with JDBC URL, username and password.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Quell-Datenbankverbindung mit JDBC-URL, Benutzername und Passwort.") })
    public void sourceDatabase(String jdbcUrl, String username, String password) {
        getSourceJdbcUrl().set(jdbcUrl);
        getSourceUsername().set(username);
        getSourcePassword().set(password);
    }

    @GretlDslMethod(required = true, description = "Configures the target database connection with a JDBC URL.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Ziel-Datenbankverbindung mit einer JDBC-URL.") })
    public void targetDatabase(String jdbcUrl) {
        getTargetJdbcUrl().set(jdbcUrl);
    }

    @GretlDslMethod(required = true, description = "Configures the target database connection with JDBC URL, username and password.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die Ziel-Datenbankverbindung mit JDBC-URL, Benutzername und Passwort.") })
    public void targetDatabase(String jdbcUrl, String username, String password) {
        getTargetJdbcUrl().set(jdbcUrl);
        getTargetUsername().set(username);
        getTargetPassword().set(password);
    }

    @GretlDslMethod(required = true, description = "Specifies a transfer from a SQL file into a target table.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt einen Transfer aus einer SQL-Datei in eine Zieltabelle an.") })
    public void transfer(Object sqlFile, String targetTable, boolean deleteAllRows, String... geometryColumns) {
        addTransfer(new TransferConfig()
                .sqlFile(sqlFile)
                .targetTable(targetTable)
                .deleteAllRows(deleteAllRows)
                .geometryColumns(geometryColumns));
    }

    @GretlDslMethod(required = true, description = "Specifies a transfer using nested configuration: sqlFile(...), targetTable(...), deleteAllRows(...) and geometryColumns(...).",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt einen Transfer mit verschachtelter Konfiguration an: sqlFile(...), targetTable(...), deleteAllRows(...) und geometryColumns(...).") })
    public void transfer(Action<TransferConfig> action) {
        TransferConfig config = new TransferConfig();
        action.execute(config);
        addTransfer(config);
    }

    @GretlDslMethod(description = "Specifies one SQL parameter map used for a single execution of all transfers.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt eine SQL-Parameter-Map für eine einzelne Ausführung aller Transfers fest.") })
    public void sqlParameters(Map<String, ?> parameters) {
        getSqlParameters().set(toStringMap(parameters));
    }

    @GretlDslMethod(description = "Specifies multiple SQL parameter maps. For each map, all transfers are executed in order.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt mehrere SQL-Parameter-Maps an. Für jede Map werden alle Transfers in Reihenfolge ausgeführt.") })
    @SafeVarargs
    public final void sqlParameterSets(Map<String, ?>... parameterSets) {
        getSqlParameterSets().set(Stream.of(parameterSets)
                .map(Db2Db::toStringMap)
                .toList());
    }

    @GretlDslMethod(defaultValue = "5000", description = "Specifies the JDBC batch size for target inserts.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die JDBC-Batch-Grösse für Zieleinfügungen fest.") })
    public void batchSize(int value) {
        getBatchSize().set(value);
    }

    @GretlDslMethod(defaultValue = "5000", description = "Specifies the JDBC fetch size for source reads.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die JDBC-Fetch-Grösse für Quell-Lesevorgänge fest.") })
    public void fetchSize(int value) {
        getFetchSize().set(value);
    }

    @TaskAction
    public void executeTask() {
        try {
            new DbTransferEngine().execute(createRequest());
        } catch (Exception e) {
            log.error("Exception while executing Db2Db.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private DbTransferRequest createRequest() {
        if (!getSourceJdbcUrl().isPresent()) {
            throw new GradleException("sourceDatabase is not configured");
        }
        if (!getTargetJdbcUrl().isPresent()) {
            throw new GradleException("targetDatabase is not configured");
        }
        if (transfers.isEmpty()) {
            throw new GradleException("transfers is empty");
        }

        return new DbTransferRequest(
                getName(),
                new DatabaseSpec(getSourceJdbcUrl().get(), getSourceUsername().getOrNull(), getSourcePassword().getOrNull()),
                new DatabaseSpec(getTargetJdbcUrl().get(), getTargetUsername().getOrNull(), getTargetPassword().getOrNull()),
                transfers,
                resolveParameterSets(),
                getBatchSize().get(),
                getFetchSize().get()
        );
    }

    private void addTransfer(TransferConfig config) {
        if (config.sqlFile == null) {
            throw new IllegalArgumentException("transfer sqlFile must not be null");
        }
        DbTransferSpec spec = config.toSpec(getProject().file(config.sqlFile).toPath());
        transfers.add(spec);
        transferSqlFiles.from(config.sqlFile);
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

    public static final class TransferConfig {
        private Object sqlFile;
        private String targetTable;
        private boolean deleteAllRows;
        private final List<String> geometryColumns = new ArrayList<>();

        public TransferConfig sqlFile(Object sqlFile) {
            this.sqlFile = sqlFile;
            return this;
        }

        public TransferConfig targetTable(String targetTable) {
            this.targetTable = targetTable;
            return this;
        }

        public TransferConfig deleteAllRows(boolean deleteAllRows) {
            this.deleteAllRows = deleteAllRows;
            return this;
        }

        public TransferConfig geometryColumns(String... geometryColumns) {
            Collections.addAll(this.geometryColumns, geometryColumns);
            return this;
        }

        public void setSqlFile(Object sqlFile) {
            this.sqlFile = sqlFile;
        }

        public void setTargetTable(String targetTable) {
            this.targetTable = targetTable;
        }

        public void setDeleteAllRows(boolean deleteAllRows) {
            this.deleteAllRows = deleteAllRows;
        }

        private DbTransferSpec toSpec(Path absoluteSqlFile) {
            if (targetTable == null || targetTable.isBlank()) {
                throw new IllegalArgumentException("transfer targetTable must not be null or blank");
            }
            return new DbTransferSpec(absoluteSqlFile, targetTable, deleteAllRows, geometryColumns);
        }
    }
}
