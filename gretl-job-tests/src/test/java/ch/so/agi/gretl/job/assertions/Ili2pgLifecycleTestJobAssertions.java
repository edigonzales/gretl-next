package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.fixture.PostgisTestFixtureLease;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class Ili2pgLifecycleTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "interlis-ili2pg-lifecycle"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var job = context.job(); var export = job.resolve("build/export/DatasetA-out.xtf"); var validation = job.resolve("build/validation.log");
        assertTrue(Files.isRegularFile(export) && Files.size(export) > 0); assertTrue(Files.isRegularFile(validation));
        String validationText = Files.readString(validation); assertTrue(validationText.contains("validate done"));
        assertTrue(!validationText.toLowerCase().contains("validation error"));
        var factory = DocumentBuilderFactory.newInstance(); factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false); factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        var document = factory.newDocumentBuilder().parse(export.toFile());
        assertTrue(document.getDocumentElement().getNodeName().equals("TRANSFER"));
        String xml = Files.readString(export); assertTrue(xml.contains("Beispiel2")); assertTrue(xml.contains("DATASECTION"));
        PostgisTestFixtureLease lease = context.requireFixture("postgis", PostgisTestFixtureLease.class);
        try (var connection = lease.openHostConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("select count(*) from " + quote(lease.schema()) + ".boflaechen")) {
            assertTrue(rows.next()); assertEquals(0, rows.getInt(1));
        }
    }
    private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}
