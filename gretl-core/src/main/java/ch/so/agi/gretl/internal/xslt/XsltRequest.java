package ch.so.agi.gretl.internal.xslt;

import java.nio.file.Path;
import java.util.List;

public record XsltRequest(
        String taskName,
        Path xslFile,
        String xslResource,
        List<Path> xmlFiles,
        Path outputDirectory,
        String fileExtension
) {

    public XsltRequest {
        if ((xslFile == null && (xslResource == null || xslResource.isBlank()))
                || (xslFile != null && xslResource != null && !xslResource.isBlank())) {
            throw new IllegalArgumentException("Configure either xslFile or xslResource");
        }
        if (xmlFiles == null || xmlFiles.isEmpty()) {
            throw new IllegalArgumentException("xmlFiles must not be empty");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory must not be null");
        }
        if (fileExtension == null || fileExtension.isBlank()) {
            throw new IllegalArgumentException("fileExtension must not be null or blank");
        }
        taskName = (taskName == null || taskName.isBlank()) ? "XslTransformer" : taskName;
        xmlFiles = List.copyOf(xmlFiles);
    }
}
