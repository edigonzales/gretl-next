package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class Ili2duckdbRoundtripTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "interlis-ili2duckdb-roundtrip"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var job = context.job();
        assertTrue(Files.isRegularFile(job.resolve("build/db/data.duckdb")));
        var export = job.resolve("build/export/export.xml");
        assertTrue(Files.isRegularFile(export)); assertTrue(Files.size(export) > 0);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        var document = factory.newDocumentBuilder().parse(export.toFile());
        String rootName = document.getDocumentElement().getLocalName() == null
                ? document.getDocumentElement().getNodeName() : document.getDocumentElement().getLocalName();
        assertTrue(rootName.equals("TRANSFER"));
        String xml = Files.readString(export); assertTrue(xml.contains("GB2AV")); assertTrue(xml.contains("DATASECTION"));
        assertTrue(xml.replaceAll("(?s)<HEADERSECTION.*?</HEADERSECTION>", "").contains("<"));
    }
}
