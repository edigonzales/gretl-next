package ch.so.agi.gretl.lsp.diagnostics;

public enum DiagnosticCode {

    MISSING_REQUIRED_PROPERTY("GRETL1001", "Pflichtparameter `%s` fehlt f\u00fcr Task `%s`."),
    UNKNOWN_PROPERTY("GRETL1002", "Unbekannte Property `%s`. Meintest du `%s`?"),
    UNKNOWN_PROPERTY_NO_SUGGESTION("GRETL1002", "Unbekannte Property `%s`."),
    WRONG_ARGUMENT_COUNT("GRETL1003", "`%s` erwartet %d Argumente: %s."),
    UNKNOWN_TASK_TYPE("GRETL1004", "Task-Typ `%s` ist nicht im GRETL-Metadatenmanifest. Falls es ein externer Gradle-Task ist, kann diese Warnung ignoriert werden."),
    DYNAMIC_TASK_TYPE("GRETL1004", "GRETL-LSP kann dynamisch berechnete Task-Typen nicht vollst\u00e4ndig analysieren."),
    UNKNOWN_DEPENDENCY("GRETL1101", "Task `%s` existiert nicht. Meintest du `%s`?"),
    UNKNOWN_DEPENDENCY_NO_SUGGESTION("GRETL1101", "Task `%s` existiert nicht in diesem Dokument."),
    UNKNOWN_DEFAULT_TASK("GRETL1102", "defaultTasks verweist auf unbekannten Task `%s`. Meintest du `%s`?"),
    UNKNOWN_DEFAULT_TASK_NO_SUGGESTION("GRETL1102", "defaultTasks verweist auf unbekannten Task `%s`."),
    DUPLICATE_TASK_NAME("GRETL1103", "Task-Name `%s` ist mehrfach definiert."),
    LEGACY_DSL("GRETL1201", "Alte GRETL-DSL-Schreibweise. Quick Fix kann zu `%s` migrieren.");

    private final String code;
    private final String messageTemplate;

    DiagnosticCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String code() {
        return code;
    }

    public String format(Object... args) {
        return code + ": " + String.format(messageTemplate, args);
    }
}
