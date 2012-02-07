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
     * Converts given source files to ASAM ODS ATFX. All measurement data will be merged into one ATFX file.
     * 
     * @param sourceFiles List of source files.
     * @param atfxFile The target ATFX file.
     * @param props Convert properties.
     * @throws ConvertException Error performing conversion.
     */
    public void convert(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException;

}
