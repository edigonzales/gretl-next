package ch.so.agi.gretl.doclet.internal;

import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Comparator;
import java.util.List;

public final class TaskClassCollector {
    public List<TypeElement> collect(DocletEnvironment environment) {
        return environment.getIncludedElements().stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .map(TypeElement.class::cast)
                .filter(this::hasTaskDocAnnotation)
                .sorted(Comparator.comparing(element -> element.getQualifiedName().toString()))
                .toList();
    }

    private boolean hasTaskDocAnnotation(TypeElement element) {
        String annotationName = GretlTaskDoc.class.getCanonicalName();
        return element.getAnnotationMirrors().stream()
                .anyMatch(annotation -> annotation.getAnnotationType().toString().equals(annotationName));
    }
}
