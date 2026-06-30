package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Gpkg2DxfFunctionalTest extends CoreFunctionalTestSupport {
    private static final Charset DXF_CHARSET = Charset.forName("ISO-8859-1");

    @Test
    void convertsIli2gpkgTablesToDxfFiles() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/iox-wkf/Gpkg2Dxf", projectDir);
        writeBuild(gpkg2DxfBuild("""
                tasks.register('gpkg2dxf', Gpkg2Dxf) {
                    dataFile 'ch.so.agi_av_gb_administrative_einteilungen_2020-08-20.gpkg'
                    outputDir layout.buildDirectory.dir('dxf')
                }
                """));

        run("gpkg2dxf");

        Path outputDirectory = projectDir.resolve("build/dxf");
        Path municipalities = outputDirectory.resolve("nachfuehrngskrise_gemeinde.dxf");
        Path landRegistryDistricts = outputDirectory.resolve("grundbuchkreise_grundbuchkreis.dxf");
        assertTrue(Files.isRegularFile(municipalities));
        assertTrue(Files.isRegularFile(landRegistryDistricts));

        String municipalityDxf = Files.readString(municipalities, DXF_CHARSET);
        String landRegistryDistrictDxf = Files.readString(landRegistryDistricts, DXF_CHARSET);
        assertTrue(municipalityDxf.contains("LerchWeberAG"));
        assertTrue(municipalityDxf.contains("2638171.578"));
        assertTrue(landRegistryDistrictDxf.contains("2619682.201"));
        assertTrue(landRegistryDistrictDxf.contains("ENDSEC"));
    }

    private String gpkg2DxfBuild(String tasks) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Gpkg2Dxf

                %s
                """.formatted(tasks);
    }
}
