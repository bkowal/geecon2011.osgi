package geecon.osgi.spi;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class SpiClient {

    public void useSpi() {
        try {
            // this cannot work as there's no such package

            // the only purpose of this invocation is to see what class is used
            // at runtime when providing the JAXB impl; is it the build-in one
            // (com.sun...internal) or the one fetched through SPI from an
            // external impl?

            JAXBContext.newInstance("com.bartek.whocares");
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
