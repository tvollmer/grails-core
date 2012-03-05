<%=packageName%>
<% import grails.persistence.Event %>
<% import org.codehaus.groovy.grails.commons.* %>

<%
def domainSuffix = "Instance"

class MyScaffoldingGrailsDomainClassProperty {

    @Delegate
    GrailsDomainClassProperty grailsDomainClassProperty

    String propertyPath
    String domainSuffix
    
    String getNullsafePropertyPath(){
        propertyPath.replaceAll(/\./, '?.')
    }
    
    String getPropertyPathFieldName(){
        propertyPath.replaceAll("(^.*${domainSuffix}\\.)(.*\$)", '\$2')
    }
}

    excludedProps = Event.allEvents.toList() << 'version' << 'dateCreated' << 'lastUpdated'
	persistentPropNames = domainClass.persistentProperties*.name
	boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
	if (hasHibernate && org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder.getMapping(domainClass)?.identity?.generator == 'assigned') {
		persistentPropNames << domainClass.identifier.name
	}
	props = domainClass.properties.findAll { persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) }
	Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
	for (p in props) {
		if (p.embedded) {
			def embeddedPropNames = p.component.persistentProperties*.name
			def embeddedProps = p.component.properties.findAll { embeddedPropNames.contains(it.name) && !excludedProps.contains(it.name) }
			Collections.sort(embeddedProps, comparator.constructors[0].newInstance([p.component] as Object[]))
			%><fieldset class="embedded"><legend><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /></legend><%
				for (ep in p.component.properties) {
					MyScaffoldingGrailsDomainClassProperty sProp = new MyScaffoldingGrailsDomainClassProperty(grailsDomainClassProperty:ep,propertyPath:"${domainClass.propertyName}${domainSuffix}.${p.name}.${ep.name}",domainSuffix:domainSuffix)
					renderFieldForProperty(sProp, p.component, "${p.name}.")
				}
			%></fieldset><%
		} else {
			MyScaffoldingGrailsDomainClassProperty sProp = new MyScaffoldingGrailsDomainClassProperty(grailsDomainClassProperty:p,propertyPath:"${domainClass.propertyName}${domainSuffix}.${p.name}",domainSuffix:domainSuffix)
			renderFieldForProperty(p, domainClass)
		}
	}

private renderFieldForProperty(p, owningClass, prefix = "") {
	boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
	boolean display = true
	boolean required = false
	if (hasHibernate) {
		cp = owningClass.constrainedProperties[p.name]
		display = (cp ? cp.display : true)
		required = (cp ? !(cp.propertyType in [boolean, Boolean]) && !cp.nullable && (cp.propertyType != String || !cp.blank) : false)
	}
	if (display) { %>
<div class="fieldcontain \${hasErrors(bean: ${propertyName}, field: '${prefix}${p.name}', 'error')} ${required ? 'required' : ''}">
	<label for="${prefix}${p.name}">
		<g:message code="${domainClass.propertyName}.${prefix}${p.name}.label" default="${p.naturalName}" />
		<% if (required) { %><span class="required-indicator">*</span><% } %>
	</label>
	${renderEditor(p)}
</div>
<%  }   } %>
