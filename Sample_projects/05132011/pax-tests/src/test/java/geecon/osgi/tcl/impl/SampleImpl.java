
package geecon.osgi.tcl.impl;

import geecon.osgi.tcl.SampleInterface;

import org.apache.log4j.Logger;


public class SampleImpl implements SampleInterface {

    private static final Logger log = Logger.getLogger(SampleImpl.class);

    public void doIt() {
        log.info("DONE !!!!!!!!!");
    }

}
