package geecon.osgi.paxtests.sandbox;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import geecon.osgi.paxtests.AbstractIntegrationTest;
import geecon.osgi.tcl.CglibInterceptor;
import geecon.osgi.tcl.SampleInterface;
import geecon.osgi.tcl.SampleService;
import geecon.osgi.tcl.impl.SampleImpl;
import geecon.osgi.tcl.internal.OtherInterface;
import geecon.osgi.tcl.internal.SuperClass;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;


/**
 * @author Bartosz Kowalewski
 *
 */
@RunWith(JUnit4TestRunner.class)
public class TclTest extends AbstractIntegrationTest {

    public Option[] testSpecificConfiguration() {

        Option[] testSpecificOptions = options(

        // bundle that provides the interface and the service
                provision(TinyBundles
                        .newBundle()
                        .set(Constants.BUNDLE_NAME, "geecon.osgi.tcl")
                        .set(Constants.BUNDLE_SYMBOLICNAME, "geecon.osgi.tcl")
                        .set(Constants.EXPORT_PACKAGE,
                                "geecon.osgi.tcl")
                        .set(Constants.IMPORT_PACKAGE,
                                "org.apache.log4j, net.sf.cglib.proxy, net.sf.cglib.core, net.sf.cglib.reflect")
                        .add(SampleInterface.class).add(SampleService.class)
                        .add(CglibInterceptor.class).add(SuperClass.class).add(
                                OtherInterface.class).build(withBnd())),

                // bundle that provides the implementation and calls the service
                provision(TinyBundles
                        .newBundle()
                        .set(Constants.BUNDLE_NAME, "geecon.osgi.tcl.impl")
                        .set(Constants.BUNDLE_SYMBOLICNAME,
                                "geecon.osgi.tcl.impl")
                        .set(Constants.IMPORT_PACKAGE,
                                "org.apache.log4j, geecon.osgi.tcl")
                        .add(SampleImpl.class).build(withBnd())),

                new Customizer() {
                    @Override
                    public InputStream customizeTestProbe(InputStream testProbe)
                            throws Exception {

                        TinyBundle bundle = TinyBundles
                                .modifyBundle(testProbe)

                                .removeResource(
                                        "geecon/osgi/tcl/impl/SampleImpl.class")
                                .removeResource(
                                        "geecon/osgi/tcl/impl/")

                                .removeResource(
                                        "geecon/osgi/tcl/internal/SuperClass.class")
                                .removeResource(
                                        "geecon/osgi/tcl/internal/OtherInterface.class")
                                .removeResource(
                                        "geecon/osgi/tcl/internal/")

                                .removeResource(
                                        "geecon/osgi/tcl/SampleInterface.class")
                                .removeResource(
                                        "geecon/osgi/tcl/SampleService.class")
                                .removeResource(
                                        "geecon/osgi/tcl/CglibInterceptor.class")
                                .removeResource(
                                        "geecon/osgi/tcl/")

                                .removeHeader("Private-Package");

                        return bundle.build();
                    }

                }, mavenBundle().groupId("net.sourceforge.cglib").artifactId(
                        "com.springsource.net.sf.cglib").version("2.1.3")

        );

        return testSpecificOptions;

    }

    @Test
    public void testIt() throws Exception {

        ClassLoader oldTcl = Thread.currentThread().getContextClassLoader();

        try {
            // 1. Without thread context classloader
            // Thread.currentThread().setContextClassLoader(null);

            // 2. With thread context classloader
            Thread.currentThread().setContextClassLoader(
                    SampleImpl.class.getClassLoader());

            // will work, but only with context classloader set
            new SampleService()
                    .process1("geecon.osgi.tcl.impl.SampleImpl");

            // This cannot work, even with context classloader set; See
            // SampleService().process2(); CGLib requires a single classloader
            // that will be capable of loading:
            // geecon.osgi.tcl.impl.SampleImpl,
            // geecon.osgi.tcl.SampleInterface,
            // geecon.osgi.tcl.internal.OtherInterface

            // The context classloader is only capable of loading:
            // geecon.osgi.tcl.impl.SampleImpl, and
            // geecon.osgi.tcl.SampleInterface

            // The 'geecon.osgi.tcl' bundle (the bundle that contains
            // SampleService is only capable of loading:
            // geecon.osgi.tcl.SampleInterface, and
            // geecon.osgi.tcl.internal.OtherInterface

             new SampleService().process2(new SampleImpl());

        } finally {
            Thread.currentThread().setContextClassLoader(oldTcl);
        }

    }

}
