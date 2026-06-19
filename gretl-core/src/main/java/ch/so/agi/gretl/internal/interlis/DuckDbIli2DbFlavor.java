package ch.so.agi.gretl.internal.interlis;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import org.gradle.api.GradleException;

import java.io.File;

final class DuckDbIli2DbFlavor {

    Config createConfig(AbstractIli2DbTask task) {
        Config settings = new Config();
        new ch.ehi.ili2duckdb.DuckDBMain().initConfig(settings);
        settings.setStrokeArcs(settings.STROKE_ARCS_ENABLE);
        applyDatabase(task, settings);
        return settings;
    }

    private void applyDatabase(AbstractIli2DbTask task, Config settings) {
        if (!task.getDatabaseFile().isPresent()) {
            throw new GradleException("databaseFile is not configured");
        }
        File file = task.getDatabaseFile().get().getAsFile();
        settings.setDbfile(file.getAbsolutePath());
        settings.setDburl("jdbc:duckdb:" + file.getAbsolutePath());
    }
}
