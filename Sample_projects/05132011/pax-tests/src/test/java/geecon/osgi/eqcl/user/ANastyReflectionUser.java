package geecon.osgi.eqcl.user;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ANastyReflectionUser {

    public void callMyPrivateClassThroughReflection() throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        Class<?> clazz = classLoader.loadClass("geecon.osgi.eqcl.MyPrivateClass");
        Object newInstance = clazz.getConstructor().newInstance();

        Method method = clazz.getMethod("call");
        method.invoke(newInstance);
    }

}
