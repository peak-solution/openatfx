package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.file.Path;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.omg.CORBA.ORB;


/**
 * Main class for opening / converting MDF4 files with the ASAM ODS OO-API abstraction layer.
 * 
 * @author Christian Rechner
 */
public class MDF4Reader {

    /**
     * Opens an MDF4-file and gives full access to all its contents via the ASAM ODS OO-API interface.
     * 
     * @param orb The ORB.
     * @param mdfFile The source file.
     * @return The ASAM ODS session object.
     * @throws AoException Error creating ASAM ODS session.
     * @throws IOException Error reading MDF4 file.
     */
    public AoSession getAoSessionForMDF4(ORB orb, Path mdfFile) throws AoException, IOException {
        
        
        
        return null;
    }
}
