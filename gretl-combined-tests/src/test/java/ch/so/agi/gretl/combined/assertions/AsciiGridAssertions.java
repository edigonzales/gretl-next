package ch.so.agi.gretl.combined.assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AsciiGridAssertions {
    public record AsciiGrid(int columns, int rows, double cellSize, double noData, double[][] values,
                            Map<String, String> headers) {
    }

    public static AsciiGrid read(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.size() < 6) {
                throw new AssertionError("ASCII Grid has fewer than six header lines: " + path);
            }
            Map<String, String> headers = new LinkedHashMap<>();
            for (int i = 0; i < 6; i++) {
                String[] parts = lines.get(i).trim().split("\\s+", 2);
                if (parts.length != 2) {
                    throw new AssertionError("Malformed ASCII Grid header: " + lines.get(i));
                }
                headers.put(parts[0].toLowerCase(Locale.ROOT), parts[1]);
            }
            int columns = Integer.parseInt(headers.get("ncols"));
            int rows = Integer.parseInt(headers.get("nrows"));
            double cellSize = Double.parseDouble(headers.get("cellsize"));
            double noData = Double.parseDouble(headers.get("nodata_value"));
            double[][] values = new double[rows][columns];
            int lineIndex = 6;
            for (int row = 0; row < rows; row++) {
                if (lineIndex >= lines.size()) {
                    throw new AssertionError("ASCII Grid is missing row " + row);
                }
                String[] cells = lines.get(lineIndex++).trim().split("\\s+");
                if (cells.length != columns) {
                    throw new AssertionError("ASCII Grid row " + row + " has " + cells.length
                            + " cells, expected " + columns);
                }
                for (int column = 0; column < columns; column++) {
                    values[row][column] = Double.parseDouble(cells[column]);
                }
            }
            return new AsciiGrid(columns, rows, cellSize, noData, values, headers);
        } catch (IOException | NumberFormatException e) {
            throw new AssertionError("Cannot read ASCII Grid " + path, e);
        }
    }

    public static void assertDimensions(AsciiGrid grid, int columns, int rows) {
        assertEquals(columns, grid.columns());
        assertEquals(rows, grid.rows());
    }

    public static void assertCellSize(AsciiGrid grid, double expected) {
        assertEquals(expected, grid.cellSize(), 0.000001);
    }

    public static void assertNoData(AsciiGrid grid, double expected) {
        assertEquals(expected, grid.noData(), 0.000001);
    }

    public static void assertValues(AsciiGrid grid, double[][] expected) {
        assertEquals(expected.length, grid.values().length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], grid.values()[row], 0.000001,
                    "Unexpected ASCII Grid values in row " + row);
        }
    }

    private AsciiGridAssertions() {
    }
}
