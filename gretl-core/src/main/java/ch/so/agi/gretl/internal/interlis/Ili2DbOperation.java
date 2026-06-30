package ch.so.agi.gretl.internal.interlis;

import ch.ehi.ili2db.gui.Config;

public enum Ili2DbOperation {
    SCHEMA_IMPORT(Config.FC_SCHEMAIMPORT, false),
    IMPORT(Config.FC_IMPORT, true),
    REPLACE(Config.FC_REPLACE, true),
    UPDATE(Config.FC_UPDATE, true),
    DELETE(Config.FC_DELETE, false),
    VALIDATE(Config.FC_VALIDATE, false),
    EXPORT(Config.FC_EXPORT, false);

    private final int function;
    private final boolean importLike;

    Ili2DbOperation(int function, boolean importLike) {
        this.function = function;
        this.importLike = importLike;
    }

    public int function() {
        return function;
    }

    public boolean usesExternalFileLogger() {
        return importLike;
    }
}
