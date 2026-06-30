package ch.so.agi.gretl.doclet.internal;

import java.util.Locale;

final class Messages {
    private final boolean german;

    Messages(Locale locale) {
        this.german = locale != null && "de".equals(locale.getLanguage());
    }

    String dslMethod() {
        return german ? "DSL-Methode" : "DSL method";
    }

    String required() {
        return german ? "Erforderlich" : "Required";
    }

    String defaultColumn() {
        return german ? "Standard" : "Default";
    }

    String description() {
        return german ? "Beschreibung" : "Description";
    }

    String yes() {
        return german ? "ja" : "yes";
    }

    String no() {
        return german ? "nein" : "no";
    }

    String taskReference() {
        return german ? "Task-Referenz" : "Task Reference";
    }
}
