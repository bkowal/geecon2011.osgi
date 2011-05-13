
package geecon.osgi.tcl;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CglibInterceptor implements MethodInterceptor {

    final Object delegate;

    CglibInterceptor(Object delegate) {
        this.delegate = delegate;
    }

    public Object intercept(Object object, Method method, Object[] objects,
            MethodProxy methodProxy) throws Throwable {
        return methodProxy.invoke(delegate, objects);
    }
}
