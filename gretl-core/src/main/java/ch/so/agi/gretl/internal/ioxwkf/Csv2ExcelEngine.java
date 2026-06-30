package ch.so.agi.gretl.internal.ioxwkf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Csv2ExcelEngine {

    public void convert(Csv2ExcelRequest request) throws IOException {
        if (request.csvFile() == null || !Files.isRegularFile(request.csvFile())) {
            throw new IllegalArgumentException("csvFile must reference an existing file");
        }
        if (request.outputFile() == null) {
            throw new IllegalArgumentException("outputFile must not be null");
        }
        if (request.outputFile().getParent() != null) {
            Files.createDirectories(request.outputFile().getParent());
        }

        Charset charset = request.encoding() == null
                ? Charset.defaultCharset()
                : Charset.forName(request.encoding());
        List<List<String>> rows = readCsv(request.csvFile(), charset,
                characterOrDefault(request.valueSeparator(), ','),
                characterOrDefault(request.valueDelimiter(), '"'));
        writeWorkbook(request.outputFile(), sheetName(request.csvFile()), rows);
    }

    private List<List<String>> readCsv(Path csvFile, Charset charset, char separator, char delimiter) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, charset);
        List<List<String>> rows = new ArrayList<>();
        for (String line : lines) {
            rows.add(parseLine(line, separator, delimiter));
        }
        return rows;
    }

    private List<String> parseLine(String line, char separator, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == delimiter) {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == delimiter) {
                    value.append(delimiter);
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == separator && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(ch);
            }
        }
        values.add(value.toString());
        return values;
    }

    private void writeWorkbook(Path outputFile, String sheetName, List<List<String>> rows) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outputFile))) {
            entry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            entry(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            entry(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            entry(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="%s" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """.formatted(xml(sheetName)));
            entry(zip, "xl/worksheets/sheet1.xml", worksheet(rows));
        }
    }

    private String worksheet(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("    <row r=\"").append(rowIndex + 1).append("\">\n");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String ref = cellRef(columnIndex, rowIndex + 1);
                builder.append("      <c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                        .append(xml(row.get(columnIndex))).append("</t></is></c>\n");
            }
            builder.append("    </row>\n");
        }
        builder.append("""
                  </sheetData>
                </worksheet>
                """);
        return builder.toString();
    }

    private void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private char characterOrDefault(String value, char defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.length() != 1) {
            throw new IllegalArgumentException("CSV delimiter settings must be single characters");
        }
        return value.charAt(0);
    }

    private String sheetName(Path csvFile) {
        String name = csvFile.getFileName().toString()
                .replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (name.length() > 31) {
            name = name.substring(0, 31);
        }
        return name.isBlank() ? "Sheet1" : name;
    }

    private String cellRef(int columnIndex, int rowIndex) {
        StringBuilder column = new StringBuilder();
        int value = columnIndex + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            column.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return column + Integer.toString(rowIndex);
    }

    private String xml(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '&') {
                builder.append("&amp;");
            } else if (ch == '<') {
                builder.append("&lt;");
            } else if (ch == '>') {
                builder.append("&gt;");
            } else if (ch == '"') {
                builder.append("&quot;");
            } else if (ch >= 0x20 || ch == '\n' || ch == '\r' || ch == '\t') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public record Csv2ExcelRequest(
            Path csvFile,
            Path outputFile,
            boolean firstLineIsHeader,
            String valueDelimiter,
            String valueSeparator,
            String encoding,
            String models,
            String modeldir) {
    }
}
