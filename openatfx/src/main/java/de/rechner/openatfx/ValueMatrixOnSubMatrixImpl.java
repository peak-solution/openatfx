package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.Column;
import org.asam.ods.ColumnHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameUnit;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.NameValueUnit;
import org.asam.ods.NameValueUnitIterator;
import org.asam.ods.Relationship;
import org.asam.ods.SetType;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrixMode;
import org.asam.ods.ValueMatrixPOA;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


/**
 * Implementation of <code>org.asam.ods.ValueMatrix</code>.
 * 
 * @author Christian Rechner
 */
class ValueMatrixOnSubMatrixImpl extends ValueMatrixPOA {

    private static final Log LOG = LogFactory.getLog(ValueMatrixOnSubMatrixImpl.class);

    private final POA modelPOA;
    private final SubMatrixImpl sourceSubMatrix;
    private final ValueMatrixMode mode;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param sourceSubMatrix The SubMatrix object.
     * @param mode The ValueMatrixMode.
     */
    public ValueMatrixOnSubMatrixImpl(POA modelPOA, SubMatrixImpl sourceSubMatrix, ValueMatrixMode mode) {
        this.modelPOA = modelPOA;
        this.sourceSubMatrix = sourceSubMatrix;
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
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
        int cnt = iter.getCount();
        iter.destroy();
        return cnt;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getRowCount()
     */
    public int getRowCount() throws AoException {
        NameValueUnit nvu = this.sourceSubMatrix.getValueByBaseName("number_of_rows");
        if (nvu.value.flag == 15) {
            return nvu.value.u.longVal();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listColumns(java.lang.String)
     */
    public String[] listColumns(String colPattern) throws AoException {
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, colPattern);
        InstanceElement[] ies = iter.nextN(iter.getCount());
        iter.destroy();

        List<String> list = new ArrayList<String>(ies.length);
        for (int i = 0; i < ies.length; i++) {
            list.add(ies[i].getName());
            ies[i].destroy();
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#listIndependentColumns(java.lang.String)
     */
    public String[] listIndependentColumns(String colPattern) throws AoException {
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, colPattern);
        InstanceElement[] ies = iter.nextN(iter.getCount());
        iter.destroy();

        List<String> list = new ArrayList<String>(ies.length);
        for (int i = 0; i < ies.length; i++) {
            NameValueUnit nvu = ies[i].getValueByBaseName("independent");
            if (nvu.value.flag == 15 && nvu.value.u.shortVal() > 0) {
                list.add(ies[i].getName());
            }
            ies[i].destroy();
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getColumns(java.lang.String)
     */
    public Column[] getColumns(String colPattern) throws AoException {
        try {
            InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD,
                                                                                             colPattern);
            InstanceElement[] ies = iter.nextN(iter.getCount());
            iter.destroy();

            List<Column> list = new ArrayList<Column>(ies.length);
            for (int i = 0; i < ies.length; i++) {
                ColumnOnSubMatrixImpl columnImpl = new ColumnOnSubMatrixImpl(ies[i], this.mode);
                Column column = ColumnHelper.unchecked_narrow(modelPOA.servant_to_reference(columnImpl));
                list.add(column);
            }
            return list.toArray(new Column[0]);
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
     * @see org.asam.ods.ValueMatrixOperations#getIndependentColumns(java.lang.String)
     */
    public Column[] getIndependentColumns(String colPattern) throws AoException {
        try {
            InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD,
                                                                                             colPattern);
            InstanceElement[] ies = iter.nextN(iter.getCount());
            iter.destroy();

            List<Column> list = new ArrayList<Column>(ies.length);
            for (int i = 0; i < ies.length; i++) {
                NameValueUnit nvu = ies[i].getValueByBaseName("independent");
                if (nvu.value.flag == 15 && nvu.value.u.shortVal() > 0) {
                    ColumnOnSubMatrixImpl columnImpl = new ColumnOnSubMatrixImpl(ies[i], this.mode);
                    Column column = ColumnHelper.unchecked_narrow(modelPOA.servant_to_reference(columnImpl));
                    list.add(column);
                }
            }
            return list.toArray(new Column[0]);
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
     * @see org.asam.ods.ValueMatrixOperations#getValueVector(org.asam.ods.Column, int, int)
     */
    public TS_ValueSeq getValueVector(Column col, int startPoint, int count) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ValueMatrixOperations#getValue(org.asam.ods.Column[], int, int)
     */
    public NameValueSeqUnit[] getValue(Column[] columns, int startPoint, int count) throws AoException {
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
     * @see org.asam.ods.ValueMatrixOperations#destroy()
     */
    public void destroy() throws AoException {
        try {
            byte[] id = this.modelPOA.servant_to_id(this);
            this.modelPOA.deactivate_object(id);
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ObjectNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
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

}
