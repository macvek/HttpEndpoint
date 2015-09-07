<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html"/> 
 
    <xsl:template match="/">
        <html>
            <body>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="display">
        <div>DISPLAY</div>
        <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="display/input">
        <label><xsl:value-of select="@label"/></label>
        <input type="text">
            <xsl:attribute name="name">
                <xsl:value-of select="@name" />
            </xsl:attribute>            
        </input>
    </xsl:template>
    
    <xsl:template match="display/trigger" >
        <input type="button" value="Submit" />
    </xsl:template>

</xsl:stylesheet>


