<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>
  <xsl:template match="/">Hello <xsl:value-of select="/root/name"/></xsl:template>
</xsl:stylesheet>
