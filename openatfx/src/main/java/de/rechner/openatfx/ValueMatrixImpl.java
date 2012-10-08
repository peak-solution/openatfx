package de.rechner.openatfx;

import org.asam.ods.AoException;
import org.asam.ods.Column;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnitIterator;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrixMode;
import org.asam.ods.ValueMatrixPOA;


/**
 * Implementation of <code>org.asam.ods.ValueMatrix</code>.
 * 
 * @author Christian Rechner
 */
class ValueMatrixImpl extends ValueMatrixPOA {

    // private final SubMatrixImpl sourceSubMatrix;
    private final ValueMatrixMode mode;

    public ValueMatrixImpl(SubMatrixImpl sourceSubMatrix, ValueMatrixMode mode) {
        // this.sourceSubMatrix = sourceSubMatrix;
        this.mode = mode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getMode()
     */
    public ValueMatrixMode getMode() throws AoException {
        return this.mode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getColumnCount()
     */
    public int getColumnCount() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listColumns(java.lang.String)
     */
    public String[] listColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getColumns(java.lang.String)
     */
    public Column[] getColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listIndependentColumns(java.lang.String)
     */
    public String[] listIndependentColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getIndependentColumns(java.lang.String)
     */
    public Column[] getIndependentColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getRowCount()
     */
    public int getRowCount() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getValueMeaPoint(int)
     */
    public NameValueUnitIterator getValueMeaPoint(int meaPoint) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getValueVector(org.asam.ods.Column, int, int)
     */
    public TS_ValueSeq getValueVector(Column col, int startPoint, int count) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#removeValueMeaPoint(java.lang.String[], int, int)
     */
    public void removeValueMeaPoint(String[] columnNames, int meaPoint, int count) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#removeValueVector(org.asam.ods.Column, int, int)
     */
    public void removeValueVector(Column col, int startPoint, int count) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#setValueMeaPoint(org.asam.ods.SetType, int, org.asam.ods.NameValue[])
     */
    public void setValueMeaPoint(SetType set, int meaPoint, NameValue[] value) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#setValueVector(org.asam.ods.Column, org.asam.ods.SetType, int,
     *      org.asam.ods.TS_ValueSeq)
     */
    public void setValueVector(Column col, SetType set, int startPoint, TS_ValueSeq value) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#setValue(org.asam.ods.SetType, int, org.asam.ods.NameValueSeqUnit[])
     */
    public void setValue(SetType set, int startPoint, NameValueSeqUnit[] value) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#addColumn(org.asam.ods.NameUnit)
     */
    public Column addColumn(NameUnit newColumn) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listScalingColumns(java.lang.String)
     */
    public String[] listScalingColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getScalingColumns(java.lang.String)
     */
    public Column[] getScalingColumns(String colPattern) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listColumnsScaledBy(org.asam.ods.Column)
     */
    public String[] listColumnsScaledBy(Column scalingColumn) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getColumnsScaledBy(org.asam.ods.Column)
     */
    public Column[] getColumnsScaledBy(Column scalingColumn) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#addColumnScaledBy(org.asam.ods.NameUnit, org.asam.ods.Column)
     */
    public Column addColumnScaledBy(NameUnit newColumn, Column scalingColumn) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    public NameValueSeqUnit[] getValue(Column[] columns, int startPoint, int count) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    public void destroy() throws AoException {
        // do nothing
    }

}
