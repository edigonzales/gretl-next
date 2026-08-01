import ch.so.agi.gretl.tasks.Gzip
import ch.so.agi.gretl.tasks.XslTransformer
import ch.so.agi.gretl.geotools.tasks.RasterReclassify

plugins {
    id("ch.so.agi.gretl")
    id("ch.so.agi.gretl.geotools")
}

val generateRaster = tasks.register<XslTransformer>("generateRaster") {
    xslFile("input/raster-to-asc.xsl")
    xmlFiles("input/raster.xml")
    outDirectory(layout.buildDirectory.dir("generated"))
    fileExtension("asc")
}

val generatedRaster = generateRaster.flatMap { it.outDirectory.file("raster.asc") }

val reclassifyRaster = tasks.register<RasterReclassify>("reclassifyRaster") {
    inputRaster.set(generatedRaster)
    outputRaster(layout.buildDirectory.file("geotools/reclassified.tif"))
    breaks(0.0, 55.0, 60.0, 65.0, 70.0, 500.0)
    noData(-100.0)
}

tasks.register<Gzip>("packageRaster") {
    dataFile(reclassifyRaster.flatMap { it.outputRaster })
    gzipFile(layout.buildDirectory.file("distribution/reclassified.tif.gz"))
}
