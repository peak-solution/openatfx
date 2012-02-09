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
     * @param props Converter properties.
     * @throws ConvertException Error performing conversion.
     */
    public void convertFiles(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException;

    /**
     * Converts all files having the ending '*.dat' in given directory the given ATFX file.
     * 
     * @param directory The directory to search for ATFX files.
     * @param atfxFile The ATFX file.
     * @param props Converter properties.
     * @throws ConvertException Error performing conversion.
     */
    public void convertDirectory(File directory, File atfxFile, Properties props) throws ConvertException;

}
