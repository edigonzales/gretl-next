package ch.so.agi.gretl.lsp.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public sealed interface GretlExpression
        permits StringLiteralExpression, BooleanLiteralExpression, NumberLiteralExpression,
                VariableExpression, MethodCallExpression, ListExpression, MapExpression,
                UnknownExpression {

    org.eclipse.lsp4j.Range range();

    String sourceText();
}
