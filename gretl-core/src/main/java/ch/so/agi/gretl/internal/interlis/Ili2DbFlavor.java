package ch.so.agi.gretl.internal.interlis;

import ch.ehi.ili2db.gui.Config;
import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import org.gradle.api.GradleException;

import java.io.File;

public enum Ili2DbFlavor {
    POSTGIS {
        @Override
        Config createConfig(AbstractIli2DbTask task) {
            Config settings = new Config();
            new ch.ehi.ili2pg.PgMain().initConfig(settings);
            return settings;
        }

        @Override
        boolean usesJdbcConnection() {
            return true;
        }
    },
    GEOPACKAGE {
        @Override
        Config createConfig(AbstractIli2DbTask task) {
            Config settings = new Config();
            new ch.ehi.ili2gpkg.GpkgMain().initConfig(settings);
            applyDatabaseFile(task, settings, "jdbc:sqlite:");
            return settings;
        }
    },
    DUCKDB {
        @Override
        Config createConfig(AbstractIli2DbTask task) {
            Config settings = new Config();
            new ch.ehi.ili2duckdb.DuckDBMain().initConfig(settings);
            settings.setStrokeArcs(Config.STROKE_ARCS_ENABLE);
            applyDatabaseFile(task, settings, "jdbc:duckdb:");
            return settings;
        }
    };

    abstract Config createConfig(AbstractIli2DbTask task);

    boolean usesJdbcConnection() {
        return false;
    }

    private static void applyDatabaseFile(AbstractIli2DbTask task, Config settings, String jdbcPrefix) {
        if (!task.getDatabaseFile().isPresent()) {
            throw new GradleException("databaseFile is not configured");
        }
        File file = task.getDatabaseFile().get().getAsFile();
        settings.setDbfile(file.getAbsolutePath());
        settings.setDburl(jdbcPrefix + file.getAbsolutePath());
    }
}
