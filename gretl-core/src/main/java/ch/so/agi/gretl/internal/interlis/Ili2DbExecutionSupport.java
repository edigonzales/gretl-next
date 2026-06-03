package ch.so.agi.gretl.internal.interlis;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.interlis.iox_j.logging.FileLogger;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import ch.so.agi.gretl.tasks.Ili2duckdbExport;
import ch.so.agi.gretl.tasks.Ili2duckdbImport;
import ch.so.agi.gretl.tasks.Ili2duckdbImportSchema;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Ili2DbExecutionSupport {

    private final DuckDbIli2DbFlavor duckDbFlavor = new DuckDbIli2DbFlavor();
    private final Ili2DbOptionApplier commonOptions = new Ili2DbOptionApplier();
    private final Ili2DbSchemaOptionApplier schemaOptions = new Ili2DbSchemaOptionApplier();
    private final TransferInputResolver inputResolver = new TransferInputResolver();
    private final DatasetNameResolver datasetNameResolver = new DatasetNameResolver();

    public void executeSchemaImport(Ili2duckdbImportSchema task) {
        Config settings = duckDbFlavor.createConfig(task);
        commonOptions.apply(task, settings, Config.FC_SCHEMAIMPORT);
        schemaOptions.apply(task, settings);

        if (task.getIliFile().isPresent()) {
            settings.setXtffile(task.getIliFile().get().getAsFile().getAbsolutePath());
        } else if (task.getModelNames().get().isEmpty()) {
            throw new GradleException("Configure either iliFile(...) or modelNames(...) for schema import.");
        }
        if (task.getIliMetaAttrsFile().isPresent()) {
            settings.setIliMetaAttrsFile(task.getIliMetaAttrsFile().get().getAsFile().getAbsolutePath());
        }

        try {
            runOnce(task, settings);
        } finally {
            EhiLogger.getInstance().setTraceFilter(true);
        }
    }

    public void executeImport(Ili2duckdbImport task) {
        TransferInputResolver.TransferInputs inputs = inputResolver.resolve(task, true);
        if (inputs.isEmpty()) {
            return;
        }

        List<String> datasetNames = datasetNameResolver.resolve(task, inputs);
        Config settings = duckDbFlavor.createConfig(task);
        commonOptions.apply(task, settings, Config.FC_IMPORT);

        ch.ehi.basics.logging.FileListener fileLogger = null;
        try {
            if (task.getLogFile().isPresent()) {
                Path path = task.getLogFile().get().getAsFile().toPath();
                createParentDirectory(path);
                fileLogger = new FileLogger(path.toFile());
                EhiLogger.getInstance().addListener(fileLogger);
            }
            for (int i = 0; i < inputs.executionInputs().size(); i++) {
                String input = inputs.executionInputs().get(i);
                settings.setDatasetName(datasetNames != null ? datasetNames.get(i) : null);
                settings.setXtffile(input);
                settings.setItfTransferfile(inputs.usesLocalFiles() && Ili2db.isItfFilename(input));
                if (!runOnce(task, settings)) {
                    break;
                }
            }
        } finally {
            if (fileLogger != null) {
                EhiLogger.getInstance().removeListener(fileLogger);
                fileLogger.close();
            }
            EhiLogger.getInstance().setTraceFilter(true);
        }
    }

    public void executeExport(Ili2duckdbExport task) {
        TransferInputResolver.TransferInputs inputs = inputResolver.resolve(task, false);
        if (inputs.isEmpty()) {
            return;
        }

        List<String> datasetNames = datasetNameResolver.resolve(task, inputs);
        Config settings = duckDbFlavor.createConfig(task);
        commonOptions.apply(task, settings, Config.FC_EXPORT);
        if (task.getExport3().get()) {
            settings.setVer3_export(true);
        }
        if (!task.getExportModels().get().isEmpty()) {
            settings.setExportModels(String.join(";", task.getExportModels().get()));
        }

        try {
            for (int i = 0; i < inputs.executionInputs().size(); i++) {
                String output = inputs.executionInputs().get(i);
                createParentDirectory(Path.of(output));
                settings.setDatasetName(datasetNames != null ? datasetNames.get(i) : null);
                settings.setXtffile(output);
                settings.setItfTransferfile(Ili2db.isItfFilename(output));
                if (!runOnce(task, settings)) {
                    break;
                }
            }
        } finally {
            EhiLogger.getInstance().setTraceFilter(true);
        }
    }

    private boolean runOnce(AbstractIli2DbTask task, Config settings) {
        GretlLogger log = LogEnvironment.getLogger(task.getClass());
        try {
            createParentDirectory(Path.of(settings.getDbfile()));
            if (settings.getLogfile() != null) {
                createParentDirectory(Path.of(settings.getLogfile()));
            }
            Ili2db.readSettingsFromDb(settings);
            Ili2db.run(settings, null);
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
}
