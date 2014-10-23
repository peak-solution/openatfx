package de.rechner.openatfx;

import java.util.Set;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ColumnPOA;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.Column</code>.
 * 
 * @author Christian Rechner
 */
class ColumnOnSubMatrixImpl extends ColumnPOA {

    private final AtfxCache atfxCache;
    private final InstanceElement ieLocalColumn;

    /**
     * Constructor.
     * 
     * @param atfxCache The cache.
     * @param ieLocalColumn The Local Column.
     */
    public ColumnOnSubMatrixImpl(AtfxCache atfxCache, InstanceElement ieLocalColumn) {
        this.atfxCache = atfxCache;
        this.ieLocalColumn = ieLocalColumn;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getSourceMQ()
     */
    public InstanceElement getSourceMQ() throws AoException {
        // get application element derived from 'AoLocalColumn'
        Set<Long> aids = this.atfxCache.getAidsByBaseType("aolocalcolumn");
        if (aids == null || aids.size() != 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "None or multiple application elements of type 'AoLocalColumn' found!");
        }
        long aidLc = aids.iterator().next();

        // get application relation from 'AoLocalColumn' to 'AoMeasurementQuantity'
        ApplicationRelation applRelLcMeq = this.atfxCache.getApplicationRelationByBaseName(aidLc,
                                                                                           "measurement_quantity");
        if (applRelLcMeq == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Application relation derived from base relation 'measurement_quantity' not found!");
        }

        // get related instance
        InstanceElementIterator iter = this.ieLocalColumn.getRelatedInstances(applRelLcMeq, "*");
        if (iter.getCount() != 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "None or multiple related instances found for base relation 'measurement_quantity': "
                                          + this.ieLocalColumn.getAsamPath());
        }
        InstanceElement ieMeq = iter.nextOne();
        iter.destroy();

        return ieMeq;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getName()
     */
    public String getName() throws AoException {
        return getSourceMQ().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getFormula()
     */
    public String getFormula() throws AoException {
        return "";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#isIndependent()
     */
    public boolean isIndependent() throws AoException {
        NameValueUnit nvu = this.ieLocalColumn.getValueByBaseName("independent");
        return (nvu.value.flag == 15) && (nvu.value.u.shortVal() > 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getDataType()
     */
    public DataType getDataType() throws AoException {
        InstanceElement ieMeq = getSourceMQ();
        NameValueUnit nvu = ieMeq.getValueByBaseName("datatype");
        if (nvu.value.flag != 15) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Value of attribute 'datatype' not set:" + ieMeq.getAsamPath());
        }
        return ODSHelper.enum2dataType(nvu.value.u.enumVal());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getUnit()
     */
    public String getUnit() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setFormula(java.lang.String)
     */
    public void setFormula(String formula) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setUnit(java.lang.String)
     */
    public void setUnit(String unit) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#isScaling()
     */
    public boolean isScaling() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setIndependent(boolean)
     */
    public void setIndependent(boolean independent) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setScaling(boolean)
     */
    public void setScaling(boolean scaling) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#destroy()
     */
    public void destroy() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getSequenceRepresentation()
     */
    public int getSequenceRepresentation() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setSequenceRepresentation(int)
     */
    public void setSequenceRepresentation(int sequenceRepresentation) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getGenerationParameters()
     */
    public TS_Union getGenerationParameters() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#setGenerationParameters(org.asam.ods.TS_Union)
     */
    public void setGenerationParameters(TS_Union generationParameters) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getRawDataType()
     */
    public DataType getRawDataType() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

}
