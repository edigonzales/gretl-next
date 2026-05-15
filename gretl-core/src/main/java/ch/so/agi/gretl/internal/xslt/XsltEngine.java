package ch.so.agi.gretl.internal.xslt;

import ch.so.agi.gretl.logging.GretlLogger;
import ch.so.agi.gretl.logging.LogEnvironment;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.io.FilenameUtils;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class XsltEngine {

    private final GretlLogger log;

    public XsltEngine() {
        this(LogEnvironment.getLogger(XsltEngine.class));
    }

    XsltEngine(GretlLogger log) {
        this.log = log;
    }

    public void execute(XsltRequest request) throws Exception {
        Files.createDirectories(request.outputDirectory());

        log.lifecycle(String.format("Start XslTransformer(Name: %s XmlFiles: %s OutDirectory: %s FileExtension: %s)",
                request.taskName(), request.xmlFiles(), request.outputDirectory(), request.fileExtension()));

        Processor processor = new Processor(false);
        XsltExecutable executable = compile(processor, request);

        for (Path xmlFile : request.xmlFiles()) {
            transform(processor, executable, xmlFile, request.outputDirectory(), request.fileExtension());
        }
    }

    private XsltExecutable compile(Processor processor, XsltRequest request) throws Exception {
        XsltCompiler compiler = processor.newXsltCompiler();
        if (request.xslFile() != null) {
            return compiler.compile(new StreamSource(request.xslFile().toFile()));
        }

        String resourcePath = "xslt/" + request.xslResource();
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("XSLT resource not found: " + resourcePath);
        }
        try (stream) {
            StreamSource source = new StreamSource(stream);
            source.setSystemId(resourcePath);
            return compiler.compile(source);
        }
    }

    private void transform(Processor processor, XsltExecutable executable, Path xmlFile,
            Path outputDirectory, String fileExtension) throws Exception {
        XdmNode source = processor.newDocumentBuilder().build(new StreamSource(xmlFile.toFile()));
        Path outputFile = outputDirectory.resolve(FilenameUtils.getBaseName(xmlFile.getFileName().toString())
                + "." + fileExtension);
        Serializer serializer = processor.newSerializer(outputFile.toFile());
        XsltTransformer transformer = executable.load();
        try {
            transformer.setInitialContextNode(source);
            transformer.setDestination(serializer);
            transformer.transform();
        } finally {
            transformer.close();
        }
    }
}
