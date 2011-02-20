package de.rechner.openatfx;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoFactoryPOA;
import org.asam.ods.AoSession;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.io.DOMatfxReader;


/**
 * Implementation of <code>org.asam.ods.AoFactory</code>.
 * 
 * @author Christian Rechner
 */
class AoFactoryImpl extends AoFactoryPOA {

    private static final Log LOG = LogFactory.getLog(AoFactoryImpl.class);

    private final ORB orb;

    /**
     * Creates a new AoFactory object.
     * 
     * @param orb The ORB.
     */
    public AoFactoryImpl(ORB orb) {
        this.orb = orb;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getInterfaceVersion()
     */
    public String getInterfaceVersion() throws AoException {
        return "V5.2.0";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getType()
     */
    public String getType() throws AoException {
        return "XATF-ASCII";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getName()
     */
    public String getName() throws AoException {
        return "XATF-ASCII";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getDescription()
     */
    public String getDescription() throws AoException {
        return "ATFX file driver for ASAM OO-API";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#newSession(java.lang.String)
     */
    public AoSession newSession(String auth) throws AoException {
        try {
            File atfxFile = null;
            for (String str : auth.split(",")) {
                String[] parts = str.split("=");
                if (parts.length == 2 && parts[0].toUpperCase().equals("FILENAME")) {
                    atfxFile = new File(parts[1]);
                }
            }
            if (atfxFile == null || !atfxFile.exists()) {
                throw new AoException(ErrorCode.AO_ACCESS_DENIED, SeverityFlag.ERROR, 0, "Unable to open ATFX file: "
                        + auth);
            }
            return DOMatfxReader.getInstance().createSessionForATFX(orb, atfxFile);
        } catch (AoException aoe) {
            LOG.error(aoe.reason, aoe);
            throw aoe;
        }
    }

}
