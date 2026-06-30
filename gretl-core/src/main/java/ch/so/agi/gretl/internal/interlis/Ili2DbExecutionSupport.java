package ch.so.agi.gretl.internal.interlis;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.FileListener;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.interlis.iox_j.logging.FileLogger;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.tasks.AbstractIli2DbExportTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbSchemaImportTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbTransferTask;
import ch.so.agi.gretl.tasks.Ili2gpkgImport;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public final class Ili2DbExecutionSupport {

    private final Ili2DbOptionApplier commonOptions = new Ili2DbOptionApplier();
    private final Ili2DbSchemaOptionApplier schemaOptions = new Ili2DbSchemaOptionApplier();
    private final TransferInputResolver inputResolver = new TransferInputResolver();
    private final DatasetNameResolver datasetNameResolver = new DatasetNameResolver();

    public void executeSchemaImport(AbstractIli2DbSchemaImportTask task, Ili2DbFlavor flavor) {
        Config settings = flavor.createConfig(task);
        commonOptions.apply(task, settings, Ili2DbOperation.SCHEMA_IMPORT);
        schemaOptions.apply(task, settings);

        String iliFile = resolveIliFile(task);
        if (iliFile != null) {
            settings.setXtffile(iliFile);
        } else if (task.getModelNames().get().isEmpty() && !task.getMetaConfig().isPresent()) {
            throw new GradleException("Configure either iliFile(...) or modelNames(...)/models(...) for schema import.");
        }
        if (task.getIliMetaAttrsFile().isPresent()) {
            settings.setIliMetaAttrsFile(task.getIliMetaAttrsFile().get().getAsFile().getAbsolutePath());
        }

        runWithTraceReset(task, () -> runOnce(task, flavor, settings));
    }

    public void executeImport(AbstractIli2DbTransferTask task, Ili2DbFlavor flavor, Ili2DbOperation operation) {
        TransferInputResolver.TransferInputs inputs = inputResolver.resolve(task, true);
        if (inputs.isEmpty()) {
            return;
        }

        List<String> datasetNames = datasetNameResolver.resolve(task, inputs);

        runWithExternalFileLogger(task, operation, () -> {
            for (int i = 0; i < inputs.executionInputs().size(); i++) {
                String input = inputs.executionInputs().get(i);
                Config settings = flavor.createConfig(task);
                commonOptions.apply(task, settings, operation);
                applyGeoPackageImportOptionsIfNeeded(task, settings, !datasetNames.isEmpty());
                settings.setDatasetName(datasetNames.isEmpty() ? null : datasetNames.get(i));
                settings.setXtffile(input);
                settings.setItfTransferfile(inputs.usesLocalFiles() && Ili2db.isItfFilename(input));
                if (!runOnce(task, flavor, settings)) {
                    break;
                }
            }
        });
    }

    public void executeExport(AbstractIli2DbExportTask task, Ili2DbFlavor flavor) {
        TransferInputResolver.TransferInputs outputs = inputResolver.resolveLocal(task);
        if (outputs.isEmpty()) {
            return;
        }

        List<String> datasetNames = datasetNameResolver.resolve(task, outputs);

        runWithTraceReset(task, () -> {
            for (int i = 0; i < outputs.executionInputs().size(); i++) {
                String output = outputs.executionInputs().get(i);
                createParentDirectory(Path.of(output));
                Config settings = flavor.createConfig(task);
                commonOptions.apply(task, settings, Ili2DbOperation.EXPORT);
                if (task.getExport3().get()) {
                    settings.setVer3_export(true);
                }
                if (!task.getExportModels().get().isEmpty()) {
                    settings.setExportModels(String.join(";", task.getExportModels().get()));
                }
                settings.setDatasetName(datasetNames.isEmpty() ? null : datasetNames.get(i));
                settings.setXtffile(output);
                settings.setItfTransferfile(Ili2db.isItfFilename(output));
                if (!runOnce(task, flavor, settings)) {
                    break;
                }
            }
        });
    }

    public void executeDelete(AbstractIli2DbTask task, Ili2DbFlavor flavor) {
        List<String> datasetNames = datasetNameResolver.resolve(task);
        if (datasetNames.isEmpty()) {
            throw new GradleException("dataset is not configured");
        }
        runDatasetOperation(task, flavor, Ili2DbOperation.DELETE, datasetNames);
    }

    public void executeValidate(AbstractIli2DbTask task, Ili2DbFlavor flavor) {
        List<String> datasetNames = datasetNameResolver.resolve(task);
        runDatasetOperation(task, flavor, Ili2DbOperation.VALIDATE, datasetNames);
    }

    private void runDatasetOperation(AbstractIli2DbTask task, Ili2DbFlavor flavor, Ili2DbOperation operation, List<String> datasetNames) {
        runWithTraceReset(task, () -> {
            if (datasetNames.isEmpty()) {
                Config settings = flavor.createConfig(task);
                commonOptions.apply(task, settings, operation);
                if (operation == Ili2DbOperation.DELETE) {
                    settings.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
                }
                runOnce(task, flavor, settings);
                return;
            }
            for (String datasetName : datasetNames) {
                Config settings = flavor.createConfig(task);
                commonOptions.apply(task, settings, operation);
                if (operation == Ili2DbOperation.DELETE) {
                    settings.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
                }
                settings.setDatasetName(datasetName);
                if (!runOnce(task, flavor, settings)) {
                    break;
                }
            }
        });
    }

    private void applyGeoPackageImportOptionsIfNeeded(AbstractIli2DbTransferTask task, Config settings, boolean hasDatasets) {
        if (!(task instanceof Ili2gpkgImport gpkgImport)) {
            return;
        }
        settings.setDoImplicitSchemaImport(true);
        if (hasDatasets) {
            settings.setBasketHandling(Config.BASKET_HANDLING_READWRITE);
        }
        if (gpkgImport.getCoalesceJson().get()) {
            settings.setJsonTrafo(Config.JSON_TRAFO_COALESCE);
        }
        if (gpkgImport.getNameByTopic().get()) {
            settings.setNameOptimization(Config.NAME_OPTIMIZATION_TOPIC);
        }
        if (gpkgImport.getDefaultSrsCode().isPresent()) {
            settings.setDefaultSrsCode(gpkgImport.getDefaultSrsCode().get());
        }
        if (gpkgImport.getCreateEnumTabs().get()) {
            settings.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI);
        }
        if (gpkgImport.getCreateMetaInfo().get()) {
            settings.setCreateMetaInfo(true);
        }
        if (gpkgImport.getCreateGeomIdx().get()) {
            settings.setValue(Config.CREATE_GEOM_INDEX, Config.TRUE);
        }
    }

    private boolean runOnce(AbstractIli2DbTask task, Ili2DbFlavor flavor, Config settings) {
        GretlLogger log = LogEnvironment.getLogger(task.getClass());
        try {
            if (flavor.usesJdbcConnection()) {
                runPostgis(task.databaseSpec(), settings);
            } else {
                runFileDatabase(settings);
            }
            return true;
        } catch (Exception e) {
            if (e instanceof Ili2dbException && !task.getFailOnException().get()) {
                log.lifecycle(e.getMessage());
                return false;
            }
            log.error("failed to run ili2db", e);
            throw TaskUtil.toGradleException(asException(e));
        }
    }

    private void runPostgis(DatabaseSpec database, Config settings) throws Exception {
        try (Connection connection = connect(database)) {
            try {
                settings.setDburl(database.jdbcUrl());
                settings.setJdbcConnection(connection);
                Ili2db.readSettingsFromDb(settings);
                Ili2db.run(settings, null);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                settings.setJdbcConnection(null);
            }
        }
    }

    private void runFileDatabase(Config settings) throws Exception {
        if (settings.getDbfile() == null) {
            throw new GradleException("databaseFile is not configured");
        }
        createParentDirectory(Path.of(settings.getDbfile()));
        if (settings.getLogfile() != null) {
            createParentDirectory(Path.of(settings.getLogfile()));
        }
        Ili2db.readSettingsFromDb(settings);
        Ili2db.run(settings, null);
    }

    private static Connection connect(DatabaseSpec database) throws SQLException {
        Connection connection;
        if (database.username() == null || database.username().isBlank()) {
            connection = DriverManager.getConnection(database.jdbcUrl());
        } else {
            connection = DriverManager.getConnection(database.jdbcUrl(), database.username(), database.password());
        }
        connection.setAutoCommit(false);
        return connection;
    }

    private void runWithExternalFileLogger(AbstractIli2DbTask task, Ili2DbOperation operation, ThrowingRunnable runnable) {
        FileListener fileLogger = null;
        try {
            if (operation.usesExternalFileLogger() && task.getLogFile().isPresent()) {
                Path path = task.getLogFile().get().getAsFile().toPath();
                createParentDirectory(path);
                fileLogger = new FileLogger(path.toFile());
                EhiLogger.getInstance().addListener(fileLogger);
            }
            runnable.run();
        } catch (Exception e) {
            throw TaskUtil.toGradleException(e);
        } finally {
            if (fileLogger != null) {
                EhiLogger.getInstance().removeListener(fileLogger);
                fileLogger.close();
            }
            EhiLogger.getInstance().setTraceFilter(true);
        }
    }

    private void runWithTraceReset(AbstractIli2DbTask task, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw TaskUtil.toGradleException(e);
        } finally {
            EhiLogger.getInstance().setTraceFilter(true);
        }
    }

    private String resolveIliFile(AbstractIli2DbSchemaImportTask task) {
        Object value = task.getIliFileRaw();
        if (value == null) {
            return null;
        }
        if (value instanceof String text && (text.startsWith("ilidata:") || !text.contains("."))) {
            return text;
        }
        return task.getProject().file(value).getAbsolutePath();
    }

    private void createParentDirectory(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new GradleException("failed to create directory " + parent, e);
        }
    }

    private Exception asException(Throwable throwable) {
        return throwable instanceof Exception exception
                ? exception
                : new Exception(throwable.getMessage(), throwable);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
