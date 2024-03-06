<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.w3.org/1999/XSL/Transform https://www.w3.org/2007/schema-for-xslt20.xsd">

<xsl:import href="usage.xsl"/>

<xsl:preserve-space elements="description" />
<xsl:output indent="yes" omit-xml-declaration="yes"/>

<xsl:template match="/">
<xsl:call-template name="getUsage_links"/>
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="plugin">
# <xsl:value-of select="name"/>

<xsl:text>
</xsl:text>

<xsl:value-of select="description"/>

## Usage

<xsl:call-template name="getUsage"/>

## Goals

### Overview
<xsl:apply-templates select="mojos/mojo" mode="overview"/>

<xsl:text>
</xsl:text>

<xsl:apply-templates select="mojos/mojo" mode="detail"/>

</xsl:template>

<xsl:template match="mojo" mode="overview">
- [<xsl:value-of select="/plugin/goalPrefix"/>:<xsl:value-of select="goal"/>](#<xsl:value-of select="/plugin/goalPrefix"/><xsl:value-of select="goal"/>) <xsl:call-template name="getSummary"><xsl:with-param name="description" select="description"/></xsl:call-template>
</xsl:template>

<xsl:template match="mojo" mode="detail">
### <xsl:value-of select="/plugin/goalPrefix"/>:<xsl:value-of select="goal"/>

**Full name:**

<xsl:value-of select="/plugin/groupId"/>:<xsl:value-of select="/plugin/artifactId"/>:<xsl:value-of select="/plugin/version"/>:<xsl:value-of select="goal"/>

**Description:**

<xsl:value-of select="description"/>

**Attributes:**

<xsl:if test="requiresProject = 'true'">- Requires a Maven project to be executed.
</xsl:if>
<xsl:if test="requiresDependencyResolution">- Requires dependency resolution of artifacts in scope: <xsl:value-of select="requiresDependencyResolution"/>.
</xsl:if>
<xsl:if test="requiresDependencyCollection">- Requires dependency collection of artifacts in scope: <xsl:value-of select="requiresDependencyCollection"/>.
</xsl:if>
<xsl:if test="since">- Since version: <xsl:value-of select="since"/>.
</xsl:if>
<xsl:if test="phase">- Binds by default to the lifecycle phase: <xsl:value-of select="phase"/>.
</xsl:if>

<xsl:if test="parameters/parameter[required = 'true']">

#### Required parameters

<xsl:element name="table" namespace="">
	<xsl:element name="tr" namespace="">
		<xsl:element name="th" namespace="">Name</xsl:element>
		<xsl:element name="th" namespace="">Type</xsl:element>
		<xsl:element name="th" namespace="">Description</xsl:element>
	</xsl:element>
	<xsl:apply-templates select="parameters/parameter[required = 'true']" mode="overview"/>
</xsl:element>
</xsl:if>

<xsl:if test="parameters/parameter[required = 'false']">

#### Optional parameters

<xsl:element name="table" namespace="">
	<xsl:element name="tr" namespace="">
		<xsl:element name="th" namespace="">Name</xsl:element>
		<xsl:element name="th" namespace="">Type</xsl:element>
		<xsl:element name="th" namespace="">Description</xsl:element>
	</xsl:element>
	<xsl:apply-templates select="parameters/parameter[required = 'false']" mode="overview"/>
</xsl:element>

</xsl:if>

#### Parameter details

<xsl:apply-templates select="parameters/parameter" mode="detail"/>

<xsl:text>
</xsl:text>

</xsl:template>

<xsl:template match="parameter" mode="overview">
<xsl:variable name="parameterHref"><xsl:call-template name="getParameterHref"><xsl:with-param name="parameter" select="."/></xsl:call-template></xsl:variable>
<xsl:variable name="userProperty"><xsl:call-template name="getUserProperty"><xsl:with-param name="parameter" select="."/></xsl:call-template></xsl:variable>
<xsl:variable name="defaultValue"><xsl:call-template name="getDefaultValue"><xsl:with-param name="parameter" select="."/></xsl:call-template></xsl:variable>
<xsl:element name="tr" namespace="">
	<xsl:element name="td" namespace=""><xsl:element name="a" namespace=""><xsl:attribute name="href"><xsl:value-of select="$parameterHref"/></xsl:attribute><xsl:value-of select="name"/></xsl:element></xsl:element>
	<xsl:element name="td" namespace=""><xsl:call-template name="getSimpleName"><xsl:with-param name="typeName" select="type"/></xsl:call-template></xsl:element>
	<xsl:element name="td" namespace="">
		<xsl:call-template name="getSummary"><xsl:with-param name="description" select="description"/></xsl:call-template>
		<xsl:element name="ul" namespace="">
		<xsl:if test="string-length($userProperty) &gt; 0">
		<xsl:element name="li" namespace=""><xsl:element name="em" namespace="">User property</xsl:element>: <xsl:value-of select="$userProperty"/></xsl:element>
		</xsl:if>
		<xsl:if test="string-length($defaultValue) &gt; 0">
		<xsl:element name="li" namespace=""><xsl:element name="em" namespace="">Default</xsl:element>: <xsl:value-of select="$defaultValue"/></xsl:element>
		</xsl:if>
		</xsl:element>
	</xsl:element>
</xsl:element>
</xsl:template>

<xsl:template match="parameter" mode="detail">

##### &lt;<xsl:value-of select="name"/>&gt;

<xsl:value-of select="description"/>

<xsl:variable name="userProperty"><xsl:call-template name="getUserProperty"><xsl:with-param name="parameter" select="."/></xsl:call-template></xsl:variable>
<xsl:variable name="defaultValue"><xsl:call-template name="getDefaultValue"><xsl:with-param name="parameter" select="."/></xsl:call-template></xsl:variable>

- **Type**: <xsl:value-of select="type"/>
<xsl:if test="since">- **Since**: <xsl:value-of select="since"/>
</xsl:if>
- **Required**: <xsl:choose><xsl:when test="required = 'true'">yes</xsl:when><xsl:otherwise>no</xsl:otherwise></xsl:choose> 
<xsl:if test="string-length($userProperty) &gt; 0">
- **User property**: <xsl:value-of select="$userProperty"/>
</xsl:if>
<xsl:if test="string-length($defaultValue) &gt; 0">
- **Default**: <xsl:value-of select="$defaultValue"/>
</xsl:if>

<xsl:text>
</xsl:text>

</xsl:template>

<!--<xsl:template name="getSummary">
<xsl:param name="description"/>
<xsl:choose>
<xsl:when test="contains($description, '&#xa;')"><xsl:value-of select="substring-before($description, '&#xa;')"/></xsl:when>
<xsl:otherwise><xsl:value-of select="$description"/></xsl:otherwise>
</xsl:choose>
</xsl:template>-->

<xsl:template name="getSummary">
<xsl:param name="description"/>
<xsl:choose>
<xsl:when test="$description = ''"></xsl:when>
<xsl:when test="starts-with($description, '&#xa;')">
<xsl:call-template name="getSummary">
<xsl:with-param name="description" select="substring-after($description, '&#xa;')"/>
</xsl:call-template>
</xsl:when>
<xsl:when test="contains($description, '&#xa;')"><xsl:value-of select="substring-before($description, '&#xa;')"/></xsl:when>
<xsl:otherwise><xsl:value-of select="$description"/></xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template name="getSimpleName">
<xsl:param name="typeName"/>
<xsl:choose>
<xsl:when test="contains($typeName, '.')">
<xsl:call-template name="getSimpleName">
<xsl:with-param name="typeName" select="substring-after($typeName, '.')"/>
</xsl:call-template>
</xsl:when>
<xsl:otherwise>
<xsl:choose>
<xsl:when test="contains($typeName, '$')"><xsl:value-of select="substring-after($typeName, '$')"/></xsl:when>
<xsl:otherwise><xsl:value-of select="$typeName"/></xsl:otherwise>
</xsl:choose>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template name="getUserProperty">
<xsl:param name="parameter"/>
<xsl:value-of select="substring-before(substring-after($parameter/../../configuration/child::node()[local-name() = $parameter/name], '{'), '}')"/>
</xsl:template>

<xsl:template name="getDefaultValue">
<xsl:param name="parameter"/>
<xsl:value-of select="$parameter/../../configuration/child::node()[local-name() = $parameter/name]/@default-value"/>
</xsl:template>

<xsl:template name="getParameterHref">
<xsl:param name="parameter"/>
<xsl:variable name="parameterPosition" select="count($parameter/preceding::parameter[name = $parameter/name])"/>#<xsl:value-of select="$parameter/name"/><xsl:if test="$parameterPosition &gt; 0"><xsl:value-of select="$parameterPosition"/></xsl:if></xsl:template>

</xsl:stylesheet>
