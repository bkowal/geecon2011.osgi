package geecon.osgi.tcl;

import geecon.osgi.tcl.internal.OtherInterface;
import geecon.osgi.tcl.internal.SuperClass;
import net.sf.cglib.proxy.Enhancer;

import org.apache.log4j.Logger;


public class SampleService {

    private static final Logger log = Logger.getLogger(SampleService.class);

    public void process1(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {

        Class<?> simpleInterfaceImpl = null;
        try {
            simpleInterfaceImpl = this.getClass().getClassLoader().loadClass(
                    className);
        } catch (ClassNotFoundException e) {

            log.info("Unable to load the given implementation ('" + className
                    + "') using bundle classloader.", e);

            ClassLoader contextClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            if (contextClassLoader == null) {
                throw e;
            }

            simpleInterfaceImpl = contextClassLoader.loadClass(className);
            log.info("Loaded the given implementation ('" + className
                    + "') using thread context classloader.");
        }

        SampleInterface instance = (SampleInterface) simpleInterfaceImpl
                .newInstance();

        instance.doIt();
    }

    public void process2(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {

        ClassLoader contextClassLoader = Thread.currentThread()
                .getContextClassLoader();
        Class<?> simpleInterfaceImpl = contextClassLoader.loadClass(className);

        SampleInterface instance = (SampleInterface) simpleInterfaceImpl
                .newInstance();

        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new CglibInterceptor(instance));
        enhancer.setSuperclass(simpleInterfaceImpl);
        enhancer.setInterfaces(new Class[] { SampleInterface.class,
                OtherInterface.class });

        SampleInterface cglibProxy = (SampleInterface) enhancer.create();

        cglibProxy.doIt();
    }

    public void process2(SampleInterface instance)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {

        // This piece of code mimics what Hibernate normally does

        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new CglibInterceptor(instance));
        enhancer.setSuperclass(instance.getClass());
        enhancer.setInterfaces(new Class[] { SampleInterface.class,
                OtherInterface.class });

        SampleInterface cglibProxy = (SampleInterface) enhancer.create();

        cglibProxy.doIt();
    }

    public void processX(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {

        ClassLoader contextClassLoader = Thread.currentThread()
                .getContextClassLoader();
        Class<?> simpleInterfaceImpl = contextClassLoader.loadClass(className);

        SampleInterface instance = (SampleInterface) simpleInterfaceImpl
                .newInstance();

        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new CglibInterceptor(instance));
        enhancer.setSuperclass(SuperClass.class);
        enhancer.setInterfaces(new Class[] { SampleInterface.class,
                OtherInterface.class });

        SampleInterface cglibProxy = (SampleInterface) enhancer.create();

        cglibProxy.doIt();
    }

}
