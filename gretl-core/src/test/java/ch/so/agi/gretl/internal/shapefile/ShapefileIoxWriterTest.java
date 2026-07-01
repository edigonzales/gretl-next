package ch.so.agi.gretl.internal.shapefile;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.jts.Jts2iox;
import ch.so.agi.gretl.internal.shapefile.core.DbfFieldType;
import ch.so.agi.gretl.internal.shapefile.core.DbfReader;
import ch.so.agi.gretl.internal.shapefile.core.ShapeType;
import ch.so.agi.gretl.internal.shapefile.core.ShpReader;
import ch.so.agi.gretl.internal.shapefile.core.ShxReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapefileIoxWriterTest {

    private final GeometryFactory geometries = new GeometryFactory();

    @TempDir
    Path folder;

    @Test
    void writesDescriptorSchemaBeforeFirstObject() throws Exception {
        Path target = folder.resolve("point.shp");
        ShapefileIoxWriter writer = new ShapefileIoxWriter(target, StandardCharsets.UTF_8, Optional.of("LOCAL_CS[\"test\"]"));
        writer.setAttributeDescriptors(List.of(
                new ShapefileAttributeDescriptor("geom", "geom", null, 0, 0, ShapeType.POINT, 2056, false),
                new ShapefileAttributeDescriptor("name", "name", DbfFieldType.CHARACTER, 16, 0, null, null, false),
                new ShapefileAttributeDescriptor("count", "count", DbfFieldType.NUMERIC, 11, 0, null, null, false),
                new ShapefileAttributeDescriptor("ratio", "ratio", DbfFieldType.NUMERIC, 12, 2, null, null, false),
                new ShapefileAttributeDescriptor("date", "date", DbfFieldType.DATE, 8, 0, null, null, false),
                new ShapefileAttributeDescriptor("enabled", "enabled", DbfFieldType.LOGICAL, 1, 0, null, null, false),
                new ShapefileAttributeDescriptor("stamp", "stamp", DbfFieldType.CHARACTER, 24, 0, null, null, false)));

        Iom_jObject object = new Iom_jObject("Model.Topic.Class", "o1");
        object.addattrobj("geom", Jts2iox.JTS2coord(new Coordinate(2638000, 1175250)));
        object.setattrvalue("name", "abc");
        object.setattrvalue("count", "2");
        object.setattrvalue("ratio", "3.40");
        object.setattrvalue("date", "2013-10-21");
        object.setattrvalue("enabled", "true");
        object.setattrvalue("stamp", "2015-02-16T08:35:45.000");

        writer.write(new ObjectEvent(object));
        writer.write(new EndTransferEvent());
        writer.close();

        try (ShpReader shp = ShpReader.open(target); DbfReader dbf = DbfReader.open(target.resolveSibling("point.dbf"), StandardCharsets.UTF_8)) {
            assertEquals(ShapeType.POINT, shp.header().shapeType());
            assertEquals(List.of("name", "count", "ratio", "date", "enabled", "stamp"),
                    dbf.fields().stream().map(field -> field.name()).toList());
            List<String> values = dbf.readNext().orElseThrow().values();
            assertEquals("abc", values.get(0).trim());
            assertEquals("2", values.get(1).trim());
            assertEquals("3.40", values.get(2).trim());
            assertEquals("20131021", values.get(3).trim());
            assertEquals("T", values.get(4).trim());
            assertEquals("2015-02-16T08:35:45.000", values.get(5).trim());
        }
        try (ShxReader shx = ShxReader.open(target.resolveSibling("point.shx"))) {
            assertEquals(1, shx.entries().size());
        }
        assertEquals("UTF-8", Files.readString(target.resolveSibling("point.cpg"), StandardCharsets.US_ASCII));
        assertTrue(Files.readString(target.resolveSibling("point.prj")).contains("LOCAL_CS"));
    }

    @Test
    void writesNullShapeForTablesWithoutGeometry() throws Exception {
        Path target = folder.resolve("dbf_only.shp");
        ShapefileIoxWriter writer = new ShapefileIoxWriter(target, StandardCharsets.UTF_8, Optional.empty());
        writer.setAttributeDescriptors(List.of(
                new ShapefileAttributeDescriptor("id", "id", DbfFieldType.NUMERIC, 11, 0, null, null, false),
                new ShapefileAttributeDescriptor("text", "text", DbfFieldType.CHARACTER, 8, 0, null, null, false)));

        Iom_jObject object = new Iom_jObject("Model.Topic.Class", "o1");
        object.setattrvalue("id", "7");
        object.setattrvalue("text", "plain");
        writer.write(new ObjectEvent(object));
        writer.write(new EndTransferEvent());
        writer.close();

        try (ShpReader shp = ShpReader.open(target); DbfReader dbf = DbfReader.open(target.resolveSibling("dbf_only.dbf"), StandardCharsets.UTF_8)) {
            assertEquals(ShapeType.NULL, shp.header().shapeType());
            assertEquals("7", dbf.readNext().orElseThrow().values().get(0).trim());
        }
    }

    @Test
    void writesSupportedGeometryFamilies() throws Exception {
        writeGeometry("point", ShapeType.POINT, Jts2iox.JTS2coord(new Coordinate(1, 2)));
        writeGeometry("multipoint", ShapeType.MULTIPOINT,
                Jts2iox.JTS2multicoord(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) }));
        writeGeometry("polyline", ShapeType.POLYLINE,
                Jts2iox.JTS2polyline(geometries.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) })));
        writeGeometry("multipolyline", ShapeType.POLYLINE,
                Jts2iox.JTS2multipolyline(geometries.createMultiLineString(new com.vividsolutions.jts.geom.LineString[] {
                        geometries.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) })
                })));
        writeGeometry("polygon", ShapeType.POLYGON,
                Jts2iox.JTS2surface(geometries.createPolygon(new Coordinate[] {
                        new Coordinate(0, 0), new Coordinate(4, 0), new Coordinate(4, 4), new Coordinate(0, 0)
                })));
        writeGeometry("multipolygon", ShapeType.POLYGON,
                Jts2iox.JTS2multisurface(geometries.createMultiPolygon(new com.vividsolutions.jts.geom.Polygon[] {
                        geometries.createPolygon(new Coordinate[] {
                                new Coordinate(0, 0), new Coordinate(4, 0), new Coordinate(4, 4), new Coordinate(0, 0)
                        })
                })));
    }

    private void writeGeometry(String name, ShapeType shapeType, IomObject geometry) throws Exception {
        Path target = folder.resolve(name + ".shp");
        ShapefileIoxWriter writer = new ShapefileIoxWriter(target, StandardCharsets.UTF_8, Optional.empty());
        writer.setAttributeDescriptors(List.of(
                new ShapefileAttributeDescriptor("geom", "geom", null, 0, 0, shapeType, 2056, false)));
        Iom_jObject object = new Iom_jObject("Model.Topic.Class", "o1");
        object.addattrobj("geom", geometry);
        writer.write(new ObjectEvent(object));
        writer.write(new EndTransferEvent());
        writer.close();

        try (ShpReader shp = ShpReader.open(target)) {
            assertEquals(shapeType, shp.header().shapeType());
            assertTrue(shp.readNext().isPresent());
        }
    }
}
