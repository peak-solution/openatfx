package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.Column;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValue;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrixOperations;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixHelper;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


/**
 * Implementation of <code>org.asam.ods.SubMatrix</code>.
 * 
 * @author Christian Rechner
 */
class SubMatrixImpl extends InstanceElementImpl implements SubMatrixOperations {

    private static final Log LOG = LogFactory.getLog(SubMatrixImpl.class);

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param aid The application element id.
     * @param iid The instance id.
     */
    public SubMatrixImpl(POA modelPOA, POA instancePOA, AtfxCache atfxCache, long aid, long iid) {
        super(modelPOA, instancePOA, atfxCache, aid, iid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.SubMatrixOperations#listColumns(java.lang.String)
     */
    public String[] listColumns(String colPattern) throws AoException {
        List<String> list = new ArrayList<String>();
        InstanceElementIterator iter = getRelatedInstancesByRelationship(Relationship.CHILD, colPattern);
        for (int i = 0; i < iter.getCount(); i++) {
            list.add(iter.nextOne().getName());
        }
        iter.destroy();
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.SubMatrixOperations#getColumns(java.lang.String)
     */
    public Column[] getColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.SubMatrixOperations#getValueMatrix()
     */
    public ValueMatrix getValueMatrix() throws AoException {
        try {
            // read mode from session
            ValueMatrixMode vmMode = ValueMatrixMode.CALCULATED;
            NameValue nv = this.atfxCache.getContext().get("VALUEMATRIX_MODE");
            if (nv != null && nv.value.u.stringVal().equalsIgnoreCase("STORAGE")) {
                vmMode = ValueMatrixMode.STORAGE;
            } else if (nv != null && nv.value.u.stringVal().equalsIgnoreCase("CALCULATED")) {
                vmMode = ValueMatrixMode.CALCULATED;
            }

            // create ValueMatrix object
            ValueMatrixImpl valueMatrixImpl = new ValueMatrixImpl(this, vmMode);
            ValueMatrix valueMatrix = ValueMatrixHelper.unchecked_narrow(modelPOA.servant_to_reference(valueMatrixImpl));
            return valueMatrix;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.SubMatrixOperations#getValueMatrixInMode(org.asam.ods.ValueMatrixMode)
     */
    public ValueMatrix getValueMatrixInMode(ValueMatrixMode vmMode) throws AoException {
        try {
            ValueMatrixImpl valueMatrixImpl = new ValueMatrixImpl(this, vmMode);
            ValueMatrix valueMatrix = ValueMatrixHelper.unchecked_narrow(modelPOA.servant_to_reference(valueMatrixImpl));
            return valueMatrix;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

}
