package ch.so.agi.gretl.internal.interlis;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.interlis.ili2c.gui.UserSettings;
import ch.so.agi.gretl.tasks.AbstractIli2DbTask;

final class Ili2DbOptionApplier {

    void apply(AbstractIli2DbTask task, Config settings, Ili2DbOperation operation) {
        settings.setFunction(operation.function());
        EhiLogger.getInstance().setTraceFilter(!task.getTrace().get());

        if (task.getProxy().isPresent()) {
            settings.setValue(UserSettings.HTTP_PROXY_HOST, task.getProxy().get());
        }
        if (task.getProxyPort().isPresent()) {
            settings.setValue(UserSettings.HTTP_PROXY_PORT, task.getProxyPort().get().toString());
        }
        if (task.getSchema().isPresent()) {
            settings.setDbschema(task.getSchema().get());
        }
        if (!task.getModelDirectories().get().isEmpty()) {
            settings.setModeldir(String.join(";", task.getModelDirectories().get()));
        }
        if (!task.getModelNames().get().isEmpty()) {
            settings.setModels(String.join(";", task.getModelNames().get()));
        }
        if (!task.getBaskets().get().isEmpty()) {
            settings.setBaskets(String.join(";", task.getBaskets().get()));
        }
        if (!task.getTopics().get().isEmpty()) {
            settings.setTopics(String.join(";", task.getTopics().get()));
        }
        if (task.getImportTid().get()) {
            settings.setImportTid(true);
        }
        if (task.getExportTid().get()) {
            settings.setExportTid(true);
        }
        if (task.getImportBid().get()) {
            settings.setImportBid(true);
        }
        if (task.getPreScript().isPresent()) {
            settings.setPreScript(task.getPreScript().get().getAsFile().getAbsolutePath());
        }
        if (task.getPostScript().isPresent()) {
            settings.setPostScript(task.getPostScript().get().getAsFile().getAbsolutePath());
        }
        if (task.getDeleteData().get()) {
            settings.setDeleteMode(Config.DELETE_DATA);
        }
        if (!operation.usesExternalFileLogger() && task.getLogFile().isPresent()) {
            settings.setLogfile(task.getLogFile().get().getAsFile().getAbsolutePath());
        }
        if (task.getValidConfigFile().isPresent()) {
            settings.setValidConfigFile(task.getValidConfigFile().get().getAsFile().getAbsolutePath());
        }
        if (task.getDisableValidation().get()) {
            settings.setValidation(false);
        }
        if (task.getDisableAreaValidation().get()) {
            settings.setDisableAreaValidation(true);
        }
        if (task.getForceTypeValidation().get()) {
            settings.setOnlyMultiplicityReduction(true);
        }
        if (task.getStrokeArcs().get()) {
            settings.setStrokeArcs(Config.STROKE_ARCS_ENABLE);
        }
        if (task.getSkipPolygonBuilding().get()) {
            Ili2db.setSkipPolygonBuilding(settings);
        }
        if (task.getSkipGeometryErrors().get()) {
            settings.setSkipGeometryErrors(true);
        }
        if (task.getIligml20().get()) {
            settings.setTransferFileFormat(Config.ILIGML20);
        }
        if (task.getDisableRounding().get()) {
            settings.setDisableRounding(true);
        }
    }
}
