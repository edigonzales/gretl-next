package ch.so.agi.gretl.tasks;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.api.LocaleText;
import ch.so.agi.gretl.internal.ioxwkf.Csv2ExcelEngine;
import ch.so.agi.gretl.internal.ioxwkf.Csv2ExcelEngine.Csv2ExcelRequest;
import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import ch.so.agi.gretl.util.TaskUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

@GretlTaskDoc(name = "Csv2Excel", description = "Converts a CSV file into an XLSX workbook.",
        descriptions = { @LocaleText(locale = "de_CH", value = "Konvertiert eine CSV-Datei in eine XLSX-Arbeitsmappe.") })
public abstract class Csv2Excel extends AbstractCoreGretlTask {
    private final GretlLogger log = LogEnvironment.getLogger(Csv2Excel.class);

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getCsvFile();

    @Input
    public abstract Property<Boolean> getFirstLineIsHeader();

    @Input
    @Optional
    public abstract Property<String> getValueDelimiter();

    @Input
    @Optional
    public abstract Property<String> getValueSeparator();

    @Input
    @Optional
    public abstract Property<String> getEncoding();

    @Input
    @Optional
    public abstract Property<String> getModels();

    @Input
    @Optional
    public abstract Property<String> getModeldir();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    public Csv2Excel() {
        getFirstLineIsHeader().convention(true);
    }

    @GretlDslMethod(required = true, description = "Specifies the CSV file to convert.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die zu konvertierende CSV-Datei fest.") })
    public void csvFile(Object path) {
        setRegularFile(getCsvFile(), path);
    }

    @GretlDslMethod(required = true, description = "Specifies the XLSX output file.",
            descriptions = { @LocaleText(locale = "de_CH", value = "Legt die XLSX-Ausgabedatei fest.") })
    public void outputFile(Object path) {
        setRegularFile(getOutputFile(), path);
    }

    public void firstLineIsHeader(boolean value) { getFirstLineIsHeader().set(value); }
    public void valueDelimiter(String value) { getValueDelimiter().set(value); }
    public void valueSeparator(String value) { getValueSeparator().set(value); }
    public void encoding(String value) { getEncoding().set(value); }
    public void models(String value) { getModels().set(value); }
    public void modeldir(String value) { getModeldir().set(value); }

    @TaskAction
    public void convert() {
        try {
            new Csv2ExcelEngine().convert(new Csv2ExcelRequest(
                    getCsvFile().get().getAsFile().toPath(),
                    getOutputFile().get().getAsFile().toPath(),
                    getFirstLineIsHeader().get(),
                    getValueDelimiter().getOrNull(),
                    getValueSeparator().getOrNull(),
                    getEncoding().getOrNull(),
                    getModels().getOrNull(),
                    getModeldir().getOrNull()));
        } catch (Exception e) {
            log.error("Exception in Csv2Excel task.", e);
            throw TaskUtil.toGradleException(e);
        }
    }
}
