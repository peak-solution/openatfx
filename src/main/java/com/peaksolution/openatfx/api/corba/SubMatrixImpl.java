package com.peaksolution.openatfx.api.corba;

import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.Column;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrixOperations;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixHelper;
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import com.peaksolution.openatfx.api.AtfxInstance;
import com.peaksolution.openatfx.api.NameValueUnit;


/**
 * Implementation of <code>org.asam.ods.SubMatrix</code>.
 * 
 * @author Christian Rechner
 */
class SubMatrixImpl extends InstanceElementImpl implements SubMatrixOperations {
    private final NameValueUnit valueMatrixModeValue;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param delegate The delegate instance.
     * @param valueMatrixModeValue 
     */
    public SubMatrixImpl(POA modelPOA, POA instancePOA, CorbaAtfxCache atfxCache, AtfxInstance delegate, NameValueUnit valueMatrixModeValue) {
        super(modelPOA, instancePOA, atfxCache, delegate);
        
        this.valueMatrixModeValue = valueMatrixModeValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.SubMatrixOperations#listColumns(java.lang.String)
     */
    public String[] listColumns(String colPattern) throws AoException {
        List<String> list = new ArrayList<>();
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
            if (valueMatrixModeValue != null && valueMatrixModeValue.getValue().stringVal().equalsIgnoreCase("STORAGE")) {
                vmMode = ValueMatrixMode.STORAGE;
            }

            // create ValueMatrix object
            ValueMatrixOnSubMatrixImpl valueMatrixImpl = new ValueMatrixOnSubMatrixImpl(this.modelPOA, this, vmMode);
            return ValueMatrixHelper.unchecked_narrow(modelPOA.servant_to_reference(valueMatrixImpl));
        } catch (ServantNotActive | WrongPolicy e) {
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
            ValueMatrixOnSubMatrixImpl valueMatrixImpl = new ValueMatrixOnSubMatrixImpl(this.modelPOA, this, vmMode);
            return ValueMatrixHelper.unchecked_narrow(modelPOA.servant_to_reference(valueMatrixImpl));
        } catch (ServantNotActive | WrongPolicy e) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

}
