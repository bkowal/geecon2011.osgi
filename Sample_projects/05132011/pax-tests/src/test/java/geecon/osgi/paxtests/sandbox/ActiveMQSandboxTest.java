package geecon.osgi.paxtests.sandbox;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.options;
import geecon.osgi.paxtests.AbstractIntegrationTest;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;


@RunWith(JUnit4TestRunner.class)
public class ActiveMQSandboxTest extends AbstractIntegrationTest {
    public Option[] testSpecificConfiguration() {

        Option[] testSpecificOptions = options(

        // Please remember that this generated bundle has default
        // startUp level.
        // It means that it may not be fully initialized when your app
        // starts.
        generateActiveMQBrokerBundle("sandboxTest", this.getClass()
                .getClassLoader().getResourceAsStream(
                        "localResources/sandbox-activemq-broker.xml"))

        );

        // combine the broker with its prerequisites
        return OptionUtils.combine(activemq53configuration(),
                testSpecificOptions);

    }

    @Test
    public void testSendLocal() throws Exception {

        Object brokersAppContext = waitForActiveMQBrokerStartup("sandboxTest",
                10000);
        assertNotNull(brokersAppContext);

        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(
                "tcp://localhost:61619");
        Connection connection = cf.createConnection();
        assertNotNull(connection);
        connection.close();
    }

}
