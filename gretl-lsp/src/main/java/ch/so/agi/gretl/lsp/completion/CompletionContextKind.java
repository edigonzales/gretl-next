package ch.so.agi.gretl.lsp.completion;

public enum CompletionContextKind {
    TASK_TYPE,
    INSIDE_GRETL_TASK_BODY,
    DEPENDENCY_TASK_NAME,
    IMPORT,
    FILE_PATH,
    SQL_PARAMETER_NAME,
    TOP_LEVEL,
    UNKNOWN
}
