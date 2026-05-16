package ch.so.agi.gretl.doclet.internal;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class AnnotationValues {
    private final Elements elements;

    AnnotationValues(Elements elements) {
        this.elements = elements;
    }

    Optional<Map<String, Object>> find(Element element, String annotationClassName) {
        return element.getAnnotationMirrors().stream()
                .filter(annotation -> annotation.getAnnotationType().toString().equals(annotationClassName))
                .findFirst()
                .map(this::valuesWithDefaults);
    }

    private Map<String, Object> valuesWithDefaults(AnnotationMirror annotation) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : elements.getElementValuesWithDefaults(annotation).entrySet()) {
            values.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
        }
        return values;
    }

    static String string(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value == null ? "" : value.toString();
    }

    static boolean bool(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Boolean bool && bool;
    }
}
