package org.codehaus.groovy.grails.web.pages

import grails.util.Environment
import groovy.lang.MetaClass

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.web.metaclass.TagLibInvoker
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

class GroovyPagesMetaUtils {
    private final static Object[] EMPTY_OBJECT_ARRAY=new Object[0]
    private static final boolean PROXY_TAGLIB_METACLASS_CALLS=Boolean.getBoolean("grails.gsp.taglib.proxy_calls").booleanValue()

    public static void registerMethodMissingForGSP(Class gspClass, TagLibraryLookup gspTagLibraryLookup) {
        registerMethodMissingForGSP(GrailsMetaClassUtils.getExpandoMetaClass(gspClass), gspTagLibraryLookup)
    }

    public static void registerMethodMissingForGSP(final MetaClass mc, final TagLibraryLookup gspTagLibraryLookup) {
        final boolean addMethodsToMetaClass = !Environment.isDevelopmentMode()

        mc.methodMissing = { String name, args ->
            methodMissingForTagLib(mc, mc.getTheClass(), gspTagLibraryLookup, GroovyPage.DEFAULT_NAMESPACE, name, args, addMethodsToMetaClass)
        }
        registerMethodMissingWorkaroundsForDefaultNamespace(mc, gspTagLibraryLookup)
    }

    public static Object methodMissingForTagLib(MetaClass mc, Class type, TagLibraryLookup gspTagLibraryLookup, String namespace, String name, args, boolean addMethodsToMetaClass) {
        final GroovyObject tagBean = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
        if (tagBean != null) {
            final MetaMethod method=tagBean.respondsTo(name, args).find{ it }
            if (method != null) {
                if (addMethodsToMetaClass) {
                    registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, name, true)
                }
                return method.invoke(tagBean, args)
            }
        }
        throw new MissingMethodException(name, type, args)
    }

    public static void registerMethodMissingWorkaroundsForDefaultNamespace(MetaClass mc, TagLibraryLookup gspTagLibraryLookup) {
        // hasErrors gets mixed up by hasErrors method without this metaclass modification
        registerMethodMissingForTags(mc, gspTagLibraryLookup, GroovyPage.DEFAULT_NAMESPACE, 'hasErrors', false)
    }

    // copied from /grails-plugin-controllers/src/main/groovy/org/codehaus/groovy/grails/web/plugins/support/WebMetaUtils.groovy
    private static void registerMethodMissingForTags(final MetaClass mc, final TagLibraryLookup gspTagLibraryLookup, final String namespace, final String name, final boolean addAll) {
        if(!addAll) {
            mc."$name" = {Map attrs, Closure body ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, GrailsWebRequest.lookup())
            }
            mc."$name" = {Map attrs, CharSequence body ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, new GroovyPage.ConstantClosure(body), GrailsWebRequest.lookup())
            }
            mc."$name" = {Map attrs ->
                GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, GrailsWebRequest.lookup())
            }
        } else {
            TagLibInvoker invoker=new TagLibInvoker(gspTagLibraryLookup, namespace, name, mc.getTheClass(), true)
            mc."$name" = { Object[] varArgs ->
                invoker.invoke(varArgs)
            }
        }
    }
}
