package ch.so.agi.gretl.doclet.internal;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;

import javax.lang.model.element.Element;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class JavadocToAsciiDoc {
    private static final Pattern CODE = Pattern.compile("\\{@code\\s+([^}]+)}");
    private static final Pattern LINK = Pattern.compile("\\{@link(?:plain)?\\s+([^} ]+)(?:\\s+([^}]+))?}");

    private final DocTrees docTrees;

    JavadocToAsciiDoc(DocTrees docTrees) {
        this.docTrees = docTrees;
    }

    String convert(Element element) {
        DocCommentTree tree = docTrees.getDocCommentTree(element);
        if (tree == null) {
            return "";
        }
        return convert(tree.getFullBody().stream()
                .map(Object::toString)
                .collect(Collectors.joining()));
    }

    String convert(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String converted = text.strip();
        converted = CODE.matcher(converted).replaceAll(match -> "`" + match.group(1).strip() + "`");
        converted = LINK.matcher(converted).replaceAll(match -> {
            String label = match.group(2) == null ? simpleReferenceName(match.group(1)) : match.group(2).strip();
            return "`" + label + "`";
        });
        converted = converted.replace("<p>", "\n\n").replace("</p>", "");
        converted = converted.replace("<ul>", "\n").replace("</ul>", "\n");
        converted = converted.replace("<li>", "\n* ").replace("</li>", "");
        return converted.replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static String simpleReferenceName(String reference) {
        int hash = reference.lastIndexOf('#');
        int dot = reference.lastIndexOf('.');
        int start = Math.max(hash, dot) + 1;
        return reference.substring(start);
    }
}
