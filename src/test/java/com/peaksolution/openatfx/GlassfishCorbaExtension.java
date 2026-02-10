package com.peaksolution.openatfx;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class GlassfishCorbaExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");
        System.setProperty("com.sun.corba.ee.spi.orb.ORBDisableGMBAL", "true");
    }

}
