package ch.so.agi.gretl.doclet.internal;

import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.ParameterDescriptor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsciiDocRendererTest {
    @Test
    void rendersStrongMethodNameGenericsWildcardsAndAsciiVarargs() {
        DslMethodDescriptor method = new DslMethodDescriptor(
                "sqlParameterSets",
                "void",
                List.of(new ParameterDescriptor(
                        "parameterSets",
                        "Map<String, ? extends Number>...",
                        true)),
                false,
                "",
                "SQL parameters");
        TaskDescriptor task = new TaskDescriptor(
                "Fixture",
                "example.Fixture",
                "example",
                "",
                List.of(method));

        String rendered = new AsciiDocRenderer(Locale.GERMAN).renderTaskTable(task);

        assertTrue(rendered.contains(
                "[.dsl-signature]#*sqlParameterSets*(Map<String, ? extends Number>\\... parameterSets)#"));
        assertTrue(rendered.contains("| [.acronym]#SQL# parameters"));
        assertTrue(rendered.contains("| [.optional]#nein#"));
    }
}
