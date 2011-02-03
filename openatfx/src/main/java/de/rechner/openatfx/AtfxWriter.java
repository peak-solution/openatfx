package de.rechner.openatfx;

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
