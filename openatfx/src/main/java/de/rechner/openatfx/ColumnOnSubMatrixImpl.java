package de.rechner.openatfx;

import org.asam.ods.AoException;
import org.asam.ods.ColumnPOA;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.ValueMatrixMode;


/**
 * Implementation of <code>org.asam.ods.Column</code>.
 * 
 * @author Christian Rechner
 */
class ColumnOnSubMatrixImpl extends ColumnPOA {

    // private final InstanceElement ieLocalColumn;
    // private final ValueMatrixMode mode;

    public ColumnOnSubMatrixImpl(InstanceElement ieLocalColumn, ValueMatrixMode mode) {
//        this.ieLocalColumn = ieLocalColumn;
//        this.mode = mode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getFormula()
     */
    public String getFormula() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getName()
     */
    public String getName() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ColumnOperations#getSourceMQ()
     */
    public InstanceElement getSourceMQ() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
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
     * @see org.asam.ods.ColumnOperations#isIndependent()
     */
    public boolean isIndependent() throws AoException {
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
     * @see org.asam.ods.ColumnOperations#getDataType()
     */
    public DataType getDataType() throws AoException {
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
