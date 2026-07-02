package ch.so.agi.gretl.lsp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GretlImport(String importPath) {

    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("import\\s+([\\w.]+(?:\\*)?)");

    public boolean isStarImport() {
        return importPath.endsWith(".*");
    }

    public String packagePrefix() {
        if (isStarImport()) {
            return importPath.substring(0, importPath.length() - 2);
        }
        return importPath;
    }

    public static List<GretlImport> parseAll(String text) {
        List<GretlImport> imports = new ArrayList<>();
        if (text == null) {
            return imports;
        }
        Matcher m = IMPORT_PATTERN.matcher(text);
        while (m.find()) {
            imports.add(new GretlImport(m.group(1)));
        }
        return imports;
    }
}
