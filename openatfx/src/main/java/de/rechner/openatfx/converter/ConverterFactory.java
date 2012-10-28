package de.rechner.openatfx.converter;

import de.rechner.openatfx.converter.diadem_dat.Dat2AtfxConverter;
import de.rechner.openatfx.converter.maccor_csv.MaccorCSV2AtfxConverter;


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
     * @throws ConvertException Error creating converter.
     */
    public IConverter createConverter(String name) throws ConvertException {
        if (name.equals("diadem_dat2atfx")) {
            return new Dat2AtfxConverter();
        } else if (name.equals("maccor_csv2atfx")) {
            return new MaccorCSV2AtfxConverter();
        }
        throw new ConvertException("Converter not found: " + name);
    }

    /**
     * Lists all available converters.
     * 
     * @return Array of converter name.
     * @throws ConvertException Error listing converters.
     */
    public String[] listConverter() throws ConvertException {
        return new String[] { "diadem_dat2atfx", "maccor_csv2atfx", "digatron_csv2atfx" };
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
    public static synchronized ConverterFactory getInstance() {
        if (instance == null) {
            instance = new ConverterFactory();
        }
        return instance;
    }

}
