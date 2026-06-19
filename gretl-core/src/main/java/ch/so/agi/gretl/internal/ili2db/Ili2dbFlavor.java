package ch.so.agi.gretl.internal.ili2db;

import ch.ehi.ili2db.gui.Config;

public enum Ili2dbFlavor {
    POSTGIS("ili2pg") {
        @Override
        public void init(Config config) {
            new ch.ehi.ili2pg.PgMain().initConfig(config);
        }
    },
    GEOPACKAGE("ili2gpkg") {
        @Override
        public void init(Config config) {
            new ch.ehi.ili2gpkg.GpkgMain().initConfig(config);
        }
    },
    DUCKDB("ili2duckdb") {
        @Override
        public void init(Config config) {
            new ch.ehi.ili2duckdb.DuckDBMain().initConfig(config);
            config.setStrokeArcs(Config.STROKE_ARCS_ENABLE);
        }
    };

    private final String toolName;

    Ili2dbFlavor(String toolName) {
        this.toolName = toolName;
    }

    public String toolName() {
        return toolName;
    }

    public abstract void init(Config config);
}
