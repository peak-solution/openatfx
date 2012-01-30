package de.rechner.openatfx.converter;

import de.rechner.openatfx.converter.diadem_dat.Dat2AtfxConverter;


/**
 * Factory object to get converters.
 * 
 * @author Christian Rechner
 */
public class ConverterFactory {

    /** The singleton instance */
    private static ConverterFactory instance;

    /**
     * Creates a new converter by give name.
     * 
     * @param name The name.
     * @return The converter.
     */
    public IConverter createConverter(String name) throws ConvertException {
        if (name.equals("diadem_dat2atfx")) {
            return new Dat2AtfxConverter();
        }
        throw new ConvertException("Converter not found: " + name);
    }

    /**
     * Non visible constructor.
     */
    private ConverterFactory() {}

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static ConverterFactory getInstance() {
        if (instance == null) {
            instance = new ConverterFactory();
        }
        return instance;
    }

}
