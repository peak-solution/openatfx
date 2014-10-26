package de.rechner.openatfx;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.asam.ods.ValueMatrixMode;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.Column</code>.
 * 
 * @author Christian Rechner
 */
class ColumnImpl extends ColumnPOA {

    private static final Log LOG = LogFactory.getLog(ColumnImpl.class);

    private final POA modelPOA;
    private final AtfxCache atfxCache;
    private final InstanceElement ieLocalColumn;
    private final ValueMatrixMode mode;

    /** is not null if other unit is set */
    private InstanceElement unit = null;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param atfxCache The cache.
     * @param ieLocalColumn The Local Column.
     * @param mode The value matrix mode.
     */
    public ColumnImpl(POA modelPOA, AtfxCache atfxCache, InstanceElement ieLocalColumn, ValueMatrixMode mode) {
        this.modelPOA = modelPOA;
        this.atfxCache = atfxCache;
        this.ieLocalColumn = ieLocalColumn;
        this.mode = mode;
    }

    /**
     * Get the source measurement quantity.
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
     * Get the name of the column.
     * 
     * @see org.asam.ods.ColumnOperations#getName()
     */
    public String getName() throws AoException {
        return getSourceMQ().getName();
    }

    /**
     * Get the formula of the column. As there is currently no specification of a formula within ASAM ODS, this method
     * should not be used; it will return an empty string if used.
     * 
     * @see org.asam.ods.ColumnOperations#getFormula()
     */
    public String getFormula() throws AoException {
        return "";
    }

    /**
     * Is the column an independent column.
     * 
     * @see org.asam.ods.ColumnOperations#isIndependent()
     */
    public boolean isIndependent() throws AoException {
        NameValueUnit nvu = this.ieLocalColumn.getValueByBaseName("independent");
        return (nvu.value.flag == 15) && (nvu.value.u.shortVal() > 0);
    }

    /**
     * Get the data type of the column. This method always returns the data type of the measurement quantity to which
     * this local column belongs, independent of
     * <ul>
     * <li>the value matrix mode of the value matrices this local column belongs to</li>
     * <li>the sequence representation of this local column</li>
     * </ul>
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
     * Get the sequence representation of the column. When the value matrix mode of the value matrix to which the column
     * belongs is 'CALCULATED', the sequence representation is always explicit.
     * 
     * @see org.asam.ods.ColumnOperations#getSequenceRepresentation()
     */
    public int getSequenceRepresentation() throws AoException {
        if (this.mode == ValueMatrixMode.CALCULATED) {
            return 0; // explicit
        }

        NameValueUnit nvu = this.ieLocalColumn.getValueByBaseName("sequence_representation");
        if (nvu.value.flag != 15) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Value of attribute 'sequence_representation' not set:" + ieLocalColumn.getAsamPath());
        }
        return nvu.value.u.enumVal();
    }

    /**
     * Get the generation parameters of the column. If no generation parameters exist, an empty sequence is returned,
     * whose type is double and whose length is 0. If the value matrix mode of the value matrix to which the column
     * belongs is 'CALCULATED', an empty sequence is returned, whose type is double and whose length is 0. The data type
     * of the generation parameters for implicit columns is the data type given at the corresponding measurement
     * quantity. The data type of the generation parameters for raw data is always T_DOUBLE
     * 
     * @see org.asam.ods.ColumnOperations#getGenerationParameters()
     */
    public TS_Union getGenerationParameters() throws AoException {
        if (this.mode == ValueMatrixMode.CALCULATED) {
            TS_Union u = new TS_Union();
            u.doubleSeq(new double[0]);
            return u;
        }

        NameValueUnit nvu = this.ieLocalColumn.getValueByBaseName("generation_parameters");
        if (nvu.value.flag != 15) {
            TS_Union u = new TS_Union();
            u.doubleSeq(new double[0]);
            return u;
        }
        return nvu.value.u;
    }

    /**
     * Get the data type of the raw values (which is the value of the attribute derived from the base attribute
     * 'raw_datatype'). If the local column does not contain raw values, the data type of the corresponding measurement
     * quantity is returned. Also, if the value matrix mode of the value matrix to which the column belongs is
     * 'CALCULATED', the data type of the measurement quantity is returned.
     * 
     * @see org.asam.ods.ColumnOperations#getRawDataType()
     */
    public DataType getRawDataType() throws AoException {
        // raw_linear (=4), raw_polynomial (=5), raw_linear_calibrated (=10)
        int seqReq = getSequenceRepresentation();
        if ((mode == ValueMatrixMode.STORAGE) && (seqReq == 4 || seqReq == 5 || seqReq == 10)) {
            NameValueUnit nvu = this.ieLocalColumn.getValueByBaseName("raw_datatype'");
            if (nvu.value.flag == 15) {
                return ODSHelper.enum2dataType(nvu.value.u.enumVal());
            }
        }
        return getDataType();
    }

    /**
     * Get the unit of the column. The method returns the unit of the values according the current setting. Initially
     * within a session this is the unit as given in the storage. After setUnit() is called for a particular column, the
     * unit as specified by setUnit() will be returned for this column. Thus the unit returned will be retrieved by the
     * ODS server according to following strategy:
     * <ul>
     * <li>1) if a unit has already been set for the particular column by setUnit(..) before, that unit is returned,
     * else</li>
     * <li>2) if the related measurement quantity has a unit (i.e. it has a relation derived from the base relation
     * 'unit' referencing an instance of base type AoUnit), that unit's name is returned, else</li>
     * <li>3) if the related measurement quantity has a relation to a quantity (i.e. its relation derived from the base
     * relation 'quantity' is referencing an instance of base type AoQuantity), and this quantity has a default unit
     * (i.e. it has a relation derived from the base relation 'default_unit' referencing an instance of base type
     * AoUnit), that unit's name is returned, else</li>
     * <li>4) an empty string is returned.</li>
     * </ul>
     * 
     * @see org.asam.ods.ColumnOperations#getUnit()
     */
    public String getUnit() throws AoException {
        InstanceElement ieMeq = getSourceMQ();

        // 1) if a unit has already been set for the particular column by setUnit(..) before, that unit is returned,
        if (this.unit != null) {
            return this.unit.getName();
        }

        // 2) if the related measurement quantity has a unit (i.e. it has a relation derived from the
        // base relation 'unit' referencing an instance of base type AoUnit), that unit's name is
        // returned
        long aidMeq = ODSHelper.asJLong(ieMeq.getApplicationElement().getId());
        ApplicationRelation applRelMeqUnit = this.atfxCache.getApplicationRelationByBaseName(aidMeq, "unit");
        if (applRelMeqUnit != null) {
            InstanceElementIterator iter = ieMeq.getRelatedInstances(applRelMeqUnit, "*");
            InstanceElement[] instances = iter.nextN(iter.getCount());
            iter.destroy();
            if (instances.length == 1) {
                return instances[0].getName();
            }
        }

        // 3) if the related measurement quantity has a relation to a quantity (i.e. its relation
        // derived from the base relation 'quantity' is referencing an instance of base type
        // AoQuantity), and this quantity has a default unit (i.e. it has a relation derived from the
        // base relation 'default_unit' referencing an instance of base type AoUnit), that unit's
        // name is returned
        ApplicationRelation applRelMeqQuantity = this.atfxCache.getApplicationRelationByBaseName(aidMeq, "quantity");
        if (applRelMeqQuantity != null) {
            InstanceElementIterator iter = ieMeq.getRelatedInstances(applRelMeqQuantity, "*");
            InstanceElement[] instances = iter.nextN(iter.getCount());
            iter.destroy();
            if (instances.length == 1) {
                InstanceElement ieQuantity = instances[0];
                long aidQuantity = ODSHelper.asJLong(ieQuantity.getApplicationElement().getId());
                ApplicationRelation applRelQuantityUnit = this.atfxCache.getApplicationRelationByBaseName(aidQuantity,
                                                                                                          "default_unit");
                if (applRelQuantityUnit != null) {
                    iter = ieQuantity.getRelatedInstances(applRelQuantityUnit, "*");
                    instances = iter.nextN(iter.getCount());
                    iter.destroy();
                    if (instances.length == 1) {
                        return instances[0].getName();
                    }
                }
            }
        }

        // 4) an empty string is returned.
        return "";
    }

    /**
     * Destroy the object on the server. This method is used to tell the server that this object is not used anymore by
     * the client. Access to this object after the destroy method will lead to an exception.
     * 
     * @see org.asam.ods.ColumnOperations#destroy()
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
     * @see org.asam.ods.ColumnOperations#setSequenceRepresentation(int)
     */
    public void setSequenceRepresentation(int sequenceRepresentation) throws AoException {
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

}
