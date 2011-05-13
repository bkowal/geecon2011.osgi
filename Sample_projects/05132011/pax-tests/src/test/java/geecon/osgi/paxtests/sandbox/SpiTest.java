package geecon.osgi.paxtests.sandbox;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import geecon.osgi.paxtests.AbstractIntegrationTest;
import geecon.osgi.spi.SpiClient;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;


/**
 * See slide No. 15. Bundles defined here are exactly the same as on that slide.
 * 
 * 
 * @author Bartosz Kowalewski
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class SpiTest extends AbstractIntegrationTest {

    public Option[] testSpecificConfiguration() {

        Option[] testSpecificOptions = options(

                // bundle that provides the interface and the service
                provision(TinyBundles.newBundle().set(Constants.BUNDLE_NAME,
                        "geecon.osgi.spi").set(Constants.BUNDLE_SYMBOLICNAME,
                        "geecon.osgi.spi").set(Constants.EXPORT_PACKAGE,
                        "geecon.osgi.spi").set(
                        Constants.IMPORT_PACKAGE, "javax.xml.bind").add(
                        SpiClient.class).build(withBnd())),

                new Customizer() {
                    @Override
                    public InputStream customizeTestProbe(InputStream testProbe)
                            throws Exception {

                        TinyBundle bundle = TinyBundles
                                .modifyBundle(testProbe)

                                .removeResource(
                                        "geecon/osgi/spi/SpiClient.class")
                                .removeResource(
                                        "geecon/osgi/spi/")

                                .removeHeader("Private-Package");

                        return bundle.build();
                    }

                },

                mavenBundle()
                        .groupId("org.apache.servicemix.specs")
                        .artifactId(
                                "org.apache.servicemix.specs.activation-api-1.1")
                        .version("1.4.0"),

                mavenBundle().groupId("org.apache.servicemix.specs")
                        .artifactId("org.apache.servicemix.specs.stax-api-1.0")
                        .version("1.4.0"),

                // This library provides an external implementation of the JAXB
                // api - com.sun.xml.bind.v2; will it be detected by the API
                // bundle? This depends on the API bundle that is used!

                // the standard 2.1.12_1 artifact + extra meta-inf header
                // ('SPI-Provider') which tells SPI-Fly to process this bundle
                mavenBundle().groupId("org.apache.servicemix.bundles")
                        .artifactId("org.apache.servicemix.bundles.jaxb-impl")
                        .version("2.1.12_1_bartek"),

                // ##############################

                // 1. jaxb-api bundled by SpringSource guys; plain jaxb-api
                // this will use the default impl sun internal available through
                // boot delegation (see AbstractIntegrationTest)

                // XXX the default (com.sun...internal) impl will be used, SPI
                // will not work; check the error printed onto console
                        mavenBundle().groupId("javax.xml.bind").artifactId("com.springsource.javax.xml.bind").version("2.2.0")

        // ##############################

        // 2. jaxb-api from ServiceMix - with enhancements :-)
        // a locator is used in the API bundle; it's an extender that is
        // able to detect bundles that provide proper impl through SPI
        // XXX in order to try this option out, comment bundle #1 out
        // and uncomment this one
        // XXX this will work! com.sun.xml.bind.v2 will be used; check the error
        // printed onto console

//         mavenBundle().groupId("org.apache.servicemix.specs")
//         .artifactId("org.apache.servicemix.specs.jaxb-api-2.1")
//         .version("1.4.0")

        // ##############################
        // 3. Aries SPI-Fly
        // This is also an extender. It processes all bundles, detects
        // those that provide some impls through SPI, instantiates these
        // impls and attempts to register them in the OSGi registry.

        // This fails as the SPI service id is
        // javax.xml.bind.JAXBContext while the object being created
        // implements javax.xml.bind.JAXBContextFactory. OSGi will not
        // let you register such a service :). There's a lot of work
        // that still needs to be done in the Apache Aries project
        // before SPI-Fly is capable of handling the JAXB usecase :).

        // XXX in order to use this component, you need to build Apache
        // Aries sources :/
        // See Aries website

//         mavenBundle().groupId("org.apache.aries.spifly").artifactId(
//         "org.apache.aries.spifly.core").version(
//         "0.2-incubating-SNAPSHOT")

        );

        return testSpecificOptions;

    }

    @Test
    public void testIt() throws Exception {

        new SpiClient().useSpi();

        getServiceRegisteredBySpiFly();

    }

    private void getServiceRegisteredBySpiFly() throws InvalidSyntaxException,
            InterruptedException {
        ServiceTracker tracker = null;
        try {
            Filter osgiFilter = FrameworkUtil.createFilter("("
                    + Constants.OBJECTCLASS + "=javax.xml.bind.JAXBContext)");
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open();

            Object x = tracker.waitForService(10000);
            if (x == null) {
                // this message should be printed - SPI-Fly will not work!
                System.out.println("Found nothing");
            } else {
                System.out.println("Found " + x.getClass());
            }
        } finally {
            if (tracker != null) {
                try {
                    tracker.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

}
