package ch.so.agi.gretl.doclet;

import ch.so.agi.gretl.doclet.internal.AsciiDocRenderer;
import ch.so.agi.gretl.doclet.internal.TaskClassCollector;
import ch.so.agi.gretl.doclet.internal.TaskDescriptorExtractor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GretlDoclet implements Doclet {
    private Reporter reporter;
    private Path outputDirectory;
    private Locale locale = Locale.ENGLISH;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
        this.locale = locale;
    }

    @Override
    public String getName() {
        return "GretlDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return Set.of(
                new OutputDirectoryOption(),
                new DocletLocaleOption(),
                new IgnoredBooleanOption("-notimestamp", "Ignored Gradle Javadoc timestamp option."));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        if (outputDirectory == null) {
            error("Missing output directory. Pass -d <directory>.");
            return false;
        }

        try {
            TaskDescriptorExtractor extractor = new TaskDescriptorExtractor(
                    environment.getElementUtils(),
                    environment.getDocTrees(),
                    locale);
            List<TaskDescriptor> tasks = new TaskClassCollector().collect(environment).stream()
                    .map(extractor::extract)
                    .toList();
            new AsciiDocRenderer(locale).render(outputDirectory, tasks);
            return true;
        } catch (IOException | RuntimeException e) {
            error("Could not generate GRETL task documentation: " + e.getMessage());
            return false;
        }
    }

    private void error(String message) {
        if (reporter != null) {
            reporter.print(Diagnostic.Kind.ERROR, message);
        }
    }

    private final class OutputDirectoryOption implements Option {
        @Override
        public int getArgumentCount() {
            return 1;
        }

        @Override
        public String getDescription() {
            return "Output directory for generated AsciiDoc files.";
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of("-d");
        }

        @Override
        public String getParameters() {
            return "<directory>";
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            outputDirectory = Path.of(arguments.get(0));
            return true;
        }
    }

    private final class DocletLocaleOption implements Option {
        @Override
        public int getArgumentCount() {
            return 1;
        }

        @Override
        public String getDescription() {
            return "Target locale for generated documentation, e.g. de_CH or de.";
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of("-docletlocale");
        }

        @Override
        public String getParameters() {
            return "<locale>";
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            locale = Locale.forLanguageTag(arguments.get(0).replace('_', '-'));
            return true;
        }
    }

    private static final class IgnoredBooleanOption implements Option {
        private final String name;
        private final String description;

        private IgnoredBooleanOption(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public int getArgumentCount() {
            return 0;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return List.of(name);
        }

        @Override
        public String getParameters() {
            return "";
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            return true;
        }
    }
}
