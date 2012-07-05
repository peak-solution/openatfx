package de.rechner.openatfx.io;

import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;
import org.asam.ods.TS_ValueSeq;


public class ExtCompReader {

    /** The singleton instance */
    private static volatile ExtCompReader instance;

    private TS_ValueSeq readValues(InstanceElement[] ieExtComps) throws AoException {
        return null;
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static ExtCompReader getInstance() {
        if (instance == null) {
            instance = new ExtCompReader();
        }
        return instance;
    }

}
