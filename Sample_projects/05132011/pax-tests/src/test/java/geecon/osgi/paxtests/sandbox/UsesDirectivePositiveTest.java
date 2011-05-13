package geecon.osgi.paxtests.sandbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;
import entities.Person;
import geecon.osgi.paxtests.AbstractIntegrationTest;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import store.Persister;

/**
 * See slide No. 10. Bundles defined here are exactly the same as the ones shown in that slide.
 * 
 * This test works - all bundles are active. It's because all bundles are deployed at the same time, so the container tries to find the best
 * possible configuration. Both the Persistence and the Client bundle get package 'entities' in version 1.0.
 * 
 * @author Bartosz Kowalewski
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class UsesDirectivePositiveTest extends AbstractIntegrationTest {

    public Option[] testSpecificConfiguration() {

        Option[] testSpecificOptions =
                options(

                // Bundle-SymbolicName: entities10
                        // Bundle-Name: entities10
                        // Bundle-Version: 1.0
                        // Export-Package: entities;version="1.0"

                        // -noimport:=true : be careful, metadata is preprocessed during bundle generation; the tool (BND library) might add
                        // a corresponding import statement to the export (entities); this might cause only a.a in ver. 1.1 to be present in
                        // the env;
                        provision(
                                TinyBundles.newBundle()
                                .set(Constants.BUNDLE_SYMBOLICNAME,"entities10")
                                .set(Constants.BUNDLE_NAME, "entities10")
                                .set(Constants.BUNDLE_VERSION, "1.0")
                                .set(Constants.IMPORT_PACKAGE, "")
                                .set(Constants.EXPORT_PACKAGE, "entities;version=\"1.0\";-noimport:=true")
                                .add(Person.class)
                                .build(withBnd())
                                ),

                        // Bundle-SymbolicName: entities11
                        // Bundle-Name: entities11
                        // Bundle-Version: 1.1
                        // Export-Package: entities;version="1.1"

                        provision(
                                TinyBundles.newBundle()
                                .set(Constants.BUNDLE_SYMBOLICNAME, "entities11")
                                .set(Constants.BUNDLE_NAME, "entities11")
                                .set(Constants.BUNDLE_VERSION, "1.1")
                                .set(Constants.IMPORT_PACKAGE, "")
                                .set(Constants.EXPORT_PACKAGE, "entities;version=\"1.1\";-noimport:=true")
                                .add(Person.class)
                                .build(withBnd())
                                ),

                      

                                
                        // Bundle-SymbolicName: store20
                        // Bundle-Name: store20
                        // Bundle-Version: 2.0
                        // Import-Package: entities;version="[1.0,1.1]"
                        // Export-Package: store;uses:=entities;version="2.0"

                        provision(
                                TinyBundles.newBundle()
                                .set(Constants.BUNDLE_SYMBOLICNAME, "store20")
                                .set(Constants.BUNDLE_NAME, "store20")
                                .set(Constants.BUNDLE_VERSION, "2.0")
                                .set(Constants.IMPORT_PACKAGE, "entities;version=\"[1.0,1.1]\"")
                                .set(Constants.EXPORT_PACKAGE, "store;version=\"2.0\";uses:=\"entities\"")
                                .add(Persister.class)
                                .build(withBnd())
                                ),
                                
                                // Bundle-SymbolicName: client10
                                // Bundle-Name: client10
                                // Bundle-Version: 1.0
                                // Import-Package: entities;version="[1.0,1.0]",store;version="[2.0, 2.0]"

                                provision(
                                         TinyBundles.newBundle()
                                         .set(Constants.BUNDLE_SYMBOLICNAME, "client10")
                                         .set(Constants.BUNDLE_NAME, "client10")
                                         .set(Constants.BUNDLE_VERSION, "1.0")
                                         .set(Constants.IMPORT_PACKAGE, "entities;version=\"[1.0,1.0]\", store;version=\"[2.0, 2.0]\"")
                                         .set(Constants.EXPORT_PACKAGE, "")
                                         .build(withBnd())
                                         ),

                        // Oh gosh, if you change order of the last two bundles, the test will fail under Felix, but it will pass when 
                        // Equinox is used:).

                        new Customizer() {

                            @Override
                            public InputStream customizeTestProbe(InputStream testProbe) throws Exception {

                                TinyBundle bundle = TinyBundles.modifyBundle(testProbe)

                                .removeResource("entities/Person.class").removeResource("entities/")

                                .removeResource("store/Persister.class").removeResource("store/")

                                .removeHeader("Private-Package");

                                return bundle.build();
                            }

                        }

                );

        return testSpecificOptions;

    }

    @Test
    public void works() throws Exception {

        // verify if all bundles are active
        for (String symbolicName : new String[]{ "entities10", "entities11", "store20", "client10" }) {
            verifyBundleIsActive(symbolicName);
        }

        // the test is failing ? go to the super class (AbstractIntegrationTest), change the framework from equinox() to felix() and rerun
        // the test; detailed info related to package resolution problems should be printed now;

    }

    private void verifyBundleIsActive(String symbolicName) {
        Bundle aaprovider1 = getBundle(symbolicName);
        assertNotNull(aaprovider1);
        assertEquals("Bundle with symbolic name " + symbolicName + " is not active.", Bundle.ACTIVE, aaprovider1.getState());
    }

}
