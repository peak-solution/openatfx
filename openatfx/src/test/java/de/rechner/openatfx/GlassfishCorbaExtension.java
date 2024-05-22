package de.rechner.openatfx;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class GlassfishCorbaExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.setProperty("org.glassfish.gmbal.no.multipleUpperBoundsException", "true");
    }

}
