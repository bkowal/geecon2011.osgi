package geecon.osgi.eqcl;

import geecon.osgi.eqcl.user.ANastyReflectionUser;

import java.lang.reflect.InvocationTargetException;

public class MyPrivateClass {

    public MyPrivateClass() {

        System.out.println("Being instantiated... TCCL: " + Thread.currentThread().getContextClassLoader());

    }

    public void call() {

        System.out.println("I'm being called. TCCL: " + Thread.currentThread().getContextClassLoader());

    }

    public void callMyselfReflectionUser() throws SecurityException, IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        System.out.println("I'm calling " + ANastyReflectionUser.class + ". TCCL: " + Thread.currentThread().getContextClassLoader());
        // not really 'myself' :); a new istance of MyPrivateClass will be used by ANastyReflectionUser

        new ANastyReflectionUser().callMyPrivateClassThroughReflection();

    }
}
