package ch.so.agi.gretl.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Checks SQL-style input files for strict UTF-8 without a byte-order mark. */
public class FileStylingDefinition {
    private static final int BUFFER_SIZE = 8192;

    private FileStylingDefinition() {
    }

    public static void checkForUtf8(File inputfile) throws Exception {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (Reader reader = new InputStreamReader(Files.newInputStream(inputfile.toPath()), decoder)) {
            char[] buffer = new char[BUFFER_SIZE];
            while (reader.read(buffer) != -1) {
                // Reading the complete stream through the strict decoder performs the validation.
            }
        } catch (CharacterCodingException e) {
            throw new GretlException("Wrong encoding (not UTF-8) detected in File " + inputfile.getAbsolutePath());
        }
    }

    public static void checkForBOMInFile(File inputfile) throws Exception {
        try (InputStream input = Files.newInputStream(inputfile.toPath())) {
            byte[] prefix = input.readNBytes(3);
            if (prefix.length == 3
                    && prefix[0] == (byte) 0xEF
                    && prefix[1] == (byte) 0xBB
                    && prefix[2] == (byte) 0xBF) {
                throw new GretlException(GretlException.TYPE_FILE_WITH_BOM, "File includes not allowed BOM");
            }
        }
    }
}
