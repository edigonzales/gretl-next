package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractIli2DbTask extends AbstractInterlisTask {

    private final RegularFileProperty databaseFile;
    private final Property<String> jdbcUrl;
    private final Property<String> username;
    private final Property<String> password;
    private final Property<String> schema;
    private final ListProperty<String> modelNames;
    private final ListProperty<String> modelDirectories;
    private final ListProperty<String> baskets;
    private final ListProperty<String> topics;
    private final ListProperty<String> datasetNames;
    private final ListProperty<Integer> datasetSubstring;
    private final ConfigurableFileCollection datasetFiles;
    private final Property<Boolean> importTid;
    private final Property<Boolean> exportTid;
    private final Property<Boolean> importBid;
    private final RegularFileProperty preScript;
    private final RegularFileProperty postScript;
    private final Property<Boolean> deleteData;
    private final RegularFileProperty logFile;
    private final Property<Boolean> trace;
    private final RegularFileProperty validConfigFile;
    private final Property<Boolean> disableValidation;
    private final Property<Boolean> disableAreaValidation;
    private final Property<Boolean> forceTypeValidation;
    private final Property<Boolean> strokeArcs;
    private final Property<Boolean> skipPolygonBuilding;
    private final Property<Boolean> skipGeometryErrors;
    private final Property<Boolean> iligml20;
    private final Property<Boolean> disableRounding;
    private final Property<Boolean> failOnException;
    private final Property<String> proxy;
    private final Property<Integer> proxyPort;
    private Object dataset;

    @Inject
    public AbstractIli2DbTask() {
        ObjectFactory objects = getProject().getObjects();
        this.databaseFile = objects.fileProperty();
        this.jdbcUrl = objects.property(String.class);
        this.username = objects.property(String.class);
        this.password = objects.property(String.class);
        this.schema = objects.property(String.class);
        this.modelNames = objects.listProperty(String.class);
        this.modelDirectories = objects.listProperty(String.class);
        this.baskets = objects.listProperty(String.class);
        this.topics = objects.listProperty(String.class);
        this.datasetNames = objects.listProperty(String.class);
        this.datasetSubstring = objects.listProperty(Integer.class);
        this.datasetFiles = getProject().files();
        this.importTid = objects.property(Boolean.class);
        this.exportTid = objects.property(Boolean.class);
        this.importBid = objects.property(Boolean.class);
        this.preScript = objects.fileProperty();
        this.postScript = objects.fileProperty();
        this.deleteData = objects.property(Boolean.class);
        this.logFile = objects.fileProperty();
        this.trace = objects.property(Boolean.class);
        this.validConfigFile = objects.fileProperty();
        this.disableValidation = objects.property(Boolean.class);
        this.disableAreaValidation = objects.property(Boolean.class);
        this.forceTypeValidation = objects.property(Boolean.class);
        this.strokeArcs = objects.property(Boolean.class);
        this.skipPolygonBuilding = objects.property(Boolean.class);
        this.skipGeometryErrors = objects.property(Boolean.class);
        this.iligml20 = objects.property(Boolean.class);
        this.disableRounding = objects.property(Boolean.class);
        this.failOnException = objects.property(Boolean.class);
        this.proxy = objects.property(String.class);
        this.proxyPort = objects.property(Integer.class);
        getModelNames().convention(Collections.emptyList());
        getModelDirectories().convention(Collections.emptyList());
        getBaskets().convention(Collections.emptyList());
        getTopics().convention(Collections.emptyList());
        getDatasetNames().convention(Collections.emptyList());
        getDatasetSubstring().convention(Collections.emptyList());
        getImportTid().convention(false);
        getExportTid().convention(false);
        getImportBid().convention(false);
        getDeleteData().convention(false);
        getTrace().convention(false);
        getDisableValidation().convention(false);
        getDisableAreaValidation().convention(false);
        getForceTypeValidation().convention(false);
        getStrokeArcs().convention(false);
        getSkipPolygonBuilding().convention(false);
        getSkipGeometryErrors().convention(false);
        getIligml20().convention(false);
        getDisableRounding().convention(false);
        getFailOnException().convention(true);
    }

    @Internal
    public RegularFileProperty getDatabaseFile() {
        return databaseFile;
    }

    @Input
    @Optional
    public Property<String> getJdbcUrl() {
        return jdbcUrl;
    }

    @Input
    @Optional
    public Property<String> getUsername() {
        return username;
    }

    @Internal
    public Property<String> getPassword() {
        return password;
    }

    @Input
    @Optional
    public Property<String> getSchema() {
        return schema;
    }

    @Input
    @Optional
    public ListProperty<String> getModelNames() {
        return modelNames;
    }

    @Input
    @Optional
    public ListProperty<String> getModelDirectories() {
        return modelDirectories;
    }

    @Input
    @Optional
    public ListProperty<String> getBaskets() {
        return baskets;
    }

    @Input
    @Optional
    public ListProperty<String> getTopics() {
        return topics;
    }

    @Input
    @Optional
    public ListProperty<String> getDatasetNames() {
        return datasetNames;
    }

    @Input
    @Optional
    public ListProperty<Integer> getDatasetSubstring() {
        return datasetSubstring;
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public ConfigurableFileCollection getDatasetFiles() {
        return datasetFiles;
    }

    @Internal
    public Object getDatasetRaw() {
        return dataset;
    }

    @Input
    public Property<Boolean> getImportTid() {
        return importTid;
    }

    @Input
    public Property<Boolean> getExportTid() {
        return exportTid;
    }

    @Input
    public Property<Boolean> getImportBid() {
        return importBid;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getPreScript() {
        return preScript;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getPostScript() {
        return postScript;
    }

    @Input
    public Property<Boolean> getDeleteData() {
        return deleteData;
    }

    @OutputFile
    @Optional
    public RegularFileProperty getLogFile() {
        return logFile;
    }

    @Input
    public Property<Boolean> getTrace() {
        return trace;
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public RegularFileProperty getValidConfigFile() {
        return validConfigFile;
    }

    @Input
    public Property<Boolean> getDisableValidation() {
        return disableValidation;
    }

    @Input
    public Property<Boolean> getDisableAreaValidation() {
        return disableAreaValidation;
    }

    @Input
    public Property<Boolean> getForceTypeValidation() {
        return forceTypeValidation;
    }

    @Input
    public Property<Boolean> getStrokeArcs() {
        return strokeArcs;
    }

    @Input
    public Property<Boolean> getSkipPolygonBuilding() {
        return skipPolygonBuilding;
    }

    @Input
    public Property<Boolean> getSkipGeometryErrors() {
        return skipGeometryErrors;
    }

    @Input
    public Property<Boolean> getIligml20() {
        return iligml20;
    }

    @Input
    public Property<Boolean> getDisableRounding() {
        return disableRounding;
    }

    @Input
    public Property<Boolean> getFailOnException() {
        return failOnException;
    }

    @Input
    @Optional
    public Property<String> getProxy() {
        return proxy;
    }

    @Input
    @Optional
    public Property<Integer> getProxyPort() {
        return proxyPort;
    }

    @GretlDslMethod(required = true, description = "Configures a file-based ili2db database.")
    public void databaseFile(Object path) {
        setRegularFile(getDatabaseFile(), path);
    }

    @GretlDslMethod(required = true, description = "Configures the PostgreSQL/PostGIS database connection with only a JDBC URL.")
    public void database(String jdbcUrl) {
        requireNonBlank("database", jdbcUrl);
        getJdbcUrl().set(jdbcUrl);
    }

    @GretlDslMethod(required = true, description = "Configures the PostgreSQL/PostGIS database connection with JDBC URL, username and password.")
    public void database(String jdbcUrl, String username, String password) {
        requireNonBlank("database", jdbcUrl);
        getJdbcUrl().set(jdbcUrl);
        getUsername().set(username);
        getPassword().set(password);
    }

    @GretlDslMethod(description = "Sets the ili2db schema name.")
    public void schema(String name) {
        requireNonBlank("schema", name);
        getSchema().set(name);
    }

    @GretlDslMethod(description = "Alias for schema(...).")
    public void dbschema(String name) {
        schema(name);
    }

    @GretlDslMethod(description = "Adds INTERLIS model names.")
    public void modelNames(String... names) {
        getModelNames().addAll(requireNonBlank("modelNames", names));
    }

    @GretlDslMethod(description = "Alias for configuring the ili2db models option.")
    public void models(String value) {
        requireNonBlank("models", value);
        getModelNames().set(List.of(value));
    }

    @GretlDslMethod(description = "Adds model directory or repository entries.")
    public void modelDirectories(String... entries) {
        getModelDirectories().addAll(requireNonBlank("modelDirectories", entries));
    }

    @GretlDslMethod(description = "Alias for configuring the ili2db modeldir option.")
    public void modeldir(String value) {
        requireNonBlank("modeldir", value);
        getModelDirectories().set(List.of(value));
    }

    @GretlDslMethod(description = "Configures ili2db baskets.")
    public void baskets(String value) {
        requireNonBlank("baskets", value);
        getBaskets().set(List.of(value));
    }

    @GretlDslMethod(description = "Configures ili2db topics.")
    public void topics(String value) {
        requireNonBlank("topics", value);
        getTopics().set(List.of(value));
    }

    @GretlDslMethod(description = "Configures one or more ili2db dataset names.")
    public void dataset(Object dataset) {
        setDataset(dataset);
    }

    @GretlDslMethod(description = "Configures substring indexes used to derive legacy dataset names.")
    public void datasetSubstring(Iterable<Integer> datasetSubstring) {
        getDatasetSubstring().set(datasetSubstring);
    }

    @GretlDslMethod(description = "Configures substring indexes used to derive legacy dataset names.")
    public void datasetSubstring(Integer... datasetSubstring) {
        getDatasetSubstring().set(Arrays.asList(datasetSubstring));
    }

    @GretlDslMethod(description = "Writes ili2db logs to a text file.")
    public void logFile(Object path) {
        setRegularFile(getLogFile(), path);
    }

    public void dbfile(Object path) {
        databaseFile(path);
    }

    public void setDbschema(String value) {
        dbschema(value);
    }

    public void setModels(String value) {
        models(value);
    }

    public void setModeldir(String value) {
        modeldir(value);
    }

    public void setBaskets(String value) {
        baskets(value);
    }

    public void setTopics(String value) {
        topics(value);
    }

    public void setDataset(Object dataset) {
        this.dataset = dataset;
        datasetFiles.setFrom();
        if (dataset instanceof org.gradle.api.file.FileCollection fileCollection) {
            datasetFiles.from(fileCollection);
        }
    }

    public void importTid(boolean value) {
        getImportTid().set(value);
    }

    public void exportTid(boolean value) {
        getExportTid().set(value);
    }

    public void importBid(boolean value) {
        getImportBid().set(value);
    }

    public void preScript(Object value) {
        setRegularFile(getPreScript(), value);
    }

    public void postScript(Object value) {
        setRegularFile(getPostScript(), value);
    }

    public void deleteData(boolean value) {
        getDeleteData().set(value);
    }

    public void trace(boolean value) {
        getTrace().set(value);
    }

    public void validConfigFile(Object value) {
        setRegularFile(getValidConfigFile(), value);
    }

    public void disableValidation(boolean value) {
        getDisableValidation().set(value);
    }

    public void disableAreaValidation(boolean value) {
        getDisableAreaValidation().set(value);
    }

    public void forceTypeValidation(boolean value) {
        getForceTypeValidation().set(value);
    }

    public void strokeArcs(boolean value) {
        getStrokeArcs().set(value);
    }

    public void skipPolygonBuilding(boolean value) {
        getSkipPolygonBuilding().set(value);
    }

    public void skipGeometryErrors(boolean value) {
        getSkipGeometryErrors().set(value);
    }

    public void iligml20(boolean value) {
        getIligml20().set(value);
    }

    public void disableRounding(boolean value) {
        getDisableRounding().set(value);
    }

    public void failOnException(boolean value) {
        getFailOnException().set(value);
    }

    public void proxy(String value) {
        getProxy().set(value);
    }

    public void proxyPort(int value) {
        getProxyPort().set(value);
    }

    public DatabaseSpec databaseSpec() {
        if (!getJdbcUrl().isPresent()) {
            throw new GradleException("database is not configured");
        }
        return new DatabaseSpec(getJdbcUrl().get(), getUsername().getOrNull(), getPassword().getOrNull());
    }

    protected static List<String> requireNonBlank(String name, String... values) {
        if (values == null || values.length == 0) {
            throw new GradleException(name + " must not be empty");
        }
        return Arrays.stream(values)
                .map(value -> {
                    if (value == null || value.isBlank()) {
                        throw new GradleException(name + " must not contain null or blank values");
                    }
                    return value;
                })
                .toList();
    }

    protected static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new GradleException(name + " must not be null or blank");
        }
    }
}
