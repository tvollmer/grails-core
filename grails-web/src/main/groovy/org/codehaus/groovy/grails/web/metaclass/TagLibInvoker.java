package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;


public class TagLibInvoker {
    final TagLibraryLookup gspTagLibraryLookup;
    final String namespace;
    final String name;
    final Class<?> parentType;
    final Map<ArgumentsKey, TagLibMethod> methods=new ConcurrentHashMap<ArgumentsKey, TagLibMethod>();
    
      
    public TagLibInvoker(TagLibraryLookup gspTagLibraryLookup, String namespace, String name, Class<?> parentType, boolean addAll) {
        this.gspTagLibraryLookup = gspTagLibraryLookup;
        this.namespace = namespace;
        this.name = name;
        this.parentType = parentType;
        initialize(addAll);
    }

    private void initialize(boolean addAll) {
        if(addAll) {
            methods.put(new ArgumentsKey(null,null,0), new TagLibMethod() {
                public Object invoke(Object[] arguments) {
                    return GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, new LinkedHashMap<Object, Object>(), null, GrailsWebRequest.lookup());
                }
            });
            methods.put(new ArgumentsKey(Closure.class,null,1), new TagLibMethod() {
                public Object invoke(Object[] arguments) {
                    return GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, new LinkedHashMap<Object, Object>(), (Closure)arguments[0], GrailsWebRequest.lookup());
                }
            });
        }
        methods.put(new ArgumentsKey(Map.class,null,1), new TagLibMethod() {
            public Object invoke(Object[] arguments) {
                return GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, (Map)arguments[0], null, GrailsWebRequest.lookup());
            }
        });
        methods.put(new ArgumentsKey(Map.class,Closure.class,2), new TagLibMethod() {
            public Object invoke(Object[] arguments) {
                return GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, (Map)arguments[0], (Closure)arguments[1], GrailsWebRequest.lookup());
            }
        });
        methods.put(new ArgumentsKey(Map.class,CharSequence.class,2), new TagLibMethod() {
            public Object invoke(Object[] arguments) {
                return GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, (Map)arguments[0], new GroovyPage.ConstantClosure((CharSequence)arguments[1]), GrailsWebRequest.lookup());
            }
        });
    }

    public Object invoke(Object[] arguments) {
        if(arguments.length > 2) {
            throw new MissingMethodException(name, parentType, arguments);
        }
        Class<?>[] argumentClasses=new Class[2];
        for(int i=0;i < arguments.length;i++) {
            if(arguments[i] != null) {
                argumentClasses[i]=arguments[i].getClass();
            }
        }
        ArgumentsKey key = new ArgumentsKey(argumentClasses, arguments.length);
        TagLibMethod method = methods.get(key);
        if(method==null) {
            for(Map.Entry<ArgumentsKey, TagLibMethod> entry : new ArrayList<Map.Entry<ArgumentsKey, TagLibMethod>>(methods.entrySet())) {
                if(entry.getKey().isValid() && entry.getKey().matches(argumentClasses)) {
                    method = entry.getValue();
                    methods.put(key, method);
                    break;
                }
            }
        }
        if(method!=null) {
            return method.invoke(arguments);
        } else {
            throw new MissingMethodException(name, parentType, arguments);
        }
    }
    
    private static interface TagLibMethod {
        public Object invoke(Object[] arguments);
    }
    
    private static class ArgumentsKey {
        private final WeakReference<Class<?>> argumentRef1;
        private final WeakReference<Class<?>> argumentRef2;
        private final int argumentsCount;
        private final boolean argument1IsNull;
        private final boolean argument2IsNull;
        
        public ArgumentsKey(Class<?>[] argumentClasses, int argumentsCount) {
            this(argumentClasses[0], argumentClasses[1], argumentsCount);
        }
        
        public ArgumentsKey(Class<?> argument1, Class<?> argument2, int argumentsCount) {
            this.argumentRef1 = new WeakReference<Class<?>>(argument1);
            this.argumentRef2 = new WeakReference<Class<?>>(argument2);
            this.argumentsCount = argumentsCount;
            this.argument1IsNull = (argument1==null);
            this.argument2IsNull = (argument2==null);
        }
        
        public Class<?> getArgument1() {
            return argumentRef1.get();
        }

        public Class<?> getArgument2() {
            return argumentRef2.get();
        }
        
        /**
         * Check that this key is still valid and the weak references haven't been collected
         * 
         * @return
         */
        public boolean isValid() {
            if(argumentsCount==1) {
                return argument1IsNull || getArgument1() != null;
            }
            if(argumentsCount==2) {
                return (argument1IsNull || getArgument1() != null) && (argument2IsNull || getArgument2() != null);
            }
            return true;
        }

        public boolean matches(Class<?>[] argumentClasses) {
            return (matches(getArgument1(), argumentClasses[0]) &&
               matches(getArgument2(), argumentClasses[1]));
        }   
        
        private boolean matches(Class<?> my, Class<?> other) {
            if(my==other) return true;
            if(my==null && other != null) return false;
            if(my != null && other == null) return false;
            return my.isAssignableFrom(other);         
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (argument1IsNull ? 1231 : 1237);
            result = prime * result + (argument2IsNull ? 1231 : 1237);
            result = prime * result + ((getArgument1() == null) ? 0 : getArgument1().hashCode());
            result = prime * result + ((getArgument2() == null) ? 0 : getArgument2().hashCode());
            result = prime * result + argumentsCount;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ArgumentsKey other = (ArgumentsKey)obj;
            if (argument1IsNull != other.argument1IsNull)
                return false;
            if (argument2IsNull != other.argument2IsNull)
                return false;
            if (getArgument1() == null) {
                if (other.getArgument1() != null)
                    return false;
            }
            else if (!getArgument1().equals(other.getArgument1()))
                return false;
            if (getArgument2() == null) {
                if (other.getArgument2() != null)
                    return false;
            }
            else if (!getArgument2().equals(other.getArgument2()))
                return false;
            if (argumentsCount != other.argumentsCount)
                return false;
            return true;
        }


    }
}
