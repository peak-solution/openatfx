package de.rechner.openatfx.converter;

import java.io.File;
import java.util.Properties;


/**
 * Interface for converters.
 * 
 * @author Christian Rechner
 */
public interface IConverter {

    /**
     * Converts
     * 
     * @param sourceFiles
     * @param atfxFile
     * @param props
     * @throws ConvertException
     */
    public void convert(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException;

}
