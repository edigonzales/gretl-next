package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.duckdb.DuckDbExecutionRequest;
import ch.so.agi.gretl.internal.duckdb.DuckDbExportSpec;
import ch.so.agi.gretl.internal.duckdb.DuckDbFileExportSpec;
import ch.so.agi.gretl.internal.duckdb.DuckDbFederationEngine;
import ch.so.agi.gretl.internal.duckdb.DuckDbSourceSpec;
import ch.so.agi.gretl.internal.duckdb.DuckDbTargetSpec;
import ch.so.agi.gretl.internal.duckdb.CsvSourceSpec;
import ch.so.agi.gretl.internal.duckdb.GeometryOverrideSpec;
import ch.so.agi.gretl.internal.duckdb.GpkgExportSpec;
import ch.so.agi.gretl.internal.duckdb.GpkgLayerSpec;
import ch.so.agi.gretl.internal.duckdb.GpkgSourceSpec;
import ch.so.agi.gretl.internal.duckdb.ParquetExportSpec;
import ch.so.agi.gretl.internal.duckdb.PostgresExportSpec;
import ch.so.agi.gretl.internal.duckdb.PostgresSourceSpec;
import ch.so.agi.gretl.internal.duckdb.PostgresTableSpec;
import ch.so.agi.gretl.internal.duckdb.PostgresTargetSpec;
import ch.so.agi.gretl.internal.duckdb.PostgresWriteMode;
import ch.so.agi.gretl.internal.duckdb.PostgresWritePath;
import ch.so.agi.gretl.internal.duckdb.XlsxExportSpec;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@GretlTaskDoc(
        name = "DuckDbSqlExecutor",
        description = "Executes SQL in a prepared DuckDB federation session.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Führt SQL in einer vorbereiteten DuckDB-Föderationssitzung aus.") }
)
public abstract class DuckDbSqlExecutor extends AbstractCoreGretlTask {
    private final ConfigurableFileCollection sqlFiles;
    private final ConfigurableFileCollection sourceFiles;
    private final ConfigurableFileCollection exportFiles;
    private final List<DuckDbSourceSpec> sources;
    private final List<DuckDbTargetSpec> targets;
    private final List<DuckDbExportSpec> exports;
    private final GretlLogger log;

    @OutputFile
    @Optional
    public abstract RegularFileProperty getDatabaseFile();

    @Input
    public abstract Property<Boolean> getInMemory();

    @Input
    public abstract Property<Boolean> getInstallExtensions();

    @Input
    @Optional
    public abstract MapProperty<String, String> getSqlParameters();

    @Input
    @Optional
    public abstract ListProperty<Map<String, String>> getSqlParameterSets();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getSqlFiles() {
        return sqlFiles;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getSourceFiles() {
        return sourceFiles;
    }

    @OutputFiles
    public ConfigurableFileCollection getExportFiles() {
        return exportFiles;
    }

    @Input
    public List<String> getSourceConfiguration() {
        return sources.stream().map(DuckDbSourceSpec::inputSignature).toList();
    }

    @Input
    public List<String> getTargetConfiguration() {
        return targets.stream().map(DuckDbTargetSpec::inputSignature).toList();
    }

    @Input
    public List<String> getExportConfiguration() {
        return exports.stream().map(DuckDbExportSpec::inputSignature).toList();
    }

    @Inject
    public DuckDbSqlExecutor() {
        this.sqlFiles = getProject().files();
        this.sourceFiles = getProject().files();
        this.exportFiles = getProject().files();
        this.sources = new ArrayList<>();
        this.targets = new ArrayList<>();
        this.exports = new ArrayList<>();
        this.log = LogEnvironment.getLogger(DuckDbSqlExecutor.class);
        getInMemory().convention(false);
        getInstallExtensions().convention(false);
        getSqlParameters().convention(Collections.emptyMap());
        getSqlParameterSets().convention(Collections.emptyList());
    }

    @GretlDslMethod(required = true, description = "Configures the DuckDB database file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert die DuckDB-Datenbankdatei.") })
    public void database(Object file) {
        setRegularFile(getDatabaseFile(), file);
    }

    @GretlDslMethod(description = "Uses an in-memory DuckDB database instead of a database file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Verwendet eine In-Memory-DuckDB-Datenbank anstelle einer Datenbankdatei.") })
    public void inMemoryDatabase() {
        getInMemory().set(true);
    }

    @GretlDslMethod(description = "Installs required DuckDB extensions before loading them. Intended for local development.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Installiert erforderliche DuckDB-Erweiterungen vor dem Laden. Für die lokale Entwicklung vorgesehen.") })
    public void installExtensions(boolean value) {
        getInstallExtensions().set(value);
    }

    @GretlDslMethod(required = true, description = "Specifies SQL files. Paths are resolved relative to the Gradle project.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt SQL-Dateien an. Pfade werden relativ zum Gradle-Projekt aufgelöst.") })
    public void sqlFiles(Object... paths) {
        getSqlFiles().from(paths);
    }

    @GretlDslMethod(required = true, description = "Configures federated sources.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert föderierte Quellen.") })
    public void sources(Action<SourcesConfig> action) {
        SourcesConfig config = new SourcesConfig(this);
        action.execute(config);
    }

    public void sources(Closure<?> closure) {
        getProject().configure(new SourcesConfig(this), closure);
    }

    @GretlDslMethod(description = "Configures writable targets for SQL and exports.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert beschreibbare Ziele für SQL und Exporte.") })
    public void targets(Action<TargetsConfig> action) {
        TargetsConfig config = new TargetsConfig(this);
        action.execute(config);
    }

    public void targets(Closure<?> closure) {
        getProject().configure(new TargetsConfig(this), closure);
    }

    @GretlDslMethod(description = "Configures exports executed after the SQL files.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Konfiguriert Exporte, die nach den SQL-Dateien ausgeführt werden.") })
    public void exports(Action<ExportsConfig> action) {
        ExportsConfig config = new ExportsConfig(this);
        action.execute(config);
    }

    public void exports(Closure<?> closure) {
        getProject().configure(new ExportsConfig(this), closure);
    }

    @GretlDslMethod(description = "Specifies one SQL parameter map used for a single execution of all SQL files.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt eine SQL-Parameter-Map für eine einzelne Ausführung aller SQL-Dateien fest.") })
    public void sqlParameters(Map<String, ?> parameters) {
        getSqlParameters().set(toStringMap(parameters));
    }

    @GretlDslMethod(description = "Specifies multiple SQL parameter maps. For each map, all SQL files are executed in order.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Gibt mehrere SQL-Parameter-Maps an. Für jede Map werden alle SQL-Dateien in Reihenfolge ausgeführt.") })
    @SafeVarargs
    public final void sqlParameterSets(Map<String, ?>... parameterSets) {
        getSqlParameterSets().set(Stream.of(parameterSets)
                .map(DuckDbSqlExecutor::toStringMap)
                .toList());
    }

    @TaskAction
    public void executeTask() {
        try {
            new DuckDbFederationEngine().execute(createRequest());
        } catch (Exception e) {
            log.error("Exception while executing DuckDbSqlExecutor.", e);
            throw TaskUtil.toGradleException(e);
        }
    }

    private DuckDbExecutionRequest createRequest() {
        boolean inMemory = getInMemory().get();
        boolean hasDatabaseFile = getDatabaseFile().isPresent();
        if (!hasDatabaseFile && !inMemory) {
            throw new GradleException("database file(...) or inMemoryDatabase() is not configured");
        }
        if (hasDatabaseFile && inMemory) {
            throw new GradleException("Use either database file(...) or inMemoryDatabase(), not both.");
        }
        if (getSqlFiles().isEmpty()) {
            throw new GradleException("sqlFiles is empty");
        }

        List<Path> files = getSqlFiles().getFiles().stream()
                .map(File::toPath)
                .toList();

        return new DuckDbExecutionRequest(
                getName(),
                hasDatabaseFile ? getDatabaseFile().get().getAsFile().toPath() : null,
                inMemory,
                getInstallExtensions().get(),
                sources,
                targets,
                files,
                resolveParameterSets(),
                exports
        );
    }

    private void addSource(DuckDbSourceSpec source) {
        sources.add(source);
        if (source instanceof GpkgSourceSpec gpkg) {
            sourceFiles.from(gpkg.file().toFile());
        } else if (source instanceof CsvSourceSpec csv) {
            sourceFiles.from(csv.file().toFile());
        }
    }

    private void addTarget(DuckDbTargetSpec target) {
        targets.add(target);
    }

    private void addExport(DuckDbExportSpec export) {
        exports.add(export);
        if (export instanceof DuckDbFileExportSpec fileExport) {
            exportFiles.from(fileExport.file().toFile());
        }
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

    public static final class SourcesConfig {
        private final DuckDbSqlExecutor task;

        private SourcesConfig(DuckDbSqlExecutor task) {
            this.task = task;
        }

        public void postgres(String alias, Action<PostgresConfig> action) {
            PostgresConfig config = new PostgresConfig(alias);
            action.execute(config);
            task.addSource(config.toSpec());
        }

        public void postgres(String alias, Closure<?> closure) {
            PostgresConfig config = new PostgresConfig(alias);
            task.getProject().configure(config, closure);
            task.addSource(config.toSpec());
        }

        public void gpkg(String alias, Action<GpkgConfig> action) {
            GpkgConfig config = new GpkgConfig(task.getProject(), alias);
            action.execute(config);
            task.addSource(config.toSpec());
        }

        public void gpkg(String alias, Closure<?> closure) {
            GpkgConfig config = new GpkgConfig(task.getProject(), alias);
            task.getProject().configure(config, closure);
            task.addSource(config.toSpec());
        }

        public void csv(String alias, Action<CsvConfig> action) {
            CsvConfig config = new CsvConfig(task.getProject(), alias);
            action.execute(config);
            task.addSource(config.toSpec());
        }

        public void csv(String alias, Closure<?> closure) {
            CsvConfig config = new CsvConfig(task.getProject(), alias);
            task.getProject().configure(config, closure);
            task.addSource(config.toSpec());
        }
    }

    public static final class TargetsConfig {
        private final DuckDbSqlExecutor task;

        private TargetsConfig(DuckDbSqlExecutor task) {
            this.task = task;
        }

        public void postgres(String alias, Action<PostgresTargetConfig> action) {
            PostgresTargetConfig config = new PostgresTargetConfig(alias);
            action.execute(config);
            task.addTarget(config.toSpec());
        }

        public void postgres(String alias, Closure<?> closure) {
            PostgresTargetConfig config = new PostgresTargetConfig(alias);
            task.getProject().configure(config, closure);
            task.addTarget(config.toSpec());
        }
    }

    public static final class PostgresTargetConfig {
        private final String alias;
        private DatabaseSpec database;

        private PostgresTargetConfig(String alias) {
            this.alias = alias;
        }

        public void database(String jdbcUrl) {
            this.database = new DatabaseSpec(jdbcUrl, null, null);
        }

        public void database(String jdbcUrl, String username, String password) {
            this.database = new DatabaseSpec(jdbcUrl, username, password);
        }

        private PostgresTargetSpec toSpec() {
            if (database == null) {
                throw new IllegalArgumentException("postgres target database is not configured");
            }
            return new PostgresTargetSpec(alias, database);
        }
    }

    public static final class PostgresConfig {
        private final String alias;
        private DatabaseSpec database;
        private String mode = "view";
        private boolean autoDetectGeometry = true;
        private final List<PostgresTableSpec> tables = new ArrayList<>();

        private PostgresConfig(String alias) {
            this.alias = alias;
        }

        public void database(String jdbcUrl) {
            this.database = new DatabaseSpec(jdbcUrl, null, null);
        }

        public void database(String jdbcUrl, String username, String password) {
            this.database = new DatabaseSpec(jdbcUrl, username, password);
        }

        public void table(String name) {
            table(name, config -> {
            });
        }

        public void table(String name, Action<PostgresTableConfig> action) {
            PostgresTableConfig config = new PostgresTableConfig(name);
            action.execute(config);
            tables.add(config.toSpec());
        }

        public void table(String name, Closure<?> closure) {
            PostgresTableConfig config = new PostgresTableConfig(name);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(config);
            closure.call();
            tables.add(config.toSpec());
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setAutoDetectGeometry(boolean autoDetectGeometry) {
            this.autoDetectGeometry = autoDetectGeometry;
        }

        private PostgresSourceSpec toSpec() {
            if (database == null) {
                throw new IllegalArgumentException("postgres source database is not configured");
            }
            return new PostgresSourceSpec(alias, database, mode, autoDetectGeometry, tables);
        }
    }

    public static final class PostgresTableConfig {
        private final String name;
        private String alias;
        private String mode;
        private final List<String> columns = new ArrayList<>();
        private final List<GeometryOverrideSpec> geometries = new ArrayList<>();

        private PostgresTableConfig(String name) {
            this.name = name;
        }

        public void alias(String alias) {
            this.alias = alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void columns(String... columns) {
            Collections.addAll(this.columns, columns);
        }

        public void setColumns(List<String> columns) {
            this.columns.clear();
            this.columns.addAll(columns);
        }

        public void geometry(String column, Action<GeometryConfig> action) {
            GeometryConfig config = new GeometryConfig(column);
            action.execute(config);
            geometries.add(config.toSpec());
        }

        public void geometry(String column, Closure<?> closure) {
            GeometryConfig config = new GeometryConfig(column);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(config);
            closure.call();
            geometries.add(config.toSpec());
        }

        private PostgresTableSpec toSpec() {
            return PostgresTableSpec.fromQualifiedName(name, alias, mode, columns, geometries);
        }
    }

    public static final class GeometryConfig {
        private final String column;
        private String alias;
        private Integer srid;
        private String type;
        private String encoding = "auto";
        private boolean force2d;
        private boolean include = true;

        private GeometryConfig(String column) {
            this.column = column;
        }

        public void alias(String alias) {
            this.alias = alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public void setSrid(Integer srid) {
            this.srid = srid;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public void setForce2d(boolean force2d) {
            this.force2d = force2d;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }

        private GeometryOverrideSpec toSpec() {
            return new GeometryOverrideSpec(column, alias, srid, type, encoding, force2d, include);
        }
    }

    public static final class GpkgConfig {
        private final Project project;
        private final String alias;
        private Path file;
        private String mode = "view";
        private final List<GpkgLayerSpec> layers = new ArrayList<>();

        private GpkgConfig(Project project, String alias) {
            this.project = project;
            this.alias = alias;
        }

        public File file(Object file) {
            File resolved = project.file(file);
            this.file = resolved.toPath();
            return resolved;
        }

        public void setFile(Object file) {
            file(file);
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void layer(String layer) {
            layer(layer, config -> {
            });
        }

        public void layer(String layer, Action<GpkgLayerConfig> action) {
            GpkgLayerConfig config = new GpkgLayerConfig(layer);
            action.execute(config);
            layers.add(config.toSpec());
        }

        public void layer(String layer, Closure<?> closure) {
            GpkgLayerConfig config = new GpkgLayerConfig(layer);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(config);
            closure.call();
            layers.add(config.toSpec());
        }

        private GpkgSourceSpec toSpec() {
            if (file == null) {
                throw new IllegalArgumentException("gpkg source file is not configured");
            }
            return new GpkgSourceSpec(alias, file, mode, layers);
        }
    }

    public static final class GpkgLayerConfig {
        private final String layer;
        private String alias;
        private String mode;
        private final List<String> columns = new ArrayList<>();
        private String srs;

        private GpkgLayerConfig(String layer) {
            this.layer = layer;
        }

        public void alias(String alias) {
            this.alias = alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void columns(String... columns) {
            Collections.addAll(this.columns, columns);
        }

        public void setColumns(List<String> columns) {
            this.columns.clear();
            this.columns.addAll(columns);
        }

        public void setSrs(String srs) {
            this.srs = srs;
        }

        private GpkgLayerSpec toSpec() {
            return new GpkgLayerSpec(layer, alias, mode, columns, srs);
        }
    }

    public static final class CsvConfig {
        private final Project project;
        private final String alias;
        private Path file;
        private String table = "data";
        private String mode = "view";
        private Boolean header;
        private String delimiter;
        private boolean allVarchar;

        private CsvConfig(Project project, String alias) {
            this.project = project;
            this.alias = alias;
        }

        public File file(Object file) {
            File resolved = project.file(file);
            this.file = resolved.toPath();
            return resolved;
        }

        public void setFile(Object file) {
            file(file);
        }

        public void table(String table) {
            this.table = table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setHeader(Boolean header) {
            this.header = header;
        }

        public void delimiter(String delimiter) {
            this.delimiter = delimiter;
        }

        public void setDelimiter(String delimiter) {
            this.delimiter = delimiter;
        }

        public void setAllVarchar(boolean allVarchar) {
            this.allVarchar = allVarchar;
        }

        private CsvSourceSpec toSpec() {
            if (file == null) {
                throw new IllegalArgumentException("csv source file is not configured");
            }
            return new CsvSourceSpec(alias, file, table, mode, header, delimiter, allVarchar);
        }
    }

    public static final class ExportsConfig {
        private final DuckDbSqlExecutor task;

        private ExportsConfig(DuckDbSqlExecutor task) {
            this.task = task;
        }

        public void gpkg(String name, Action<GpkgExportConfig> action) {
            GpkgExportConfig config = new GpkgExportConfig(task.getProject(), name);
            action.execute(config);
            task.addExport(config.toSpec());
        }

        public void gpkg(String name, Closure<?> closure) {
            GpkgExportConfig config = new GpkgExportConfig(task.getProject(), name);
            task.getProject().configure(config, closure);
            task.addExport(config.toSpec());
        }

        public void parquet(String name, Action<ParquetExportConfig> action) {
            ParquetExportConfig config = new ParquetExportConfig(task.getProject(), name);
            action.execute(config);
            task.addExport(config.toSpec());
        }

        public void parquet(String name, Closure<?> closure) {
            ParquetExportConfig config = new ParquetExportConfig(task.getProject(), name);
            task.getProject().configure(config, closure);
            task.addExport(config.toSpec());
        }

        public void xlsx(String name, Action<XlsxExportConfig> action) {
            XlsxExportConfig config = new XlsxExportConfig(task.getProject(), name);
            action.execute(config);
            task.addExport(config.toSpec());
        }

        public void xlsx(String name, Closure<?> closure) {
            XlsxExportConfig config = new XlsxExportConfig(task.getProject(), name);
            task.getProject().configure(config, closure);
            task.addExport(config.toSpec());
        }

        public void postgres(String name, Action<PostgresExportConfig> action) {
            PostgresExportConfig config = new PostgresExportConfig(name);
            action.execute(config);
            task.addExport(config.toSpec());
        }

        public void postgres(String name, Closure<?> closure) {
            PostgresExportConfig config = new PostgresExportConfig(name);
            task.getProject().configure(config, closure);
            task.addExport(config.toSpec());
        }
    }

    public abstract static class BaseExportConfig {
        protected final Project project;
        protected final String name;
        protected String query;
        protected Path file;
        protected boolean overwrite;

        private BaseExportConfig(Project project, String name) {
            this.project = project;
            this.name = name;
        }

        public void query(String query) {
            this.query = query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public File file(Object file) {
            File resolved = project.file(file);
            this.file = resolved.toPath();
            return resolved;
        }

        public void setFile(Object file) {
            file(file);
        }

        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }
    }

    public static final class GpkgExportConfig extends BaseExportConfig {
        private String layer;
        private String srs;

        private GpkgExportConfig(Project project, String name) {
            super(project, name);
        }

        public void setLayer(String layer) {
            this.layer = layer;
        }

        public void setSrs(String srs) {
            this.srs = srs;
        }

        private GpkgExportSpec toSpec() {
            if (file == null) {
                throw new IllegalArgumentException("gpkg export file is not configured");
            }
            return new GpkgExportSpec(name, query, file, layer, srs, overwrite);
        }
    }

    public static final class ParquetExportConfig extends BaseExportConfig {
        private ParquetExportConfig(Project project, String name) {
            super(project, name);
        }

        private ParquetExportSpec toSpec() {
            if (file == null) {
                throw new IllegalArgumentException("parquet export file is not configured");
            }
            return new ParquetExportSpec(name, query, file, overwrite);
        }
    }

    public static final class XlsxExportConfig extends BaseExportConfig {
        private String sheet = "Sheet1";
        private boolean header = true;
        private int sheetRowLimit = 1_048_576;

        private XlsxExportConfig(Project project, String name) {
            super(project, name);
        }

        public void setSheet(String sheet) {
            this.sheet = sheet;
        }

        public void setHeader(boolean header) {
            this.header = header;
        }

        public void setSheetRowLimit(int sheetRowLimit) {
            this.sheetRowLimit = sheetRowLimit;
        }

        private XlsxExportSpec toSpec() {
            if (file == null) {
                throw new IllegalArgumentException("xlsx export file is not configured");
            }
            return new XlsxExportSpec(name, query, file, sheet, header, sheetRowLimit, overwrite);
        }
    }

    public static final class PostgresExportConfig {
        private final String name;
        private String target;
        private String query;
        private String table;
        private String mode;
        private String writePath;
        private boolean create;
        private final List<GeometryOverrideSpec> geometries = new ArrayList<>();

        private PostgresExportConfig(String name) {
            this.name = name;
        }

        public void target(String target) {
            this.target = target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public void query(String query) {
            this.query = query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public void table(String table) {
            this.table = table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setWritePath(String writePath) {
            this.writePath = writePath;
        }

        public void setCreate(boolean create) {
            this.create = create;
        }

        public void geometry(String column, Action<GeometryConfig> action) {
            GeometryConfig config = new GeometryConfig(column);
            action.execute(config);
            geometries.add(config.toSpec());
        }

        public void geometry(String column, Closure<?> closure) {
            GeometryConfig config = new GeometryConfig(column);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(config);
            closure.call();
            geometries.add(config.toSpec());
        }

        private PostgresExportSpec toSpec() {
            return new PostgresExportSpec(
                    name,
                    target,
                    query,
                    table,
                    PostgresWriteMode.parseRequired(mode),
                    PostgresWritePath.parse(writePath),
                    create,
                    geometries);
        }
    }
}
