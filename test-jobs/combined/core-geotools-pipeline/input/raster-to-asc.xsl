<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" omit-xml-declaration="yes"/>
  <xsl:template match="/raster">
    <xsl:text>ncols </xsl:text><xsl:value-of select="@ncols"/><xsl:text>&#10;</xsl:text>
    <xsl:text>nrows </xsl:text><xsl:value-of select="@nrows"/><xsl:text>&#10;</xsl:text>
    <xsl:text>xllcorner </xsl:text><xsl:value-of select="@xllcorner"/><xsl:text>&#10;</xsl:text>
    <xsl:text>yllcorner </xsl:text><xsl:value-of select="@yllcorner"/><xsl:text>&#10;</xsl:text>
    <xsl:text>cellsize </xsl:text><xsl:value-of select="@cellsize"/><xsl:text>&#10;</xsl:text>
    <xsl:text>NODATA_value </xsl:text><xsl:value-of select="@nodata"/><xsl:text>&#10;</xsl:text>
    <xsl:for-each select="row"><xsl:value-of select="normalize-space(.)"/><xsl:text>&#10;</xsl:text></xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
