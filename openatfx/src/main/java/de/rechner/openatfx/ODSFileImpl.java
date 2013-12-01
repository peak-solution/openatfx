package de.rechner.openatfx;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.ODSFileOperations;
import org.asam.ods.ODSReadTransfer;
import org.asam.ods.ODSWriteTransfer;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;


/**
 * Implementation of <code>org.asam.ods.ODSFile</code>.
 * 
 * @author Christian Rechner
 */
class ODSFileImpl extends InstanceElementImpl implements ODSFileOperations {

    // private static final Log LOG = LogFactory.getLog(ODSFileImpl.class);

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public ODSFileImpl(POA modelPOA, POA instancePOA, AtfxCache atfxCache, long aid, long iid) {
        super(modelPOA, instancePOA, atfxCache, aid, iid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#exists()
     */
    public boolean exists() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#canRead()
     */
    public boolean canRead() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#canWrite()
     */
    public boolean canWrite() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#create()
     */
    public ODSWriteTransfer create() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#getDate()
     */
    public String getDate() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#getSize()
     */
    public T_LONGLONG getSize() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#remove()
     */
    public void remove() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#read()
     */
    public ODSReadTransfer read() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#takeUnderControl(java.lang.String)
     */
    public void takeUnderControl(String sourceUrl) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#removeFromControl(java.lang.String)
     */
    public void removeFromControl(String targetUrl) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#append()
     */
    public ODSWriteTransfer append() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

}
