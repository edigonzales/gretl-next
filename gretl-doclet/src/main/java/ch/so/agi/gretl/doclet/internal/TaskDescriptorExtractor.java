package ch.so.agi.gretl.doclet.internal;

import ch.so.agi.gretl.doclet.api.GretlDslMethod;
import ch.so.agi.gretl.doclet.api.GretlTaskDoc;
import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.ParameterDescriptor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;
import com.sun.source.util.DocTrees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class TaskDescriptorExtractor {
    private static final Comparator<DslMethodDescriptor> DSL_METHOD_ORDER =
            Comparator.comparing(DslMethodDescriptor::name)
                    .thenComparingInt(method -> method.parameters().size())
                    .thenComparing(TaskDescriptorExtractor::parameterTypeKey);

    private final Elements elements;
    private final AnnotationValues annotations;
    private final JavadocToAsciiDoc javadoc;
    private final TypeNameFormatter typeNames = new TypeNameFormatter();
    private final Locale locale;

    public TaskDescriptorExtractor(Elements elements, DocTrees docTrees, Locale locale) {
        this.elements = elements;
        this.annotations = new AnnotationValues(elements);
        this.javadoc = new JavadocToAsciiDoc(docTrees);
        this.locale = locale;
    }

    public TaskDescriptor extract(TypeElement type) {
        Map<String, Object> taskDoc = annotations.find(type, GretlTaskDoc.class.getCanonicalName())
                .orElseThrow(() -> new IllegalArgumentException(type + " is missing @GretlTaskDoc"));
        Map<String, String> taskLocaleMap = annotations.localeMap(taskDoc, "descriptions");
        String name = firstNonBlank(AnnotationValues.string(taskDoc, "name"), type.getSimpleName().toString());
        String description = resolveDescription(
                AnnotationValues.string(taskDoc, "description"),
                taskLocaleMap,
                type);
        List<DslMethodDescriptor> methods = elements.getAllMembers(type).stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(method -> method.getModifiers().contains(Modifier.PUBLIC))
                .filter(this::hasDslMethodAnnotation)
                .map(this::methodDescriptor)
                .sorted(DSL_METHOD_ORDER)
                .toList();

        return new TaskDescriptor(
                name,
                type.getQualifiedName().toString(),
                packageName(type),
                description,
                methods);
    }

    private boolean hasDslMethodAnnotation(ExecutableElement method) {
        return annotations.find(method, GretlDslMethod.class.getCanonicalName()).isPresent();
    }

    private DslMethodDescriptor methodDescriptor(ExecutableElement method) {
        Map<String, Object> values = annotations.find(method, GretlDslMethod.class.getCanonicalName())
                .orElseThrow();
        Map<String, String> localeMap = annotations.localeMap(values, "descriptions");
        String description = resolveDescription(
                AnnotationValues.string(values, "description"),
                localeMap,
                method);
        List<ParameterDescriptor> parameters = new java.util.ArrayList<>();
        for (int i = 0; i < method.getParameters().size(); i++) {
            parameters.add(parameterDescriptor(method, method.getParameters().get(i), i));
        }
        return new DslMethodDescriptor(
                method.getSimpleName().toString(),
                typeNames.returnType(method),
                parameters,
                AnnotationValues.bool(values, "required"),
                AnnotationValues.string(values, "defaultValue"),
                description);
    }

    private static String parameterTypeKey(DslMethodDescriptor method) {
        return method.parameters().stream()
                .map(ParameterDescriptor::type)
                .collect(Collectors.joining(","));
    }

    private ParameterDescriptor parameterDescriptor(ExecutableElement method, VariableElement parameter, int index) {
        boolean varargs = method.isVarArgs() && index == method.getParameters().size() - 1;
        String type = typeNames.format(parameter.asType());
        if (varargs && parameter.asType() instanceof javax.lang.model.type.ArrayType arrayType) {
            type = typeNames.format(arrayType.getComponentType()) + "...";
        }
        return new ParameterDescriptor(parameter.getSimpleName().toString(), type, varargs);
    }

    private static String packageName(TypeElement type) {
        Element element = type.getEnclosingElement();
        while (element != null && element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }
        return element == null ? "" : element.toString();
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String resolveDescription(String defaultDescription, Map<String, String> localeMap, Element element) {
        String localized = resolveLocalized(localeMap);
        if (localized != null) {
            return javadoc.convert(localized);
        }
        if (defaultDescription != null && !defaultDescription.isBlank()) {
            return javadoc.convert(defaultDescription);
        }
        return javadoc.convert(element);
    }

    private String resolveLocalized(Map<String, String> localeMap) {
        String result = localeMap.get(locale.toLanguageTag());
        if (result != null) return result;
        result = localeMap.get(locale.toString());
        if (result != null) return result;
        result = localeMap.get(locale.getLanguage());
        return result;
    }
}
