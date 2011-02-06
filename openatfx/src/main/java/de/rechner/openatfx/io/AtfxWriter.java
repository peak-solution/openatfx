package de.rechner.openatfx.io;

import java.io.File;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;


/**
 * Object for writing ATFX files.
 * 
 * @author Christian Rechner
 */
class AtfxWriter {

    /** The singleton instance */
    private static AtfxWriter instance;

    /**
     * Non visible constructor.
     */
    private AtfxWriter() {}

    /**
     * Writes the complete content of given aoSession to specified XML file.
     * 
     * @param xmlFile The XML file.
     * @param aoSession The session.
     * @throws AoException Error writing XML file.
     */
    public void writeXML(File xmlFile, AoSession aoSession) throws AoException {

    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static AtfxWriter getInstance() {
        if (instance == null) {
            instance = new AtfxWriter();
        }
        return instance;
    }

}
