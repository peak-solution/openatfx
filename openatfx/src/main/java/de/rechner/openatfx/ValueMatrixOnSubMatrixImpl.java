package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.Column;
import org.asam.ods.ColumnHelper;
import org.asam.ods.DataType;
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
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.asam.ods.ValueMatrixMode;
import org.asam.ods.ValueMatrixPOA;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;


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
     * Get the current mode of the value matrix.
     * 
     * @see org.asam.ods.ValueMatrixOperations#getMode()
     */
    public ValueMatrixMode getMode() throws AoException {
        return this.mode;
    }

    /**
     * Get the column count of the value matrix.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @return The number of columns of the value matrix.
     * @see org.asam.ods.ValueMatrixOperations#getColumnCount()
     */
    public int getColumnCount() throws AoException {
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
        int cnt = iter.getCount();
        iter.destroy();
        return cnt;
    }

    /**
     * Get the row count of the value matrix.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @return The number of rows of the value matrix.
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
     * Get the names of the columns of the value matrix no matter whether the column is dependent or independent. The
     * pattern is case sensitive and may contain wildcard characters.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_BAD_PARAMETER<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @param colPattern The name or the search pattern for the column names.
     * @return The column names of the value matrix, no matter whether the column is dependent, independent or scaled by
     *         another one.
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
     * Get the names of the independent columns of the value matrix. The independent columns are the columns used to
     * build the value matrix.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_BAD_PARAMETER<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @param colPattern The name or the search pattern for the independent column name.
     * @return The names of the independent columns of the value matrix.
     * @see org.asam.ods.ValueMatrixOperations#listIndependentColumns(java.lang.String)
     */
    public String[] listIndependentColumns(String colPattern) throws AoException {
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, colPattern);
        InstanceElement[] ies = iter.nextN(iter.getCount());
        iter.destroy();

        List<String> list = new ArrayList<String>();
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
     * Get the columns of the value matrix no matter whether the column is dependent or independent. The pattern is case
     * sensitive and may contain wildcard characters.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_BAD_PARAMETER<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @param colPattern The name or the search pattern for the column names.
     * @return The columns of the value matrix, no matter whether the column is dependent, independent or scaling
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
                ColumnImpl columnImpl = new ColumnImpl(this.modelPOA, this.sourceSubMatrix.atfxCache, ies[i], this.mode);
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
     * Get the independent columns of the value matrix.<br>
     * The independent columns are the columns used to build the value matrix.
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_BAD_PARAMETER<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     *             AO_SESSION_NOT_ACTIVE
     * @param colPattern The name or the search pattern for the independent column name.
     * @return The independent column of the value matrix.
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
                    ColumnImpl columnImpl = new ColumnImpl(this.modelPOA, this.sourceSubMatrix.atfxCache, ies[i],
                                                           this.mode);
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
     * <p>
     * Get the values or a part of values of the column from the value matrix. The parameter column specifies from which
     * column the values will be returned. The startPoint and count specify the part of the vector. A startPoint = 0 and
     * count = rowCount will return the entire vector. When startPoint >= rowCount an exception is thrown. If startPoint
     * + count > rowCount only the remaining values of the vector are returned and no exception is thrown. Use the
     * getName and getUnit method of the interface column for the name and the unit of the column. The name and the
     * value are not stored at each element of the vector. The return type TS_ValueSeq is not a sequence of TS_Value but
     * a special structure.
     * </p>
     * <p>
     * The server behavior depends on the mode of the value matrix. Value matrix mode 'CALCULATED':<br>
     * In case 'sequence_representation' of the corresponding local column is one of the entries 'raw_linear',
     * raw_polynomial', 'raw_linear_external', 'raw_polynomial_external', 'raw_linear_calibrated', or
     * 'raw_linear_calibrated_external', the server will first calculate the physical values from raw values and
     * generation parameters, before it returns them to the requesting client.
     * </p>
     * <p>
     * Value matrix mode 'STORAGE':<br>
     * In case 'sequence_representation' of the corresponding local column is one of the entries 'raw_linear',
     * raw_polynomial', 'raw_linear_external', 'raw_polynomial_external', 'raw_linear_calibrated', or
     * 'raw_linear_calibrated_external', the server will return the raw values of the local column.
     * </p>
     * 
     * @throws AoException with the following possible error codes:<br>
     *             AO_BAD_PARAMETER<br>
     *             AO_CONNECTION_LOST<br>
     *             AO_IMPLEMENTATION_PROBLEM<br>
     *             AO_INVALID_COLUMN<br>
     *             AO_INVALID_COUNT<br>
     *             AO_NOT_IMPLEMENTED<br>
     *             AO_NO_MEMORY<br>
     * @see org.asam.ods.ValueMatrixOperations#getValueVector(org.asam.ods.Column, int, int)
     */
    public TS_ValueSeq getValueVector(Column col, int startPoint, int count) throws AoException {
        int rowCount = getRowCount();
        AtfxCache atfxCache = this.sourceSubMatrix.atfxCache;
        InstanceElement ieLc = getLocalColumnInstanceByName(col.getName());
        long aidLc = ODSHelper.asJLong(ieLc.getApplicationElement().getId());
        long iidLc = ODSHelper.asJLong(ieLc.getId());
        DataType targetDt = col.getDataType();

        // range check
        if (startPoint < 0) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "startPoint must be >0");
        }
        if (startPoint >= rowCount) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "startPoint must be <rowCount, rowCount=" + rowCount);
        }
        if (count < 0) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "count must be >0");
        }
        if ((count == 0) || ((startPoint + count) > rowCount)) {
            count = rowCount - startPoint;
        }

        // create structure
        TS_ValueSeq valueSeq = new TS_ValueSeq();
        valueSeq.u = new TS_UnionSeq();

        // load flags: first check global_flag, then flags
        Integer attrNoGlobalFlag = atfxCache.getAttrNoByBaName(aidLc, "global_flag");
        Integer attrNoFlags = atfxCache.getAttrNoByBaName(aidLc, "flags");
        if (attrNoGlobalFlag != null) {
            TS_Value globalFlag = atfxCache.getInstanceValue(aidLc, attrNoGlobalFlag, iidLc);
            if (globalFlag.flag == 15) {
                valueSeq.flag = new short[count];
                Arrays.fill(valueSeq.flag, globalFlag.u.shortVal());
            }
        }
        if (valueSeq.flag == null && attrNoFlags != null) {
            TS_Value flags = atfxCache.getInstanceValue(aidLc, attrNoFlags, iidLc);
            if (flags.flag == 15) {
                valueSeq.flag = new short[count];
                System.arraycopy(flags.u.shortSeq(), startPoint, valueSeq.flag, 0, count);
            }
        }
        if (valueSeq.flag == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Either 'flags' or 'global_flag must be set!");
        }

        // load values
        NameValueUnit nvuSeqReq = ieLc.getValueByBaseName("sequence_representation");
        if (nvuSeqReq.value.flag != 15) {
            throw new AoException(ErrorCode.AO_INVALID_COLUMN, SeverityFlag.ERROR, 0,
                                  "sequence_representation not set!");
        }
        int seqReq = nvuSeqReq.value.u.enumVal();

        // explicit (=0), external_component (=7)
        if (seqReq == 0 || seqReq == 7) {
            NameValueUnit values = ieLc.getValueByBaseName("values");
            handleValuesExplicit(values, valueSeq, targetDt, startPoint, count);
        }

        // implicit_constant (=1)
        else if (seqReq == 1) {
            NameValueUnit genParams = ieLc.getValueByBaseName("generation_parameters");
            handleValuesImplicitConstant(genParams.value.u.doubleSeq(), valueSeq, targetDt, count);
        }

        // implicit_linear (=2)
        else if (seqReq == 2) {
            NameValueUnit genParams = ieLc.getValueByBaseName("generation_parameters");
            handleValuesImplicitLinear(genParams.value.u.doubleSeq(), valueSeq, targetDt, startPoint, count);
        }

        // implicit_saw (=3)
        else if (seqReq == 3) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "sequence_representation=implicit_saw is not yet implemented");
        }

        // raw_linear (=4), raw_linear_external (=8)
        else if (seqReq == 4 || seqReq == 8) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "sequence_representation=raw_linear or raw_linear_external is not yet implemented");
        }

        // raw_polynomial (=5), raw_polynomial_external (=9)
        else if (seqReq == 5 || seqReq == 9) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "sequence_representation=raw_polynomial or raw_polynomial_external is not yet implemented");
        }

        // formula (=6)
        else if (seqReq == 6) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "sequence_representation=formula is not yet implemented");
        }

        // raw_linear_calibrated (=10), raw_linear_calibrated_external (=11)
        else if (seqReq == 10 || seqReq == 11) {
            if (this.mode == ValueMatrixMode.STORAGE) {
                NameValueUnit values = ieLc.getValueByBaseName("values");
                handleValuesRawLinearCalibratedStorage(values, valueSeq, startPoint, rowCount);
            } else if (this.mode == ValueMatrixMode.CALCULATED) {
                NameValueUnit values = ieLc.getValueByBaseName("values");
                NameValueUnit genParams = ieLc.getValueByBaseName("generation_parameters");
                handleValuesRawLinearCalibratedCalculated(values, genParams.value.u.doubleSeq(), valueSeq, targetDt,
                                                          startPoint, rowCount);
            } else {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Unsupported ValueMatrixMode: " + this.mode);
            }
        }

        return valueSeq;
    }

    private void handleValuesExplicit(NameValueUnit values, TS_ValueSeq valueSeq, DataType targetDt, int startPoint,
            int count) throws AoException {
        DataType rawDt = values.value.u.discriminator();
        if (rawDt == DataType.DS_STRING && targetDt == DataType.DT_STRING) {
            valueSeq.u.stringVal(values.value.u.stringSeq());
        } else if (rawDt == DataType.DS_STRING && targetDt == DataType.DT_DATE) {
            valueSeq.u.dateVal(values.value.u.stringSeq());
        } else if (rawDt == DataType.DS_DATE && targetDt == DataType.DT_DATE) {
            valueSeq.u.dateVal(values.value.u.dateSeq());
        } else if (rawDt == DataType.DS_BOOLEAN && targetDt == DataType.DT_BOOLEAN) {
            valueSeq.u.booleanVal(values.value.u.booleanSeq());
        } else if (rawDt == DataType.DS_COMPLEX && targetDt == DataType.DT_COMPLEX) {
            valueSeq.u.complexVal(values.value.u.complexSeq());
        } else if (rawDt == DataType.DS_DCOMPLEX && targetDt == DataType.DT_DCOMPLEX) {
            valueSeq.u.dcomplexVal(values.value.u.dcomplexSeq());
        } else {
            List<Number> list = getNumbericValues(values.value.u);
            // DS_SHORT
            if (targetDt == DataType.DT_SHORT) {
                valueSeq.u.shortVal(new short[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.shortVal()[i] = list.get(startPoint + i).shortValue();
                }
            }
            // DS_FLOAT
            else if (targetDt == DataType.DT_FLOAT) {
                valueSeq.u.floatVal(new float[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.floatVal()[i] = list.get(startPoint + i).floatValue();
                }
            }
            // DS_DOUBLE
            else if (targetDt == DataType.DT_DOUBLE) {
                valueSeq.u.doubleVal(new double[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.doubleVal()[i] = list.get(startPoint + i).doubleValue();
                }
            }
            // DS_LONG
            else if (targetDt == DataType.DT_LONG) {
                valueSeq.u.longVal(new int[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.longVal()[i] = list.get(startPoint + i).intValue();
                }
            }
            // DS_LONGLONG
            else if (targetDt == DataType.DT_LONGLONG) {
                valueSeq.u.longlongVal(new T_LONGLONG[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.longlongVal()[i] = ODSHelper.asODSLongLong(list.get(startPoint + i).longValue());
                }
            }
            // DS_BYTE
            else if (targetDt == DataType.DS_BYTE) {
                valueSeq.u.byteVal(new byte[count]);
                for (int i = 0; i < count; i++) {
                    valueSeq.u.byteVal()[i] = list.get(startPoint + i).byteValue();
                }
            }
            // unsupported
            else {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                      "Unsupported datatype for sequence_representation=explicit or external_component: "
                                              + ODSHelper.dataType2String(rawDt));
            }
        }
    }

    private void handleValuesImplicitConstant(double[] genParams, TS_ValueSeq valueSeq, DataType targetDt, int count)
            throws AoException {
        if (genParams.length != 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "Generation parameters for sequence_representation=implicit_constant must have length=1");
        }
        Number genParam = genParams[0];

        // DS_SHORT
        if (targetDt == DataType.DT_SHORT) {
            valueSeq.u.shortVal(new short[count]);
            Arrays.fill(valueSeq.u.shortVal(), genParam.shortValue());
        }
        // DS_FLOAT
        else if (targetDt == DataType.DT_FLOAT) {
            valueSeq.u.floatVal(new float[count]);
            Arrays.fill(valueSeq.u.floatVal(), genParam.floatValue());
        }
        // DS_DOUBLE
        else if (targetDt == DataType.DT_DOUBLE) {
            valueSeq.u.doubleVal(new double[count]);
            Arrays.fill(valueSeq.u.doubleVal(), genParam.doubleValue());
        }
        // DS_LONG
        else if (targetDt == DataType.DT_LONG) {
            valueSeq.u.longVal(new int[count]);
            Arrays.fill(valueSeq.u.longVal(), genParam.intValue());
        }
        // DS_LONGLONG
        else if (targetDt == DataType.DT_LONGLONG) {
            valueSeq.u.longlongVal(new T_LONGLONG[count]);
            Arrays.fill(valueSeq.u.longlongVal(), ODSHelper.asODSLongLong(genParam.longValue()));
        }
        // DS_BYTE
        else if (targetDt == DataType.DS_BYTE) {
            valueSeq.u.byteVal(new byte[count]);
            Arrays.fill(valueSeq.u.byteVal(), genParam.byteValue());
        }
        // unsupported
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Unsupported datatype for sequence_representation=implicit_constant: "
                                          + ODSHelper.dataType2String(targetDt));
        }
    }

    private void handleValuesImplicitLinear(double[] genParams, TS_ValueSeq valueSeq, DataType targetDt,
            int startPoint, int count) throws AoException {
        if (genParams.length != 2) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "Generation parameters for sequence_representation=implicit_linear must have length=2");
        }
        // xn=p1+(n-1)*p2 (start value, increment)
        double offset = genParams[0];
        double factor = genParams[1];

        // DS_SHORT
        if (targetDt == DataType.DT_SHORT) {
            valueSeq.u.shortVal(new short[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.shortVal()[i] = (short) (offset + (startPoint + i) * factor);
            }
        }
        // DS_FLOAT
        else if (targetDt == DataType.DT_FLOAT) {
            valueSeq.u.floatVal(new float[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.floatVal()[i] = (float) (offset + (startPoint + i) * factor);
            }
        }
        // DS_DOUBLE
        else if (targetDt == DataType.DT_DOUBLE) {
            valueSeq.u.doubleVal(new double[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.doubleVal()[i] = (offset + (startPoint + i) * factor);
            }
        }
        // DS_LONG
        else if (targetDt == DataType.DT_LONG) {
            valueSeq.u.longVal(new int[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.longVal()[i] = (int) (offset + (startPoint + i) * factor);
            }
        }
        // DS_LONGLONG
        else if (targetDt == DataType.DT_LONGLONG) {
            valueSeq.u.longlongVal(new T_LONGLONG[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.longlongVal()[i] = ODSHelper.asODSLongLong((long) (offset + (startPoint + i) * factor));
            }
        }
        // DS_BYTE
        else if (targetDt == DataType.DS_BYTE) {
            valueSeq.u.byteVal(new byte[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.byteVal()[i] = (byte) (offset + (startPoint + i) * factor);
            }
        }
        // unsupported
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Unsupported datatype for sequence_representation=implicit_linear: "
                                          + ODSHelper.dataType2String(targetDt));
        }
    }

    private void handleValuesRawLinearCalibratedStorage(NameValueUnit values, TS_ValueSeq valueSeq, int startPoint,
            int count) throws AoException {
        DataType rawDt = values.value.u.discriminator();
        // DS_SHORT
        if (rawDt == DataType.DT_SHORT) {
            valueSeq.u.shortVal(new short[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.shortVal()[i] = values.value.u.shortSeq()[startPoint + i];
            }
        }
        // DS_FLOAT
        else if (rawDt == DataType.DT_FLOAT) {
            valueSeq.u.floatVal(new float[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.floatVal()[i] = values.value.u.floatSeq()[startPoint + i];
            }
        }
        // DS_DOUBLE
        else if (rawDt == DataType.DT_DOUBLE) {
            valueSeq.u.doubleVal(new double[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.doubleVal()[i] = values.value.u.doubleSeq()[startPoint + i];
            }
        }
        // DS_LONG
        else if (rawDt == DataType.DT_LONG) {
            valueSeq.u.longVal(new int[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.longVal()[i] = values.value.u.longSeq()[startPoint + i];
            }
        }
        // DS_LONGLONG
        else if (rawDt == DataType.DT_LONGLONG) {
            valueSeq.u.longlongVal(new T_LONGLONG[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.longlongVal()[i] = values.value.u.longlongSeq()[startPoint + i];
            }
        }
        // DS_BYTE
        else if (rawDt == DataType.DS_BYTE) {
            valueSeq.u.byteVal(new byte[count]);
            for (int i = 0; i < count; i++) {
                valueSeq.u.byteVal()[i] = values.value.u.byteSeq()[startPoint + i];
            }
        }
        // unsupported
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Unsupported datatype for sequence_representation=explicit or external_component: "
                                          + ODSHelper.dataType2String(values.value.u.discriminator()));
        }
    }

    private void handleValuesRawLinearCalibratedCalculated(NameValueUnit values, double[] genParams,
            TS_ValueSeq valueSeq, DataType targetDt, int startPoint, int count) throws AoException {
        if (genParams.length != 3) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "Generation parameters for sequence_representation=raw_linear_calibrated must have length=3");
        }
        // xn = (p1 + p2*rn)*p3 (offset, factor, calibration)
        double offset = genParams[0];
        double factor = genParams[1];
        double calibration = genParams[2];

        List<Number> list = getNumbericValues(values.value.u);
        // DS_SHORT
        if (targetDt == DataType.DT_SHORT) {
            valueSeq.u.shortVal(new short[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.shortVal()[i] = (short) ((offset + factor * rawValue) * calibration);
            }
        }
        // DS_FLOAT
        else if (targetDt == DataType.DT_FLOAT) {
            valueSeq.u.floatVal(new float[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.floatVal()[i] = (float) ((offset + factor * rawValue) * calibration);
            }
        }
        // DS_DOUBLE
        else if (targetDt == DataType.DT_DOUBLE) {
            valueSeq.u.doubleVal(new double[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.doubleVal()[i] = (offset + factor * rawValue) * calibration;
            }
        }
        // DS_LONG
        else if (targetDt == DataType.DT_LONG) {
            valueSeq.u.longVal(new int[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.longVal()[i] = (int) ((offset + factor * rawValue) * calibration);
            }
        }
        // DS_LONGLONG
        else if (targetDt == DataType.DT_LONGLONG) {
            valueSeq.u.longlongVal(new T_LONGLONG[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.longlongVal()[i] = ODSHelper.asODSLongLong((long) ((offset + factor * rawValue) * calibration));
            }
        }
        // DS_BYTE
        else if (targetDt == DataType.DS_BYTE) {
            valueSeq.u.byteVal(new byte[count]);
            for (int i = 0; i < count; i++) {
                double rawValue = list.get(startPoint + i).doubleValue();
                valueSeq.u.byteVal()[i] = (byte) ((offset + factor * rawValue) * calibration);
            }
        }
        // unsupported
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Unsupported datatype for sequence_representation=explicit or external_component: "
                                          + ODSHelper.dataType2String(values.value.u.discriminator()));
        }
    }

    private List<Number> getNumbericValues(TS_Union u) throws AoException {
        DataType dt = u.discriminator();
        List<Number> list = new ArrayList<Number>();
        // DS_SHORT
        if (dt == DataType.DS_SHORT) {
            for (short v : u.shortSeq()) {
                list.add(v);
            }
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            for (float v : u.floatSeq()) {
                list.add(v);
            }
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            for (byte v : u.byteSeq()) {
                list.add(v);
            }
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            for (int v : u.longSeq()) {
                list.add(v);
            }
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            for (double v : u.doubleSeq()) {
                list.add(v);
            }
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            for (T_LONGLONG v : u.longlongSeq()) {
                list.add(ODSHelper.asJLong(v));
            }
        }
        // not allowed
        else {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "not allowed numeric datatype: "
                    + ODSHelper.dataType2String(dt));
        }
        return list;
    }

    private InstanceElement getLocalColumnInstanceByName(String name) throws AoException {
        InstanceElementIterator iter = sourceSubMatrix.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
        InstanceElement[] ies = iter.nextN(iter.getCount());
        iter.destroy();
        for (InstanceElement ie : ies) {
            if (ie.getName().equals(name)) {
                return ie;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "LocalColumn instance '" + name
                + "' not found!");
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
