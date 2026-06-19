package ch.so.agi.gretl.internal.ili2db;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.FileListener;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.interlis.iox_j.logging.FileLogger;
import ch.so.agi.gretl.internal.sql.DatabaseSpec;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Ili2dbExecutionEngine {

    public void execute(Ili2dbRequest request) throws Exception {
        validate(request);
        FileListener fileLogger = null;
        if (request.operation().usesExternalFileLogger() && request.logFile() != null) {
            if (request.logFile().getParent() != null) {
                Files.createDirectories(request.logFile().getParent());
            }
            fileLogger = new FileLogger(request.logFile().toFile());
            EhiLogger.getInstance().addListener(fileLogger);
        }
        try {
            executeOperations(request);
        } finally {
            if (fileLogger != null) {
                EhiLogger.getInstance().removeListener(fileLogger);
                fileLogger.close();
            }
        }
    }

    private void executeOperations(Ili2dbRequest request) throws Exception {
        Config config = request.config();
        if (!request.transfers().isEmpty()) {
            for (Ili2dbTransfer transfer : request.transfers()) {
                config.setXtffile(transfer.fileName());
                config.setDatasetName(transfer.datasetName());
                config.setItfTransferfile(Ili2db.isItfFilename(transfer.fileName()));
                runOnce(request);
            }
            return;
        }

        if (!request.datasets().isEmpty()) {
            for (String dataset : request.datasets()) {
                config.setDatasetName(dataset);
                runOnce(request);
            }
            return;
        }

        runOnce(request);
    }

    private void runOnce(Ili2dbRequest request) throws Exception {
        try {
            if (request.flavor() == Ili2dbFlavor.POSTGIS) {
                runPostgis(request);
            } else {
                runFileDatabase(request);
            }
        } catch (Exception e) {
            if (e instanceof Ili2dbException && !request.failOnException()) {
                return;
            }
            throw e;
        }
    }

    private void runPostgis(Ili2dbRequest request) throws Exception {
        DatabaseSpec database = request.database();
        try (Connection connection = connect(database)) {
            request.config().setJdbcConnection(connection);
            Ili2db.readSettingsFromDb(request.config());
            Ili2db.run(request.config(), null);
            connection.commit();
        }
    }

    private void runFileDatabase(Ili2dbRequest request) throws Exception {
        if (request.dbFile() == null) {
            throw new IllegalArgumentException("dbfile must not be null");
        }
        if (request.dbFile().getParent() != null) {
            Files.createDirectories(request.dbFile().getParent());
        }
        request.config().setDbfile(request.dbFile().toString());
        if (request.flavor() == Ili2dbFlavor.GEOPACKAGE) {
            request.config().setDburl("jdbc:sqlite:" + request.config().getDbfile());
        } else {
            request.config().setDburl("jdbc:duckdb:" + request.config().getDbfile());
        }
        Ili2db.readSettingsFromDb(request.config());
        Ili2db.run(request.config(), null);
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

    private static void validate(Ili2dbRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.flavor() == null) {
            throw new IllegalArgumentException("flavor must not be null");
        }
        if (request.operation() == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (request.config() == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (request.flavor() == Ili2dbFlavor.POSTGIS && request.database() == null) {
            throw new IllegalArgumentException("database must not be null");
        }
    }
}
