package com.peaksolution.openatfx.api.corba;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.MeasurementOperations;
import org.asam.ods.SMatLink;
import org.asam.ods.SeverityFlag;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;

import com.peaksolution.openatfx.api.AtfxInstance;


/**
 * Implementation of <code>org.asam.ods.Measurement</code>.
 * 
 * @author Christian Rechner
 */
class MeasurementImpl extends InstanceElementImpl implements MeasurementOperations {

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param delegate The delegate instance.
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public MeasurementImpl(POA modelPOA, POA instancePOA, CorbaAtfxCache atfxCache, AtfxInstance delegate) {
        super(modelPOA, instancePOA, atfxCache, delegate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.MeasurementOperations#getValueMatrix()
     */
    public ValueMatrix getValueMatrix() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.MeasurementOperations#getValueMatrixInMode(org.asam.ods.ValueMatrixMode)
     */
    public ValueMatrix getValueMatrixInMode(ValueMatrixMode vmMode) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.MeasurementOperations#createSMatLink()
     */
    public SMatLink createSMatLink() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.MeasurementOperations#getSMatLinks()
     */
    public SMatLink[] getSMatLinks() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.MeasurementOperations#removeSMatLink(org.asam.ods.SMatLink)
     */
    public void removeSMatLink(SMatLink smLink) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

}
