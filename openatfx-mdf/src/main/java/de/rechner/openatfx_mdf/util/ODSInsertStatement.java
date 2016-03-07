package de.rechner.openatfx_mdf.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AIDName;
import org.asam.ods.AIDNameValueSeqUnitId;
import org.asam.ods.AoException;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplElemAccess;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ElemId;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;


/**
 * Helper class to construct ODS batch insert statements.
 * 
 * @author Christian Rechner
 */
public class ODSInsertStatement {

    private static final Log LOG = LogFactory.getLog(ODSInsertStatement.class);

    private final ODSModelCache cache;

    private final String aeName;

    // overall attrs
    private final Set<String> attrs;

    // the data
    private final List<Map<String, TS_Value>> rows;

    private int pos = -1;

    /**
     * Creates a new batch insert statement. The current pointer of the statement stands at the first statement, so no
     * initial call to 'next' is necessary.
     * 
     * @param cache the ODS cache
     * @param aeName the name of the application element to insert an instance for
     */
    public ODSInsertStatement(ODSModelCache cache, String aeName) {
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        if (aeName == null || aeName.length() < 1) {
            throw new IllegalArgumentException("aeName must not be null or empty");
        }
        this.cache = cache;
        this.aeName = aeName;
        this.attrs = new LinkedHashSet<String>();
        this.rows = new ArrayList<Map<String, TS_Value>>();
    }

    /**
     * Adds a new statement to the batch.
     * 
     * @return The current position.
     */
    public int next() {
        Map<String, TS_Value> map = new LinkedHashMap<String, TS_Value>();
        rows.add(map);
        pos++;
        return pos;
    }

    /**
     * Returns the number of the statements.
     * 
     * @return number of statements
     */
    public int size() {
        return rows.size();
    }

    /**
     * Sets a value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value as <code>org.asam.ods.TS_Value</code>.
     */
    public void setTS_Value(String aaName, TS_Value value) {
        if (pos < 0) {
            next();
        }
        this.rows.get(pos).put(aaName, value);
        this.attrs.add(aaName);
    }

    /**
     * Sets a value to the current batch position.
     * 
     * @param nv The value including the attribute name as <code>org.asam.ods.NameValue</code>.
     */
    public void setNameValue(NameValue nv) {
        setTS_Value(nv.valName, nv.value);
    }

    /**
     * Sets a value to the current batch position.
     * 
     * @param nvu The value including the attribute name as <code>org.asam.ods.NameValueUnit</code>.
     */
    public void setNameValueUnit(NameValueUnit nvu) {
        setTS_Value(nvu.valName, nvu.value);
    }

    /**
     * Sets a boolean value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setBooleanVal(String aaName, boolean value) {
        setNameValue(ODSHelper.createBooleanNV(aaName, value));
    }

    /**
     * Sets a boolean sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setBooleanSeq(String aaName, boolean[] value) {
        setNameValue(ODSHelper.createBooleanSeqNV(aaName, value));
    }

    /**
     * Sets a byte value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setByteVal(String aaName, byte value) {
        setNameValue(ODSHelper.createByteNV(aaName, value));
    }

    /**
     * Sets a byte sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setByteSeq(String aaName, byte[] value) {
        setNameValue(ODSHelper.createByteSeqNV(aaName, value));
    }

    /**
     * Sets a byteStr value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setBytestrVal(String aaName, byte[] value) {
        setNameValue(ODSHelper.createBytestrNV(aaName, value));
    }

    /**
     * Sets a byteStr sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setBytestrSeq(String aaName, byte[][] value) {
        setNameValue(ODSHelper.createBytestrSeqNV(aaName, value));
    }

    /**
     * Sets a date value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDateVal(String aaName, String value) {
        setNameValue(ODSHelper.createDateNV(aaName, value));
    }

    /**
     * Sets a date sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDateSeq(String aaName, String[] value) {
        setNameValue(ODSHelper.createDateSeqNV(aaName, value));
    }

    /**
     * Sets a T_DCOMPLEX value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDComplexVal(String aaName, T_DCOMPLEX value) {
        setNameValue(ODSHelper.createDComplexNV(aaName, value));
    }

    /**
     * Sets a T_DCOMPLEX sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDComplexSeq(String aaName, T_DCOMPLEX[] value) {
        setNameValue(ODSHelper.createDComplexSeqNV(aaName, value));
    }

    /**
     * Sets a double value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDoubleVal(String aaName, double value) {
        setNameValue(ODSHelper.createDoubleNV(aaName, value));
    }

    /**
     * Sets a double sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setDoubleSeq(String aaName, double[] value) {
        setNameValue(ODSHelper.createDoubleSeqNV(aaName, value));
    }

    /**
     * Sets a enum value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setEnumVal(String aaName, int value) {
        setNameValue(ODSHelper.createEnumNV(aaName, value));
    }

    /**
     * Sets a enum sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setEnumSeq(String aaName, int[] value) {
        setNameValue(ODSHelper.createEnumSeqNV(aaName, value));
    }

    /**
     * Sets a T_ExternalReference value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setExtRefVal(String aaName, T_ExternalReference value) {
        setNameValue(ODSHelper.createExtRefNV(aaName, value));
    }

    /**
     * Sets a T_ExternalReference sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setExtRefSeq(String aaName, T_ExternalReference[] value) {
        setNameValue(ODSHelper.createExtRefSeqNV(aaName, value));
    }

    /**
     * Sets a float value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setFloatVal(String aaName, float value) {
        setNameValue(ODSHelper.createFloatNV(aaName, value));
    }

    /**
     * Sets a float sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setFloatSeq(String aaName, float[] value) {
        setNameValue(ODSHelper.createFloatSeqNV(aaName, value));
    }

    /**
     * Sets a long value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongVal(String aaName, int value) {
        setNameValue(ODSHelper.createLongNV(aaName, value));
    }

    /**
     * Sets a long sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongSeq(String aaName, int[] value) {
        setNameValue(ODSHelper.createLongSeqNV(aaName, value));
    }

    /**
     * Sets a long long value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongLongVal(String aaName, T_LONGLONG value) {
        setNameValue(ODSHelper.createLongLongNV(aaName, value));
    }

    /**
     * Sets a long long value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongLongVal(String aaName, long value) {
        setNameValue(ODSHelper.createLongLongNV(aaName, value));
    }

    /**
     * Sets a long long sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongLongSeq(String aaName, T_LONGLONG[] value) {
        setNameValue(ODSHelper.createLongLongSeqNV(aaName, value));
    }

    /**
     * Sets a long long sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setLongLongSeq(String aaName, long[] value) {
        setNameValue(ODSHelper.createLongLongSeqNV(aaName, value));
    }

    /**
     * Sets a short value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setShortVal(String aaName, short value) {
        setNameValue(ODSHelper.createShortNV(aaName, value));
    }

    /**
     * Sets a short sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setShortSeq(String aaName, short[] value) {
        setNameValue(ODSHelper.createShortSeqNV(aaName, value));
    }

    /**
     * Sets a string value to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setStringVal(String aaName, String value) {
        setNameValue(ODSHelper.createStringNV(aaName, value));
    }

    /**
     * Sets a string sequence to the current batch position.
     * 
     * @param aaName The application attribute name.
     * @param value The value.
     */
    public void setStringSeq(String aaName, String[] value) {
        setNameValue(ODSHelper.createStringSeqNV(aaName, value));
    }

    /**
     * Determines if all values of an attribute are null.
     * 
     * @param attrName The attribute name.
     * @return true, if all values are null (flag=0), otherwise false
     */
    private boolean isAllValuesNull(String attrName) {
        for (Map<String, TS_Value> value : rows) {
            TS_Value v = value.get(attrName);
            if (v != null && v.flag != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Execute the batch insert and return the database id in the order of the given statements. A transaction has to be
     * active.
     * 
     * @return the ids of the created instances
     * @throws AoException if something went wrong
     */
    public long[] executeBatch() throws AoException {
        // check if rows to insert are given
        if (this.rows.isEmpty() || this.attrs.isEmpty()) {
            return new long[0];
        }

        // copy values to the CORBA structure needed by the ODS interface
        List<AIDNameValueSeqUnitId> list = new ArrayList<AIDNameValueSeqUnitId>();
        ApplElem applElem = cache.getApplElem(aeName);

        for (String attr : this.attrs) {
            // check if all values of columns are null
            if (isAllValuesNull(attr)) {
                continue;
            }

            // do not insert if attribute is "id"
            if (this.cache.applAttrExists(this.aeName, attr)
                    && this.cache.getApplAttr(this.aeName, attr).baName.equals("id")) {
                continue;
            }

            DataType dt = this.rows.iterator().next().get(attr).u.discriminator();

            AIDNameValueSeqUnitId anvsu = new AIDNameValueSeqUnitId();
            anvsu.unitId = ODSHelper.asODSLongLong(0);
            anvsu.attr = new AIDName();
            anvsu.attr.aaName = attr;
            anvsu.attr.aid = applElem.aid;
            anvsu.values = new TS_ValueSeq();
            anvsu.values.flag = new short[rows.size()];
            anvsu.values.u = new TS_UnionSeq();

            // DT_BLOB
            if (dt == DataType.DT_BLOB) {
                Blob[] ar = new Blob[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    if (v == null) {
                        ar[i] = this.cache.getAoSession().createBlob();
                        ar[i].setHeader("");
                        ar[i].set(new byte[0]);
                    } else {
                        ar[i] = v.u.blobVal();
                    }
                    anvsu.values.u.blobVal(ar);
                }
            }
            // DT_BOOLEAN
            else if (dt == DataType.DT_BOOLEAN) {
                boolean[] ar = new boolean[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? false : v.u.booleanVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.booleanVal(ar);
                }
            }
            // DS_BOOLEAN
            else if (dt == DataType.DS_BOOLEAN) {
                boolean[][] ar = new boolean[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new boolean[0] : v.u.booleanSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.booleanSeq(ar);
                }
            }
            // DT_BYTE
            else if (dt == DataType.DT_BYTE) {
                byte[] ar = new byte[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? 0 : v.u.byteVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.byteVal(ar);
                }
            }
            // DS_BYTE
            else if (dt == DataType.DS_BYTE) {
                byte[][] ar = new byte[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new byte[0] : v.u.byteSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.byteSeq(ar);
                }
            }
            // DT_BYTESTR
            else if (dt == DataType.DT_BYTESTR) {
                byte[][] ar = new byte[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new byte[0] : v.u.bytestrVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.bytestrVal(ar);
                }
            }
            // DS_BYTESTR
            else if (dt == DataType.DS_BYTESTR) {
                byte[][][] ar = new byte[rows.size()][][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new byte[0][0] : v.u.bytestrSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.bytestrSeq(ar);
                }
            }
            // DT_COMPLEX
            else if (dt == DataType.DT_COMPLEX) {
                T_COMPLEX[] ar = new T_COMPLEX[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_COMPLEX(0, 0) : v.u.complexVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.complexVal(ar);
                }
            }
            // DS_COMPLEX
            else if (dt == DataType.DS_COMPLEX) {
                T_COMPLEX[][] ar = new T_COMPLEX[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_COMPLEX[0] : v.u.complexSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.complexSeq(ar);
                }
            }
            // DT_DATE
            else if (dt == DataType.DT_DATE) {
                String[] ar = new String[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? "" : v.u.dateVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.dateVal(ar);
                }
            }
            // DS_DATE
            else if (dt == DataType.DS_DATE) {
                String[][] ar = new String[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new String[0] : v.u.dateSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.dateSeq(ar);
                }
            }
            // DT_DCOMPLEX
            else if (dt == DataType.DT_DCOMPLEX) {
                T_DCOMPLEX[] ar = new T_DCOMPLEX[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_DCOMPLEX(0, 0) : v.u.dcomplexVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.dcomplexVal(ar);
                }
            }
            // DS_DCOMPLEX
            else if (dt == DataType.DS_DCOMPLEX) {
                T_DCOMPLEX[][] ar = new T_DCOMPLEX[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_DCOMPLEX[0] : v.u.dcomplexSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.dcomplexSeq(ar);
                }
            }
            // DT_DOUBLE
            else if (dt == DataType.DT_DOUBLE) {
                double[] ar = new double[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? 0 : v.u.doubleVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.doubleVal(ar);
                }
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                double[][] ar = new double[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new double[0] : v.u.doubleSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.doubleSeq(ar);
                }
            }
            // DT_ENUM
            else if (dt == DataType.DT_ENUM) {
                int[] ar = new int[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? 0 : v.u.enumVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.enumVal(ar);
                }
            }
            // DS_ENUM
            else if (dt == DataType.DS_ENUM) {
                int[][] ar = new int[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new int[0] : v.u.enumSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.enumSeq(ar);
                }
            }
            // DT_EXTERNALREFERENCE
            else if (dt == DataType.DT_EXTERNALREFERENCE) {
                T_ExternalReference[] ar = new T_ExternalReference[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_ExternalReference("", "", "") : v.u.extRefVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.extRefVal(ar);
                }
            }
            // DS_EXTERNALREFERENCE
            else if (dt == DataType.DS_EXTERNALREFERENCE) {
                T_ExternalReference[][] ar = new T_ExternalReference[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_ExternalReference[0] : v.u.extRefSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.extRefSeq(ar);
                }
            }
            // DT_FLOAT
            else if (dt == DataType.DT_FLOAT) {
                float[] ar = new float[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? 0 : v.u.floatVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.floatVal(ar);
                }
            }
            // DS_FLOAT
            else if (dt == DataType.DS_FLOAT) {
                float[][] ar = new float[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new float[0] : v.u.floatSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.floatSeq(ar);
                }
            }
            // DT_LONG
            else if (dt == DataType.DT_LONG) {
                int[] ar = new int[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? 0 : v.u.longVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.longVal(ar);
                }
            }
            // DS_LONG
            else if (dt == DataType.DS_LONG) {
                int[][] ar = new int[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new int[0] : v.u.longSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.longSeq(ar);
                }
            }
            // DT_LONGLONG
            else if (dt == DataType.DT_LONGLONG) {
                T_LONGLONG[] ar = new T_LONGLONG[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_LONGLONG(0, 0) : v.u.longlongVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.longlongVal(ar);
                }
            }
            // DS_LONGLONG
            else if (dt == DataType.DS_LONGLONG) {
                T_LONGLONG[][] ar = new T_LONGLONG[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new T_LONGLONG[0] : v.u.longlongSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.longlongSeq(ar);
                }
            }
            // DT_SHORT
            else if (dt == DataType.DT_SHORT) {
                short[] ar = new short[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? (short) 0 : v.u.shortVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.shortVal(ar);
                }
            }
            // DS_SHORT
            else if (dt == DataType.DS_SHORT) {
                short[][] ar = new short[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new short[0] : v.u.shortSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.shortSeq(ar);
                }
            }
            // DT_STRING
            else if (dt == DataType.DT_STRING) {
                String[] ar = new String[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? "" : v.u.stringVal());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.stringVal(ar);
                }
            }
            // DS_STRING
            else if (dt == DataType.DS_STRING) {
                String[][] ar = new String[rows.size()][];
                for (int i = 0; i < rows.size(); i++) {
                    TS_Value v = rows.get(i).get(attr);
                    ar[i] = (v == null ? new String[0] : v.u.stringSeq());
                    anvsu.values.flag[i] = (v == null ? 0 : v.flag);
                    anvsu.values.u.stringSeq(ar);
                }
            }
            // unsupported DataType
            else {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "unsupported datatype: " + dt);
            }

            list.add(anvsu);
        }

        // execute
        ApplElemAccess applElemAccess = cache.getApplElemAccess();
        AIDNameValueSeqUnitId[] ar = list.toArray(new AIDNameValueSeqUnitId[list.size()]);

        long start = System.currentTimeMillis();
        ElemId[] elemIds = applElemAccess.insertInstances(ar);
        long[] ids = new long[elemIds.length];
        for (int i = 0; i < elemIds.length; i++) {
            ids[i] = ODSHelper.asJLong(elemIds[i].iid);
        }
        long duration = System.currentTimeMillis() - start;

        LOG.debug("InsertStatement executed [aeName=" + aeName + ",number=" + size() + ",time=" + duration + "ms]");
        return ids;
    }

    /**
     * Execute the insert and return the created id. This call is only possible if one row is provided to insert.
     * 
     * @return the id of the created instance
     * @throws AoException if something went wrong
     */
    public long execute() throws AoException {
        if (size() < 1) {
            throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0, "no attribute values provided");
        }
        if (size() > 1) {
            throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                  "Multiple insert rows existing! Use executeBatch instead!");
        }
        return executeBatch()[0];
    }

}
