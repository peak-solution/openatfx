package de.rechner.openatfx.exporter;

import java.io.File;
import java.util.Properties;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ElemId;


/**
 * Interface for data exporter.
 * 
 * @author Christian Rechner
 */
public interface IExporter {

    /**
     * Performs a data export to an ATFX file.
     * 
     * @param sourceSession The source session.
     * @param sourceElemIds The source elem ids.
     * @param targetFile The target ATFX file, must no yet exist.
     * @throws AoException Error exporting.
     */
    public void export(AoSession sourceSession, ElemId[] sourceElemIds, File targetFile, Properties props)
            throws AoException;

}
