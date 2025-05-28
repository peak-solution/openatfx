<#-- To render the third-party file.
 Available context :
 - dependencyMap a collection of Map.Entry with
   key are dependencies (as a MavenProject) (from the maven project)
   values are licenses of each dependency (array of string)
 - licenseMap a collection of Map.Entry with
   key are licenses of each dependency (array of string)
   values are all dependencies using this license
-->
<#function licenseFormat licenses>
	<#assign result = ""/>
	<#if licenses?size gt 1>
		<#assign result = result + "Multi-license: "/>
	</#if>
	<#list licenses as license>
		<#assign result = result + license/>
		<#if license_has_next>
			<#assign result = result + " or "/>
		</#if>
	</#list>
	<#return result>
</#function>
<#function artifactFormat p>
 	<#return p.artifactId + "-" + p.version + ".jar"/>
</#function>
<#function urlFormat p>
    <#return (p.url!"no url defined")>
</#function>
<#list dependencyMap as e>
    <#assign project = e.getKey()/>
    <#assign licenses = e.getValue()/>
${artifactFormat(project)};${licenseFormat(licenses)};${urlFormat(project)}
</#list>