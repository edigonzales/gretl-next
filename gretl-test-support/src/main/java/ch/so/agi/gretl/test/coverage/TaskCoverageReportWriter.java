package ch.so.agi.gretl.test.coverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TaskCoverageReportWriter {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(CoverageVerificationReport report, Path jsonFile, Path adocFile) {
        try {
            Files.createDirectories(jsonFile.toAbsolutePath().normalize().getParent());
            Files.createDirectories(adocFile.toAbsolutePath().normalize().getParent());
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("successful", report.successful());
            json.put("errors", report.errors());
            json.put("directJobExecution", report.directJobExecution());
            json.put("structuralContractOnly", report.structuralContractOnly());
            json.put("dependencyPresentOnly", report.dependencyPresentOnly());
            json.put("noCanonicalJobTrace", report.noCanonicalJobTrace());
            json.put("notApplicable", report.notApplicable());
            json.put("missingBackendExecutions", report.missingBackendExecutions());
            Files.writeString(jsonFile, JSON.writeValueAsString(json) + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.writeString(adocFile, asciidoc(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write task coverage report", e);
        }
    }

    private String asciidoc(CoverageVerificationReport report) {
        StringBuilder output = new StringBuilder();
        output.append("= GRETL Canonical Job Coverage\n\n");
        output.append("Verification: ").append(report.successful() ? "PASS" : "FAIL").append("\n\n");
        int publicTaskCount = report.directJobExecution().size()
                + report.structuralContractOnly().size()
                + report.dependencyPresentOnly().size()
                + report.noCanonicalJobTrace().size()
                + report.notApplicable().size();
        output.append("Public task inventory: ").append(publicTaskCount).append(" classified tasks.\n\n");
        output.append("Canonical job traces: ").append(report.directJobExecution().size())
                .append('/').append(publicTaskCount).append(" public tasks.\n\n");
        section(output, "DIRECT_JOB_EXECUTION", report.directJobExecution());
        section(output, "STRUCTURAL_CONTRACT_ONLY", report.structuralContractOnly());
        section(output, "DEPENDENCY_PRESENT_ONLY", report.dependencyPresentOnly());
        section(output, "NO_CANONICAL_JOB_TRACE", report.noCanonicalJobTrace());
        section(output, "NOT_APPLICABLE", report.notApplicable());
        section(output, "Missing required backend traces", report.missingBackendExecutions());
        section(output, "Verification errors", report.errors());
        return output.toString();
    }

    private void section(StringBuilder output, String title, java.util.List<String> values) {
        output.append("== ").append(title).append("\n\n");
        if (values.isEmpty()) {
            output.append("None.\n\n");
        } else {
            values.stream().sorted().forEach(value -> output.append("* ").append(value).append('\n'));
            output.append('\n');
        }
    }
}
