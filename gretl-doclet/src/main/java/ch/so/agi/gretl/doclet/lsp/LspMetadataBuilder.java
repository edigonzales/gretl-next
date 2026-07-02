package ch.so.agi.gretl.doclet.lsp;

import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.ParameterDescriptor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LspMetadataBuilder {

    private static final String SCHEMA_VERSION = "1.0.0";
    private static final String REPOSITORY = "https://github.com/sogis/gretl";
    private static final String DOCLET = "gretl-doclet";

    private static final Set<String> SQL_PARAMETER_PROVIDERS = Set.of("sqlParameters");
    private static final Set<String> FILE_METHOD_SUFFIXES = Set.of("File", "Files", "Dir", "Directory", "Path");

    public LspMetadataDocument build(List<TaskDescriptor> taskDescriptors, String gretlVersion, String commit) {
        List<LspTaskMetadata> tasks = taskDescriptors.stream()
                .sorted(Comparator.comparing(TaskDescriptor::name))
                .map(this::toTaskMetadata)
                .toList();

        return new LspMetadataDocument(
                SCHEMA_VERSION,
                Instant.now().toString(),
                gretlVersion,
                new LspMetadataSource(REPOSITORY, DOCLET, commit),
                tasks);
    }

    private LspTaskMetadata toTaskMetadata(TaskDescriptor descriptor) {
        String className = descriptor.className();
        String simpleName = descriptor.name();
        String category = inferCategory(simpleName);
        String status = inferStatus(simpleName);

        List<LspPropertyMetadata> properties = descriptor.methods().stream()
                .collect(Collectors.groupingBy(
                        DslMethodDescriptor::name,
                        java.util.LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .sorted(Comparator.comparing(methods -> methods.get(0).name()))
                .map(methods -> toPropertyMetadataFromOverloads(methods, descriptor))
                .toList();

        return new LspTaskMetadata(
                simpleName,
                className,
                simpleName,
                category,
                status,
                descriptor.description(),
                null,
                List.of(),
                properties);
    }

    private LspPropertyMetadata toPropertyMetadataFromOverloads(List<DslMethodDescriptor> overloads, TaskDescriptor task) {
        DslMethodDescriptor representative = overloads.stream()
                .max(Comparator.comparingInt(m -> m.parameters().size()))
                .orElseThrow();

        String propName = representative.name();
        String displayName = propName;
        String kind = "dsl-method-and-property";
        String valueType = inferValueType(representative);
        String javaType = inferJavaType(representative);

        List<LspAcceptedForm> acceptedForms = new ArrayList<>();
        for (DslMethodDescriptor method : overloads) {
            acceptedForms.addAll(generateAcceptedForms(method));
        }

        LspFileMetadata file = inferFileMetadata(representative);
        boolean sqlParamProvider = SQL_PARAMETER_PROVIDERS.contains(propName);

        LspCompletionMetadata completion = new LspCompletionMetadata(
                propName,
                buildCompletionDetail(representative),
                buildSortText(propName, representative.required()));

        return new LspPropertyMetadata(
                propName,
                displayName,
                kind,
                valueType,
                javaType,
                representative.required(),
                false,
                representative.description(),
                file,
                acceptedForms,
                null,
                sqlParamProvider,
                completion);
    }

    private String inferCategory(String taskName) {
        String lower = taskName.toLowerCase();
        if (lower.startsWith("sql") || lower.startsWith("db2db") || lower.startsWith("duckdb")
                || lower.endsWith("import") || lower.endsWith("export") || lower.endsWith("validator")) {
            return "database";
        }
        if (lower.startsWith("ftp")) {
            return "filetransfer";
        }
        if (lower.startsWith("s3")) {
            return "storage";
        }
        if (lower.startsWith("ili")) {
            return "interlis";
        }
        if (lower.startsWith("csv")) {
            return "database";
        }
        if (lower.startsWith("gpkg")) {
            return "database";
        }
        if (lower.startsWith("shp")) {
            return "database";
        }
        if (lower.startsWith("json")) {
            return "database";
        }
        if (lower.equals("gzip") || lower.equals("xsltransformer") || lower.startsWith("xsl")) {
            return "transformation";
        }
        if (lower.equals("curl")) {
            return "network";
        }
        if (lower.startsWith("av2")) {
            return "conversion";
        }
        if (lower.startsWith("gpkg2")) {
            return "conversion";
        }
        return "other";
    }

    private String inferStatus(String taskName) {
        return "stable";
    }

    private List<LspAcceptedForm> generateAcceptedForms(DslMethodDescriptor method) {
        List<LspAcceptedForm> forms = new ArrayList<>();
        String propName = method.name();

        String methodCallSignature = buildMethodCallSignature(method);
        String methodCallInsert = buildMethodCallInsertText(method);

        forms.add(new LspAcceptedForm(
                "method-call",
                methodCallSignature,
                methodCallInsert,
                method.parameters().isEmpty() ? null : method.parameters().size(),
                false));

        forms.add(new LspAcceptedForm(
                "assignment",
                buildAssignmentSignature(method),
                buildAssignmentInsertText(method),
                null,
                true));

        return forms;
    }

    private String buildMethodCallSignature(DslMethodDescriptor method) {
        StringBuilder sb = new StringBuilder(method.name());
        List<ParameterDescriptor> params = method.parameters();
        if (params.isEmpty()) {
            return sb.toString();
        }
        if (params.size() == 1 && isFileObjectParam(params.get(0))) {
            String fileHint = isVarargs(params.get(0)) ? "files('...')" : "file('...')";
            return sb.append(" ").append(fileHint).toString();
        }
        if (params.size() == 1 && params.get(0).type().equals("Map<String, ?>")) {
            return sb.append(" key: 'value'").toString();
        }
        sb.append(" ");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).name());
        }
        return sb.toString();
    }

    private String buildMethodCallInsertText(DslMethodDescriptor method) {
        StringBuilder sb = new StringBuilder(method.name());
        List<ParameterDescriptor> params = method.parameters();
        if (params.isEmpty()) {
            return sb.toString();
        }
        if (params.size() == 1 && isFileObjectParam(params.get(0))) {
            return sb.append(" files('${1:file}')").toString();
        }
        if (params.size() == 1 && params.get(0).type().equals("Map<String, ?>")) {
            return sb.append(" ${1:key}: ${2:'value'}").toString();
        }
        sb.append(" ");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("${").append(i + 1).append(":").append(params.get(i).name()).append("}");
        }
        return sb.toString();
    }

    private String buildAssignmentSignature(DslMethodDescriptor method) {
        StringBuilder sb = new StringBuilder(method.name()).append(" = ");
        List<ParameterDescriptor> params = method.parameters();
        if (params.isEmpty()) {
            return sb.append("value").toString();
        }
        if (params.size() == 1 && isFileObjectParam(params.get(0))) {
            return sb.append(isVarargs(params.get(0)) ? "files('...')" : "file('...')").toString();
        }
        if (params.size() > 1) {
            sb.append("[");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(params.get(i).name());
            }
            sb.append("]");
            return sb.toString();
        }
        return sb.append("value").toString();
    }

    private String buildAssignmentInsertText(DslMethodDescriptor method) {
        return method.name() + " = ${1:value}";
    }

    private String inferValueType(DslMethodDescriptor method) {
        String name = method.name().toLowerCase();
        if (name.endsWith("files") || name.endsWith("file")) {
            return "FileCollection";
        }
        if (name.equals("sqlparameters") || name.equals("sqlparametersets")) {
            return "Object";
        }
        if (method.parameters().size() >= 2) {
            return "Connector";
        }
        if (method.parameters().isEmpty()) {
            return "void";
        }
        String paramType = method.parameters().get(0).type();
        if (paramType.equals("String") || paramType.equals("String...")) {
            return "String";
        }
        if (paramType.equals("boolean") || paramType.equals("Boolean")) {
            return "Boolean";
        }
        if (paramType.startsWith("int") || paramType.startsWith("long") || paramType.startsWith("double")) {
            return "Number";
        }
        if (paramType.equals("Object") || paramType.equals("Object...")) {
            return "Object";
        }
        if (paramType.contains("Map")) {
            return "Object";
        }
        if (paramType.contains("List") || paramType.contains("Iterable")) {
            return "Object";
        }
        return "Object";
    }

    private String inferJavaType(DslMethodDescriptor method) {
        if (method.parameters().size() == 1) {
            String type = method.parameters().get(0).type();
            if (isVarargs(method.parameters().get(0))) {
                return "Property<" + type.replace("...", "") + ">";
            }
            return "Property<" + type + ">";
        }
        if (method.parameters().size() >= 2) {
            return "Property<String>";
        }
        return "Property<Object>";
    }

    private LspFileMetadata inferFileMetadata(DslMethodDescriptor method) {
        String name = method.name().toLowerCase();
        if (name.equals("sqlfiles")) {
            return new LspFileMetadata("input", List.of(".sql"), true, true);
        }
        if (name.endsWith("files") || name.endsWith("file") || name.endsWith("dir") || name.endsWith("directory")) {
            return new LspFileMetadata("input", List.of(), true, true);
        }
        if (name.equals("databases")) {
            return new LspFileMetadata("input", List.of(".db", ".duckdb", ".sqlite"), false, true);
        }
        return null;
    }

    private String buildCompletionDetail(DslMethodDescriptor method) {
        StringBuilder sb = new StringBuilder();
        if (method.required()) {
            sb.append("Pflicht");
        } else {
            sb.append("Optional");
        }
        sb.append(" \u00b7 ").append(inferValueType(method));
        return sb.toString();
    }

    private String buildSortText(String name, boolean required) {
        if (required) {
            return "0100_" + name;
        }
        return "0200_" + name;
    }

    private static boolean isFileObjectParam(ParameterDescriptor param) {
        String type = param.type();
        return type.equals("Object") || type.equals("Object...")
                || type.equals("Object[]") || type.equals("java.lang.Object")
                || type.equals("java.lang.Object[]");
    }

    private static boolean isVarargs(ParameterDescriptor param) {
        return param.varargs() || param.type().endsWith("...");
    }
}
