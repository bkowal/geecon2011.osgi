package geecon.osgi.paxtests.sandbox;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import geecon.osgi.eqcl.MyPrivateClass;
import geecon.osgi.eqcl.user.ANastyReflectionUser;
import geecon.osgi.paxtests.AbstractIntegrationTest;

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
 * 
 * Shows how Eclipse Buddy classloading and Eclipse Context Finder classloader work.
 * 
 * @author Bartosz Kowalewski
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class EquinoxClassLoadingMagicTest extends AbstractIntegrationTest {

    public Option[] testSpecificConfiguration() {

        Option[] testSpecificOptions =
                options(

                        provision(
                        TinyBundles.newBundle()
                        .set(Constants.BUNDLE_NAME, "geecon.osgi.eqcl")
                        .set(Constants.BUNDLE_SYMBOLICNAME, "geecon.osgi.eqcl")
                        .set(Constants.IMPORT_PACKAGE, "geecon.osgi.eqcl.user")
                        // **** Uncomment to make the testBuddyClassLoading() test method pass
//                         .set("Eclipse-RegisterBuddy", "geecon.osgi.eqcl.user")
                        .add(MyPrivateClass.class)
                        .build(withBnd())
                        ),

                        // bundle that uses reflection
                        provision(
                        TinyBundles.newBundle()
                        .set(Constants.BUNDLE_NAME, "geecon.osgi.eqcl.user")
                        .set(Constants.BUNDLE_SYMBOLICNAME, "geecon.osgi.eqcl.user")
                        .set(Constants.EXPORT_PACKAGE, "geecon.osgi.eqcl.user")
                        // **** Uncomment to make the testBuddyClassLoading() test method pass
//                         .set("Eclipse-BuddyPolicy", "registered")
                        .add(ANastyReflectionUser.class)
                        .build(withBnd())
                        ),

                        new Customizer() {

                            @Override
                            public InputStream customizeTestProbe(InputStream testProbe) throws Exception {

                                TinyBundle bundle =
                                        TinyBundles.modifyBundle(testProbe)

                                        .removeResource("geecon/osgi/eqcl/user/ANastyReflectionUser.class")
                                        .removeResource("geecon/osgi/eqcl/user/")

                                        .removeResource("geecon/osgi/eqcl/MyPrivateClass.class")
                                        .removeResource("geecon/osgi/eqcl")

                                        .removeHeader("Private-Package");

                                return bundle.build();
                            }

                        }

                );

        return testSpecificOptions;

    }

    @Test
    public void testBuddyClassLoading() throws Exception {

        // Only test Equinox Buddy classloading. Forget about Equinox Context Finder for a moment.
        // (Context Finder would actually work just fine as our test probe uses dynamic package import).
        System.out.println("TCCL " + Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(null);

        new ANastyReflectionUser().callMyPrivateClassThroughReflection();
        
        // Doesn't work? Now take a look at bundle definitions above and uncomment buddy classloading related 
        // headers
    }

    @Test
    public void testContextFinder() throws Exception {

        // Equinox sets Thread Context ClassLoader to Context finder by default.
        // Apache Felix sets TCCL to the classloader of the bundle that it calls.
        // Thread.currentThread().setContextClassLoader(null);

        System.out.println("TCCL " + Thread.currentThread().getContextClassLoader());
        new MyPrivateClass().callMyselfReflectionUser();
    }

}
