package ch.so.agi.gretl.doclet.internal;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor14;
import java.util.List;
import java.util.stream.Collectors;

final class TypeNameFormatter extends SimpleTypeVisitor14<String, Boolean> {
    String methodSignature(ExecutableElement method) {
        return method.getSimpleName() + "(" + parameters(method) + ")";
    }

    String returnType(ExecutableElement method) {
        return format(method.getReturnType());
    }

    String format(TypeMirror type) {
        return type.accept(this, false);
    }

    private String parameters(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        return parameters.stream()
                .map(parameter -> parameterSignature(method, parameter, parameters.indexOf(parameter)))
                .collect(Collectors.joining(", "));
    }

    private String parameterSignature(ExecutableElement method, VariableElement parameter, int index) {
        TypeMirror type = parameter.asType();
        boolean varargs = method.isVarArgs() && index == method.getParameters().size() - 1;
        String renderedType;
        if (varargs && type instanceof ArrayType arrayType) {
            renderedType = format(arrayType.getComponentType()) + "...";
        } else {
            renderedType = format(type);
        }
        return renderedType + " " + parameter.getSimpleName();
    }

    @Override
    public String visitDeclared(DeclaredType type, Boolean unused) {
        Element element = type.asElement();
        String name = element.getSimpleName().toString();
        if (type.getTypeArguments().isEmpty()) {
            return name;
        }
        String arguments = type.getTypeArguments().stream()
                .map(this::format)
                .collect(Collectors.joining(", "));
        return name + "<" + arguments + ">";
    }

    @Override
    public String visitArray(ArrayType type, Boolean unused) {
        return format(type.getComponentType()) + "[]";
    }

    @Override
    public String visitNoType(NoType type, Boolean unused) {
        if (type.getKind() == TypeKind.VOID) {
            return "void";
        }
        return type.toString();
    }

    @Override
    public String visitTypeVariable(TypeVariable type, Boolean unused) {
        return type.asElement().getSimpleName().toString();
    }

    @Override
    public String visitWildcard(WildcardType type, Boolean unused) {
        if (type.getExtendsBound() != null) {
            return "? extends " + format(type.getExtendsBound());
        }
        if (type.getSuperBound() != null) {
            return "? super " + format(type.getSuperBound());
        }
        return "?";
    }

    @Override
    protected String defaultAction(TypeMirror type, Boolean unused) {
        return type.toString();
    }
}
