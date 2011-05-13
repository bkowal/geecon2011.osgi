package geecon.osgi.paxtests;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.bundleStartLevel;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.rawPaxRunnerOption;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public abstract class AbstractIntegrationTest {

    public static final long DEFAULT_TIMEOUT = 30000;

    private static final String APPLICATION_CONTEXT_CLASS =
            "org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext";

    private static final String BLUEPRINT_CONTAINER_CLASS = "org.osgi.service.blueprint.container.BlueprintContainer";

    private static final String LOCAL_RESOURCES_DIR_NAME = "localResources";

    protected static final String BROKER_BUNDLE_SYMBOLIC_NAME_PREFIX = "geecon.amqbroker.";

    @Inject
    protected BundleContext bundleContext;

    private List<ServiceTracker> srs;

    @Before
    public void setUp() {
        srs = new ArrayList<ServiceTracker>();
    }

    @After
    public void tearDown() throws Exception {
        for (ServiceTracker st : srs) {
            if (st != null) {
                st.close();
            }
        }
    }

    protected Bundle getBundle(String symbolicName) {
        return getBundle(symbolicName, null);
    }

    @SuppressWarnings("unchecked")
    protected Bundle getBundle(String bundleSymbolicName, String version) {
        Bundle result = null;
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(bundleSymbolicName)) {

                Dictionary headers = b.getHeaders();
                Object bundleVersion = headers.get(Constants.BUNDLE_VERSION);
                if (version == null || (bundleVersion != null && bundleVersion.equals(Version.parseVersion(version)))) {
                    result = b;
                    break;
                }
            }
        }
        return result;
    }

    public static MavenArtifactProvisionOption addMavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        return getOsgiService(null, type, filter, timeout);
    }

    protected <T> T getOsgiService(BundleContext bc, Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bc == null ? bundleContext : bc, osgiFilter, null);
            tracker.open();

            // add tracker to the list of trackers we close at tear down
            srs.add(tracker);

            Object x = tracker.waitForService(timeout);
            Object svc = type.cast(x);
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Configuration
    public Option[] configuration() {

        // combine options
        Option[] options = coreOptions();
        options = OptionUtils.combine(options, servicemix42Configuration());
        Option[] testSpecificConfiguration = testSpecificConfiguration();
        if (testSpecificConfiguration != null) {
            options = OptionUtils.combine(options, testSpecificConfiguration);
        }
        return options;

    }

    protected abstract Option[] testSpecificConfiguration();

    public Option[] coreOptions() {
        Option[] coreOptions =
                options(

                 felix(),
//                        equinox(),
                        // EXTREMELY IMPORTANT !!!
                        rawPaxRunnerOption("--ee", "J2SE-1.5"),

                        bootDelegationPackages("sun.*", "com.sun.*", "javax.transaction", "javax.transaction.*"),

                        systemPackages("com.sun.org.apache.xalan.internal.xsltc.trax", "com.sun.org.apache.xerces.internal.dom",
                                "com.sun.org.apache.xerces.internal.jaxp", "com.sun.org.apache.xerces.internal.xni"),

                        // We need to be sure that the framework is fully initialized
                        waitForFrameworkStartup(),

                        frameworkStartLevel(88), bundleStartLevel(77),
                        // Include Pax Logging bundle - Logging Service, support for
                        // log4j, slf4j, etc.
                        // logProfile(),
                        // log level
                        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

                        // disable Camel JMX
                        systemProperty("org.apache.camel.jmx.disabled").value("true"), systemProperty("mom.config.dir").value(
                                "file:configurations/services/"),

                        systemProperty("karaf.startLocalConsole").value("false"), systemProperty("karaf.startRemoteShell").value("false"),

                        // systemProperty("org.apache.servicemix.specs.debug=true").value("true"),
                        // Support for OSGi Configuration Admin Service
                        // configProfile(),

                        // Let's add other maven repos. Some of the repos listed below
                        // are already used by Pax Exam.
                        repositories("http://repo.fusesource.com/maven2/", "http://repo.fusesource.com/maven2-all/",
                                "https://maven2-repository.dev.java.net/", "http://dist.wso2.org/maven2/"),

                        workingDirectory(System.getProperty("user.dir") + "/paxrunner/"),

                        // Let's remove the 'localResources' directory from the bundle.
                        // These resources are to be used in the JUnit (Pax Exam) test
                        // and not in the bundle generated by Pax Exam during the test
                        // process.
                        new Customizer() {

                            @Override
                            public InputStream customizeTestProbe(InputStream testProbe) throws Exception {
                                TinyBundle bundle = TinyBundles.modifyBundle(testProbe);

                                URL dir = getClass().getClassLoader().getResource(LOCAL_RESOURCES_DIR_NAME);

                                // is it possible to have dir == null ?
                                if (dir != null) {
                                    URI dirUri = dir.toURI();
                                    int lastIndexOfDir = dirUri.toString().lastIndexOf(LOCAL_RESOURCES_DIR_NAME);

                                    // is it possible to have lastIndexOfDir == -1 ?
                                    if (lastIndexOfDir != -1) {
                                        // remove all files contained in the dir;
                                        // TODO do we need to do it recursively?
                                        for (File resource : new File(dirUri).listFiles()) {
                                            String file = resource.toURI().toString().substring(lastIndexOfDir);
                                            if (file.contains(LOCAL_RESOURCES_DIR_NAME)) {
                                                bundle.removeResource(file);
                                            }
                                        }
                                    }
                                }

                                // remove the localResources directory itself
                                bundle.removeResource("localResources/");
                                return bundle.build();
                            }
                        }

                );

        return coreOptions;

    }

    public Option[] servicemix42Configuration() {

        return options(

        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4").startLevel(7),

        // Include Pax Logging bundle - Logging Service, support for
                // log4j, slf4j, etc.
                mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").version("1.6.2").startLevel(8),

                mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-service").version("1.6.2").startLevel(8),

                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.fileinstall").version("3.1.10").startLevel(11),

                // mavenBundle().groupId("org.apache.geronimo.blueprint")
                // .artifactId("geronimo-blueprint").version("1.0.0")
                // .startLevel(20),

                mavenBundle().groupId("asm").artifactId("asm-all").version("3.2")
                        .startLevel(20),
                
                mavenBundle().groupId("org.apache.aries").artifactId("org.apache.aries.util").version("0.3")
                        .startLevel(20),

                mavenBundle().groupId("org.apache.aries.proxy").artifactId("org.apache.aries.proxy").version("0.3").startLevel(20),

                mavenBundle().groupId("org.apache.aries.blueprint").artifactId("org.apache.aries.blueprint").version("0.3.1")
                        .startLevel(20),
                        
                // mavenBundle().groupId("org.apache.felix.gogo").artifactId(
                // "org.apache.felix.gogo.runtime").version("0.2.2")
                // .startLevel(30),
                //
                // mavenBundle().groupId("org.apache.servicemix.bundles")
                // .artifactId("org.apache.servicemix.bundles.saaj-impl")
                // .version("1.3.2_2"),
                //
                // mavenBundle()
                // .groupId("org.apache.servicemix.specs")
                // .artifactId("org.apache.servicemix.specs.jaxws-api-2.2")
                // .version("1.7.0"),
                //
                // mavenBundle().groupId("org.apache.servicemix.bundles")
                // .artifactId("org.apache.servicemix.bundles.saxon")
                // .version("9.1.0.8_1"),

                mavenBundle().groupId("org.ops4j.pax.confman").artifactId("pax-confman-propsloader").version("0.2.2"),

                // A required CXF dependency
                // mavenBundle()
                // .groupId("org.apache.servicemix.bundles")
                // .artifactId("org.apache.servicemix.bundles.fastinfoset")
                // .version("1.2.7_3"),

                // A required CXF dependency
                // mavenBundle().groupId("org.apache.servicemix.bundles")
                // .artifactId("org.apache.servicemix.bundles.xalan")
                // .version("2.7.1_3"),

                scanFeatures("mvn:org.apache.karaf/apache-karaf/2.1.4-fuse-00-09/xml/features", "spring-dm/1.2.0")

        // scanFeatures(
        // "mvn:org.apache.servicemix/apache-servicemix/4.3.1-fuse-01-09/xml/camel-features",
        // "camel-cxf/2.6.0-fuse-01-09"),
        //
        // scanFeatures(
        // "mvn:org.apache.servicemix/apache-servicemix/4.3.1-fuse-01-09/xml/camel-features",
        // "camel-spring/2.6.0-fuse-01-09")

        // *** Don't use these :)
        // scanFeatures(
        // "mvn:org.apache.camel.karaf/apache-camel/2.6.0-fuse-01-09/xml/features",
        // "camel-cxf/2.6.0-fuse-01-09"),
        //
        // scanFeatures(
        // "mvn:org.apache.camel.karaf/apache-camel/2.6.0-fuse-01-09/xml/features",
        // "camel-spring/2.6.0-fuse-01-09")

        );

    }

    protected Option generateActiveMQBrokerBundle(String bundleNameSuffix, InputStream activemqXmlContents) {
        return provision(newBundle().add("META-INF/spring/activemq-broker-" + bundleNameSuffix + ".xml", activemqXmlContents).set(
                Constants.BUNDLE_NAME, bundleNameSuffix).set(Constants.BUNDLE_SYMBOLICNAME,
                BROKER_BUNDLE_SYMBOLIC_NAME_PREFIX + bundleNameSuffix).set(Constants.IMPORT_PACKAGE,
                "javax.transaction,org.apache.activemq," + "org.apache.activemq.pool," + "org.springframework.beans.factory.config").set(
                Constants.EXPORT_PACKAGE, "").set(Constants.DYNAMICIMPORT_PACKAGE, "*").set("Spring-Context",
                "*;publish-context:=true;create-asynchronously:=false").build(withBnd()));

    }

    /**
     * Enables you to wait for the startup of the generated AMQ broker bundle.
     * 
     * @param bundleNameSuffix a suffix that will be appended to 'geecon.amqbroker.' and used as a symbolic name of the generated bundle
     * @param timeout the number of milliseconds to wait for the broker's Spring context to start up
     * @return
     * @throws InterruptedException
     * @throws InvalidSyntaxException
     */
    protected Object waitForActiveMQBrokerStartup(String bundleNameSuffix, long timeout) throws InterruptedException,
            InvalidSyntaxException {
        Filter filter =
                bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + APPLICATION_CONTEXT_CLASS + ")("
                        + Constants.BUNDLE_SYMBOLICNAME + "=" + BROKER_BUNDLE_SYMBOLIC_NAME_PREFIX + bundleNameSuffix + "))");

        ServiceTracker st = new ServiceTracker(bundleContext, filter, null);
        st.open();

        Object brokersAppContext = st.waitForService(timeout);

        st.close();

        return brokersAppContext;
    }

    public Option[] activemq53configuration() {
        return options(

        // Below are the contents of this feature:
                // scanFeatures(
                // "mvn:org.apache.servicemix/apache-servicemix/4.3.1-fuse-01-09/xml/activemq-features",
                // "activemq-spring/5.4.2-fuse-03-09")
                // Unfortunately this feature also loads AMQ commands (for the
                // SMX console). We don't want these and that is why all AMQ
                // bundles are provided explicitly.

                mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-annotation_1.0_spec").version("1.1.1"),

                mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jta_1.1_spec").version("1.1.1"),

                mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-jms_1.1_spec").version("1.1.1"),

                mavenBundle().groupId("org.apache.geronimo.specs").artifactId("geronimo-j2ee-management_1.1_spec").version("1.0.1"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-pool").version(
                        "1.5.4_2"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-collections")
                        .version("3.2.1_1"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-lang").version(
                        "2.4_4"),

                mavenBundle().groupId("commons-codec").artifactId("commons-codec").version("1.4"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.oro").version("2.0.8_4"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.velocity").version(
                        "1.6.2_4"),

                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.jasypt").version("1.6_1"),

                mavenBundle().groupId("org.apache.activemq").artifactId("activemq-core").version("5.4.2-fuse-03-09"),

                mavenBundle().groupId("org.apache.activemq").artifactId("kahadb").version("5.4.2-fuse-03-09"),

                mavenBundle().groupId("org.apache.activemq").artifactId("activemq-ra").version("5.4.2-fuse-03-09"),

                mavenBundle().groupId("org.apache.activemq").artifactId("activemq-pool").version("5.4.2-fuse-03-09"),

                mavenBundle().groupId("org.apache.aries.transaction").artifactId("org.apache.aries.transaction.manager").version(
                        "0.2-incubating"),

                mavenBundle().groupId("org.apache.activemq").artifactId("activemq-spring").version("5.4.2-fuse-03-09"),

                mavenBundle().groupId("org.apache.xbean").artifactId("xbean-spring").version("3.7")

        );

    }

    protected Option generateSpringDmBundle(String symbolicName, Class<?>[] classesToAdd, String[] importedPackages,
            String[] exportedPackages, InputStream springContextContents) {
        // Note: we do not check if parameters are not null; NPEs will be thrown

        TinyBundle generatedBundle = newBundle();
        if (classesToAdd != null) {
            for (Class<?> clazz : classesToAdd) {
                generatedBundle.add(clazz);
            }
        }

        generatedBundle.add("META-INF/spring/generated-" + symbolicName + ".xml", springContextContents);
        generatedBundle.set(Constants.BUNDLE_NAME, symbolicName);
        generatedBundle.set(Constants.BUNDLE_SYMBOLICNAME, symbolicName);

        generatedBundle.set(Constants.DYNAMICIMPORT_PACKAGE, "*");

        StringBuilder sb = new StringBuilder();
        if (importedPackages.length > 0) {
            sb.append(importedPackages[0]);
        }
        for (int i = 1; i < importedPackages.length; i++) {
            sb.append("," + importedPackages[i]);
        }
        generatedBundle.set(Constants.IMPORT_PACKAGE, sb.toString());

        sb.setLength(0);
        if (exportedPackages.length > 0) {
            sb.append(exportedPackages[0]);
        }
        for (int i = 1; i < exportedPackages.length; i++) {
            sb.append("," + exportedPackages[i]);
        }
        generatedBundle.set(Constants.EXPORT_PACKAGE, sb.toString());

        // replace Spring specific names with constants?
        generatedBundle.set("Spring-Context", "*;publish-context:=true;create-asynchronously:=false");

        return provision(generatedBundle.build(withBnd()));
    }

    protected Object waitForGeneratedSpringDmBundleStartup(String symbolicName, long timeout) throws InterruptedException,
            InvalidSyntaxException {

        Filter filter =
                bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + APPLICATION_CONTEXT_CLASS + ")("
                        + Constants.BUNDLE_SYMBOLICNAME + "=" + symbolicName + "))");

        ServiceTracker st = new ServiceTracker(bundleContext, filter, null);
        st.open();

        Object generatedBundleContext = st.waitForService(timeout);

        st.close();

        return generatedBundleContext;
    }

    protected Option generateBlueprintBundle(String symbolicName, Class<?>[] classesToAdd, String[] importedPackages,
            String[] exportedPackages, InputStream blueprintConfigurationContents) {
        // Note: we do not check if parameters are not null; NPEs will be thrown

        TinyBundle generatedBundle = newBundle();
        if (classesToAdd != null) {
            for (Class<?> clazz : classesToAdd) {
                generatedBundle.add(clazz);
            }
        }

        generatedBundle.add("OSGI-INF/blueprint/generated-" + symbolicName + ".xml", blueprintConfigurationContents);
        generatedBundle.set(Constants.BUNDLE_NAME, symbolicName);
        generatedBundle.set(Constants.BUNDLE_SYMBOLICNAME, symbolicName + ";blueprint.aries.xml-validation:=false");

        // generatedBundle.set(Constants.DYNAMICIMPORT_PACKAGE, "*");

        StringBuilder sb = new StringBuilder();

        sb.append("org.osgi.framework;version=\"1.5\"");
        sb.append(",org.osgi.service.blueprint.container;version=\"1.0\"");
        sb.append(",org.osgi.util.tracker;version=\"1.4\"");

        for (int i = 0; i < importedPackages.length; i++) {
            sb.append("," + importedPackages[i]);
        }
        generatedBundle.set(Constants.IMPORT_PACKAGE, sb.toString());

        sb.setLength(0);
        if (exportedPackages.length > 0) {
            sb.append(exportedPackages[0]);
        }
        for (int i = 1; i < exportedPackages.length; i++) {
            sb.append("," + exportedPackages[i]);
        }
        generatedBundle.set(Constants.EXPORT_PACKAGE, sb.toString());

        return provision(generatedBundle.build(withBnd()));
    }

    protected Object waitForGeneratedBlueprintBundleStartup(String symbolicName, long timeout) throws InterruptedException,
            InvalidSyntaxException {

        Filter filter =
                bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + BLUEPRINT_CONTAINER_CLASS + ")("
                        + Constants.BUNDLE_SYMBOLICNAME + "=" + symbolicName + "))");

        ServiceTracker st = new ServiceTracker(bundleContext, filter, null);
        st.open();

        Object generatedBundleContext = st.waitForService(timeout);

        st.close();

        return generatedBundleContext;
    }

    protected static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            int len;
            byte[] b = new byte[1024];
            while ((len = in.read(b)) != -1)
                out.write(b, 0, len);
        } finally {
            in.close();
        }
    }

}