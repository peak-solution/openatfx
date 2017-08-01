package de.rechner.openatfx.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asam.ods.AIDName;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.Blob;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueUnit;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_UnionSeq;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;


/**
 * Helper class with ODS specific functions.
 * 
 * @author Christian Rechner
 */
public abstract class ODSHelper {

    // prepare dateformats to avoid instantiation a single object everting
    // parsing a date.
    private static Map<Integer, DateFormat> ODS_DATEFORMATS = new HashMap<Integer, DateFormat>();
    static {
        ODS_DATEFORMATS.put(4, new SimpleDateFormat("yyyy"));
        ODS_DATEFORMATS.put(6, new SimpleDateFormat("yyyyMM"));
        ODS_DATEFORMATS.put(8, new SimpleDateFormat("yyyyMMdd"));
        ODS_DATEFORMATS.put(10, new SimpleDateFormat("yyyyMMddHH"));
        ODS_DATEFORMATS.put(12, new SimpleDateFormat("yyyyMMddHHmm"));
        ODS_DATEFORMATS.put(14, new SimpleDateFormat("yyyyMMddHHmmss"));
        ODS_DATEFORMATS.put(16, new SimpleDateFormat("yyyyMMddHHmmssSS")); // NOT ODS conform!!!
        ODS_DATEFORMATS.put(17, new SimpleDateFormat("yyyyMMddHHmmssSSS"));
    }

    // the string used to separate the elements of a sequence
    private static final String SEQ_SEPARATOR = ",";

    /**
     * Return an ODS date from a <code>java.util.Date</code>.
     * 
     * @param date the <code>java.util.Date</code> to convert
     * @return the date in ODS date-format (YYYYMMDDhhmmss)
     */
    public static synchronized String asODSDate(Date date) {
        if (date == null) {
            return "";
        }
        return ODS_DATEFORMATS.get(14).format(date);
    }

    /**
     * Returns the java date from an ODS date.
     * 
     * @param odsDate the ODS date string
     * @return the java <code>java.util.Date</code> object, null if empty date
     * @throws IllegalArgumentException unable to parse
     */
    public static synchronized Date asJDate(String odsDate) {
        try {
            if (odsDate == null || odsDate.length() < 1) {
                return null;
            }
            DateFormat format = ODS_DATEFORMATS.get(odsDate.length());
            if (format == null) {
                throw new IllegalArgumentException("Invalid ODS date: " + odsDate);
            }
            return format.parse(odsDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid ODS date: " + odsDate);
        }
    }

    /**
     * Returns a Java long from ODS T_LONGLONG.
     * 
     * @param ll ODS T_LONGLONG value
     * @return Java long with the same value as ll
     */
    public static long asJLong(T_LONGLONG ll) {
        long tmp;
        if (ll.low >= 0) {
            tmp = (long) ll.high * 0x100000000L + (long) ll.low;
        } else {
            tmp = (long) (ll.high + 1) * 0x100000000L + (long) ll.low;
        }
        return (tmp);
    }

    /**
     * Returns an array of Java long from ODS T_LONGLONG.
     * 
     * @param ll array of ODS T_LONGLONG values
     * @return array of Java long values
     */
    public static long[] asJLong(T_LONGLONG[] ll) {
        long[] ar = new long[ll.length];
        for (int i = 0; i < ll.length; i++) {
            ar[i] = asJLong(ll[i]);
        }
        return ar;
    }

    /**
     * Return ODS T_LONGLONG from Java long.
     * 
     * @param v Java long value
     * @return ODS T_LONGLONG with the same value as v
     */
    public static T_LONGLONG asODSLongLong(long v) {
        return new T_LONGLONG((int) ((v >> 32) & 0xffffffffL), (int) (v & 0xffffffffL));
    }

    /**
     * Returns an array of ODS T_LONGLONG from Java longs.
     * 
     * @param v array of Java long values
     * @return array of ODS T_LONGLONG values
     */
    public static T_LONGLONG[] asODSLongLong(long[] v) {
        T_LONGLONG[] ar = new T_LONGLONG[v.length];
        for (int i = 0; i < v.length; i++) {
            ar[i] = asODSLongLong(v[i]);
        }
        return ar;
    }

    public static String getCurrentODSDate() {
        return asODSDate(new Date());
    }

    private static NameValue createNV(String valName, TS_Union union) {
        NameValue nv = new NameValue();
        nv.valName = valName;
        nv.value = new TS_Value();
        nv.value.flag = 15;
        nv.value.u = union;
        return nv;
    }

    private static NameValueUnit createNVU(String attrName, TS_Union union) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = attrName;
        nvu.value = new TS_Value();
        nvu.unit = "";
        nvu.value.flag = 15;
        nvu.value.u = union;
        return nvu;
    }

    private static NameValueUnit createNVU(String attrName, TS_Union union, String unit) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = attrName;
        nvu.value = new TS_Value();
        nvu.value.u = new TS_Union();
        nvu.unit = unit;
        nvu.value.flag = 15;
        nvu.value.u = union;
        return nvu;
    }

    public static NameValueUnit createCurrentDateNVU(String attrName) {
        TS_Union union = new TS_Union();
        union.dateVal(getCurrentODSDate());
        return createNVU(attrName, union);
    }

    public static NameValue createStringNV(String valName, String value) {
        NameValue nv = new NameValue();
        nv.valName = valName;
        nv.value = new TS_Value();
        nv.value.u = new TS_Union();
        if (value == null || value.length() < 1) {
            nv.value.flag = 0;
            nv.value.u.stringVal("");
        } else {
            nv.value.flag = 15;
            nv.value.u.stringVal(value);
        }
        return nv;
    }

    public static NameValueUnit createStringNVU(String valName, String value) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = valName;
        nvu.value = new TS_Value();
        nvu.unit = "";
        nvu.value.u = new TS_Union();
        if (value == null || value.length() < 1) {
            nvu.value.flag = 0;
            nvu.value.u.stringVal("");
        } else {
            nvu.value.flag = 15;
            nvu.value.u.stringVal(value);
        }
        return nvu;
    }

    public static NameValue createShortNV(String valName, short value) {
        TS_Union union = new TS_Union();
        union.shortVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createShortNVU(String valName, short value) {
        TS_Union union = new TS_Union();
        union.shortVal(value);
        return createNVU(valName, union);
    }

    public static NameValueUnit createShortNVU(String valName, short value, String unit) {
        TS_Union union = new TS_Union();
        union.shortVal(value);
        return createNVU(valName, union, unit);
    }

    public static NameValue createFloatNV(String valName, float value) {
        TS_Union union = new TS_Union();
        union.floatVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createFloatNVU(String valName, float value) {
        TS_Union union = new TS_Union();
        union.floatVal(value);
        return createNVU(valName, union);
    }

    public static NameValueUnit createFloatNVU(String valName, float value, String unit) {
        TS_Union union = new TS_Union();
        union.floatVal(value);
        return createNVU(valName, union, unit);
    }

    public static NameValue createBooleanNV(String valName, boolean value) {
        TS_Union union = new TS_Union();
        union.booleanVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createBooleanNVU(String valName, boolean value) {
        TS_Union union = new TS_Union();
        union.booleanVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createByteNV(String valName, byte value) {
        TS_Union union = new TS_Union();
        union.byteVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createByteNVU(String valName, byte value) {
        TS_Union union = new TS_Union();
        union.byteVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createBytestrNV(String valName, byte value[]) {
        TS_Union union = new TS_Union();
        union.bytestrVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createBytestrNVU(String valName, byte value[]) {
        TS_Union union = new TS_Union();
        union.bytestrVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createDoubleNV(String valName, double value) {
        TS_Union union = new TS_Union();
        union.doubleVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createDoubleNVU(String valName, Double value) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = valName;
        nvu.value = new TS_Value();
        nvu.unit = "";
        nvu.value.u = new TS_Union();
        if (value == null) {
            nvu.value.flag = 0;
            nvu.value.u.doubleVal(0);
        } else {
            nvu.value.flag = 15;
            nvu.value.u.doubleVal(value);
        }
        return nvu;
    }

    public static NameValue createComplexNV(String valName, T_COMPLEX value) {
        TS_Union union = new TS_Union();
        union.complexVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createComplexNVU(String valName, T_COMPLEX value) {
        TS_Union union = new TS_Union();
        union.complexVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createDComplexNV(String valName, T_DCOMPLEX value) {
        TS_Union union = new TS_Union();
        union.dcomplexVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createDComplexNVU(String valName, T_DCOMPLEX value) {
        TS_Union union = new TS_Union();
        union.dcomplexVal(value);
        return createNVU(valName, union);
    }

    public static NameValueUnit createDoubleNVU(String valName, double value, String unit) {
        TS_Union union = new TS_Union();
        union.doubleVal(value);
        return createNVU(valName, union, unit);
    }

    public static NameValue createLongNV(String valName, int value) {
        TS_Union union = new TS_Union();
        union.longVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createLongNVU(String valName, int value) {
        TS_Union union = new TS_Union();
        union.longVal(value);
        return createNVU(valName, union);
    }

    public static NameValueUnit createLongNVU(String valName, int value, String unit) {
        TS_Union union = new TS_Union();
        union.longVal(value);
        return createNVU(valName, union, unit);
    }

    public static NameValue createLongLongNV(String valName, T_LONGLONG value) {
        TS_Union union = new TS_Union();
        union.longlongVal(value);
        return createNV(valName, union);
    }

    public static NameValue createLongLongNV(String valName, long value) {
        return createLongLongNV(valName, asODSLongLong(value));
    }

    public static NameValueUnit createLongLongNVU(String valName, long value) {
        TS_Union union = new TS_Union();
        union.longlongVal(asODSLongLong(value));
        return createNVU(valName, union);
    }

    public static NameValueUnit createLongLongNVU(String valName, long value, String unit) {
        TS_Union union = new TS_Union();
        union.longlongVal(asODSLongLong(value));
        return createNVU(valName, union, unit);
    }

    public static NameValue createDateNV(String valName, String value) {
        NameValue nv = new NameValue();
        nv.valName = valName;
        nv.value = new TS_Value();
        nv.value.u = new TS_Union();
        if (value == null || value.length() < 1) {
            nv.value.flag = 0;
            nv.value.u.dateVal("");
        } else {
            nv.value.flag = 15;
            nv.value.u.dateVal(value);
        }
        return nv;
    }

    public static NameValue createDateNV(String valName, Date value) {
        return createDateNV(valName, asODSDate(value));
    }

    public static NameValueUnit createDateNVU(String valName, String value) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = valName;
        nvu.value = new TS_Value();
        nvu.unit = "";
        nvu.value.u = new TS_Union();
        if (value == null || value.length() < 1) {
            nvu.value.flag = 0;
            nvu.value.u.dateVal("");
        } else {
            nvu.value.flag = 15;
            nvu.value.u.dateVal(value);
        }
        return nvu;
    }

    public static NameValueUnit createDateNVU(String valName, Date value) {
        return createDateNVU(valName, asODSDate(value));
    }

    public static NameValue createEnumNV(String valName, int value) {
        TS_Union union = new TS_Union();
        union.enumVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createEnumNVU(String valName, int value) {
        TS_Union union = new TS_Union();
        union.enumVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createExtRefNV(String valName, T_ExternalReference value) {
        TS_Union union = new TS_Union();
        union.extRefVal(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createExtRefNVU(String valName, T_ExternalReference value) {
        TS_Union union = new TS_Union();
        union.extRefVal(value);
        return createNVU(valName, union);
    }

    public static NameValue createStringSeqNV(String valName, String values[]) {
        TS_Union union = new TS_Union();
        union.stringSeq(values);
        return createNV(valName, union);
    }

    public static NameValueUnit createStringSeqNVU(String valName, String values[]) {
        TS_Union union = new TS_Union();
        union.stringSeq(values);
        return createNVU(valName, union);
    }

    public static NameValue createShortSeqNV(String valName, short values[]) {
        TS_Union union = new TS_Union();
        union.shortSeq(values);
        return createNV(valName, union);
    }

    public static NameValueUnit createShortSeqNVU(String attrName, short values[]) {
        TS_Union union = new TS_Union();
        union.shortSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createFloatSeqNV(String attrName, float values[]) {
        TS_Union union = new TS_Union();
        union.floatSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createFloatSeqNVU(String attrName, float values[]) {
        TS_Union union = new TS_Union();
        union.floatSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createBooleanSeqNV(String attrName, boolean values[]) {
        TS_Union union = new TS_Union();
        union.booleanSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createBooleanSeqNVU(String attrName, boolean values[]) {
        TS_Union union = new TS_Union();
        union.booleanSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createByteSeqNV(String attrName, byte values[]) {
        TS_Union union = new TS_Union();
        union.byteSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createByteSeqNVU(String attrName, byte values[]) {
        TS_Union union = new TS_Union();
        union.byteSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createBytestrSeqNV(String valName, byte value[][]) {
        TS_Union union = new TS_Union();
        union.bytestrSeq(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createBytestrSeqNVU(String valName, byte value[][]) {
        TS_Union union = new TS_Union();
        union.bytestrSeq(value);
        return createNVU(valName, union);
    }

    public static NameValue createComplexSeqNV(String valName, T_COMPLEX value[]) {
        TS_Union union = new TS_Union();
        union.complexSeq(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createComplexSeqNVU(String valName, T_COMPLEX value[]) {
        TS_Union union = new TS_Union();
        union.complexSeq(value);
        return createNVU(valName, union);
    }

    public static NameValue createDComplexSeqNV(String valName, T_DCOMPLEX value[]) {
        TS_Union union = new TS_Union();
        union.dcomplexSeq(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createDComplexSeqNVU(String valName, T_DCOMPLEX value[]) {
        TS_Union union = new TS_Union();
        union.dcomplexSeq(value);
        return createNVU(valName, union);
    }

    public static NameValue createDoubleSeqNV(String attrName, double values[]) {
        TS_Union union = new TS_Union();
        union.doubleSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createDoubleSeqNVU(String attrName, double values[]) {
        TS_Union union = new TS_Union();
        union.doubleSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createEnumSeqNV(String valName, int value[]) {
        TS_Union union = new TS_Union();
        union.enumSeq(value);
        return createNV(valName, union);
    }

    public static NameValueUnit createEnumSeqNVU(String valName, int value[]) {
        TS_Union union = new TS_Union();
        union.enumSeq(value);
        return createNVU(valName, union);
    }

    public static NameValue createLongSeqNV(String attrName, int values[]) {
        TS_Union union = new TS_Union();
        union.longSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createLongSeqNVU(String attrName, int values[]) {
        TS_Union union = new TS_Union();
        union.longSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createLongLongSeqNV(String attrName, T_LONGLONG values[]) {
        TS_Union union = new TS_Union();
        union.longlongSeq(values);
        return createNV(attrName, union);
    }

    public static NameValue createLongLongSeqNV(String attrName, long values[]) {
        TS_Union union = new TS_Union();
        union.longlongSeq(asODSLongLong(values));
        return createNV(attrName, union);
    }

    public static NameValueUnit createLongLongSeqNVU(String attrName, long values[]) {
        TS_Union union = new TS_Union();
        union.longlongSeq(asODSLongLong(values));
        return createNVU(attrName, union);
    }

    public static NameValue createDateSeqNV(String attrName, String values[]) {
        TS_Union union = new TS_Union();
        union.dateSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createDateSeqNVU(String attrName, String values[]) {
        TS_Union union = new TS_Union();
        union.dateSeq(values);
        return createNVU(attrName, union);
    }

    public static NameValue createExtRefSeqNV(String attrName, T_ExternalReference values[]) {
        TS_Union union = new TS_Union();
        union.extRefSeq(values);
        return createNV(attrName, union);
    }

    public static NameValueUnit createExtRefSeqNVU(String attrName, T_ExternalReference values[]) {
        TS_Union union = new TS_Union();
        union.extRefSeq(values);
        return createNVU(attrName, union);
    }

    public static boolean isNullVal(TS_Value value) {
        if (value.flag != 15)
            return true;
        return false;
    }

    public static boolean isNullVal(NameValueUnit nvu) {
        if (nvu.value.flag != 15)
            return true;
        return false;
    }

    public static long getLongLongVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return 0;
        else
            return asJLong(nvu.value.u.longlongVal());
    }

    public static int getLongVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return 0;
        else
            return nvu.value.u.longVal();
    }

    public static double getDoubleVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return 0d;
        else
            return nvu.value.u.doubleVal();
    }

    public static short getShortVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return (short) 0;
        else
            return nvu.value.u.shortVal();
    }

    public static byte getByteVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return (byte) 0;
        else
            return nvu.value.u.byteVal();
    }

    public static float getFloatVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return 0f;
        else
            return nvu.value.u.floatVal();
    }

    public static String getStringVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return "";
        else
            return nvu.value.u.stringVal();
    }

    public static int getEnumVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return 0;
        else
            return nvu.value.u.enumVal();
    }

    public static String getDateVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return "";
        else
            return nvu.value.u.dateVal();
    }

    public static boolean getBooleanVal(NameValueUnit nvu) {
        if (isNullVal(nvu))
            return false;
        else
            return nvu.value.u.booleanVal();
    }

    public static String[] getStringSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new String[0];
        } else {
            return nvu.value.u.stringSeq();
        }
    }

    public static short[] getShortSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new short[0];
        } else {
            return nvu.value.u.shortSeq();
        }
    }

    public static float[] getFloatSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new float[0];
        } else {
            return nvu.value.u.floatSeq();
        }
    }

    public static boolean[] getBooleanSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new boolean[0];
        } else {
            return nvu.value.u.booleanSeq();
        }
    }

    public static byte[] getByteSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new byte[0];
        } else {
            return nvu.value.u.byteSeq();
        }
    }

    public static int[] getLongSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new int[0];
        } else {
            return nvu.value.u.longSeq();
        }
    }

    public static double[] getDoubleSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new double[0];
        } else {
            return nvu.value.u.doubleSeq();
        }
    }

    public static long[] getLongLongSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new long[0];
        } else {
            return asJLong(nvu.value.u.longlongSeq());
        }
    }

    public static String[] getDateSeq(NameValueUnit nvu) {
        if (isNullVal(nvu)) {
            return new String[0];
        } else {
            return nvu.value.u.stringSeq();
        }
    }

    public static NameValue cloneNV(NameValue source) {
        NameValue nv = new NameValue();
        nv.valName = source.valName;
        nv.value = cloneTS_Value(source.value);
        return nv;
    }

    public static NameValueUnit cloneNVU(NameValueUnit source) {
        NameValueUnit nvu = new NameValueUnit();
        nvu.unit = source.unit;
        nvu.valName = source.valName;
        nvu.value = cloneTS_Value(source.value);
        return nvu;
    }

    public static TS_Value cloneTS_Value(TS_Value source) {
        TS_Value value = new TS_Value();
        value.flag = source.flag;
        value.u = cloneTS_Union(source.u);
        return value;
    }

    public static TS_Union cloneTS_Union(TS_Union source) {
        TS_Union u = new TS_Union();
        DataType dt = source.discriminator();
        // DS_BOOLEAN
        if (dt == DataType.DS_BOOLEAN) {
            int length = source.booleanSeq().length;
            boolean[] ar = new boolean[length];
            System.arraycopy(source.booleanSeq(), 0, ar, 0, length);
            u.booleanSeq(ar);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            int length = source.byteSeq().length;
            byte[] ar = new byte[length];
            System.arraycopy(source.byteSeq(), 0, ar, 0, length);
            u.byteSeq(ar);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            int length = source.bytestrSeq().length;
            byte[][] ar = new byte[length][];
            for (int i = 0; i < length; i++) {
                int l = source.bytestrSeq()[i].length;
                ar[i] = new byte[l];
                System.arraycopy(source.bytestrSeq()[i], 0, ar[i], 0, l);
            }
            u.bytestrSeq(ar);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            int length = source.complexSeq().length;
            T_COMPLEX[] ar = new T_COMPLEX[length];
            for (int i = 0; i < length; i++) {
                ar[i] = new T_COMPLEX();
                ar[i].i = source.complexSeq()[i].i;
                ar[i].r = source.complexSeq()[i].r;
            }
            u.complexSeq(ar);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            int length = source.dateSeq().length;
            String[] ar = new String[length];
            System.arraycopy(source.dateSeq(), 0, ar, 0, length);
            u.dateSeq(ar);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            int length = source.dcomplexSeq().length;
            T_DCOMPLEX[] ar = new T_DCOMPLEX[length];
            for (int i = 0; i < length; i++) {
                ar[i] = new T_DCOMPLEX();
                ar[i].i = source.dcomplexSeq()[i].i;
                ar[i].r = source.dcomplexSeq()[i].r;
            }
            u.dcomplexSeq(ar);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            int length = source.doubleSeq().length;
            double[] ar = new double[length];
            System.arraycopy(source.doubleSeq(), 0, ar, 0, length);
            u.doubleSeq(ar);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            int length = source.enumSeq().length;
            int[] ar = new int[length];
            System.arraycopy(source.enumSeq(), 0, ar, 0, length);
            u.enumSeq(ar);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            int length = source.extRefSeq().length;
            T_ExternalReference[] ar = new T_ExternalReference[length];
            for (int i = 0; i < length; i++) {
                ar[i] = new T_ExternalReference();
                ar[i].description = source.extRefSeq()[i].description;
                ar[i].location = source.extRefSeq()[i].location;
                ar[i].mimeType = source.extRefSeq()[i].mimeType;
            }
            u.extRefSeq(ar);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            int length = source.floatSeq().length;
            float[] ar = new float[length];
            System.arraycopy(source.floatSeq(), 0, ar, 0, length);
            u.floatSeq(ar);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            int length = source.longSeq().length;
            int[] ar = new int[length];
            System.arraycopy(source.longSeq(), 0, ar, 0, length);
            u.longSeq(ar);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            int length = source.longlongSeq().length;
            T_LONGLONG[] ar = new T_LONGLONG[length];
            for (int i = 0; i < length; i++) {
                ar[i] = new T_LONGLONG();
                ar[i].low = source.longlongSeq()[i].low;
                ar[i].high = source.longlongSeq()[i].high;
            }
            u.longlongSeq(ar);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            int length = source.shortSeq().length;
            short[] ar = new short[length];
            System.arraycopy(source.shortSeq(), 0, ar, 0, length);
            u.shortSeq(ar);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            int length = source.stringSeq().length;
            String[] ar = new String[length];
            System.arraycopy(source.stringSeq(), 0, ar, 0, length);
            u.stringSeq(ar);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            u.booleanVal(source.booleanVal());
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            u.byteVal(source.byteVal());
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            int length = source.bytestrVal().length;
            byte[] ar = new byte[length];
            System.arraycopy(source.bytestrVal(), 0, ar, 0, length);
            u.bytestrVal(ar);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            T_COMPLEX c = new T_COMPLEX();
            c.i = source.complexVal().i;
            c.r = source.complexVal().r;
            u.complexVal(c);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            u.dateVal(source.dateVal());
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            T_DCOMPLEX c = new T_DCOMPLEX();
            c.i = source.dcomplexVal().i;
            c.r = source.dcomplexVal().r;
            u.dcomplexVal(c);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            u.doubleVal(source.doubleVal());
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            u.enumVal(source.enumVal());
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference e = new T_ExternalReference();
            e.description = source.extRefVal().description;
            e.location = source.extRefVal().location;
            e.mimeType = source.extRefVal().mimeType;
            u.extRefVal(e);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            u.floatVal(source.floatVal());
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            u.longVal(source.longVal());
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            T_LONGLONG l = new T_LONGLONG();
            l.low = source.longlongVal().low;
            l.high = source.longlongVal().high;
            u.longlongVal(l);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            u.shortVal(source.shortVal());
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            u.stringVal(source.stringVal());
        }
        return u;
    }

    public static int tsUnionSeqLength(TS_UnionSeq unionSeq) {
        DataType dt = unionSeq.discriminator();
        int length = 0;
        if (dt == DataType.DT_BLOB)
            length = unionSeq.blobVal().length;
        else if (dt == DataType.DT_BOOLEAN)
            length = unionSeq.booleanVal().length;
        else if (dt == DataType.DT_BYTE)
            length = unionSeq.byteVal().length;
        else if (dt == DataType.DT_BYTESTR)
            length = unionSeq.bytestrVal().length;
        else if (dt == DataType.DT_COMPLEX)
            length = unionSeq.complexVal().length;
        else if (dt == DataType.DT_DATE)
            length = unionSeq.dateVal().length;
        else if (dt == DataType.DT_DCOMPLEX)
            length = unionSeq.dcomplexVal().length;
        else if (dt == DataType.DT_DOUBLE)
            length = unionSeq.doubleVal().length;
        else if (dt == DataType.DT_ENUM)
            length = unionSeq.enumVal().length;
        else if (dt == DataType.DT_EXTERNALREFERENCE)
            length = unionSeq.extRefVal().length;
        else if (dt == DataType.DT_FLOAT)
            length = unionSeq.floatVal().length;
        else if (dt == DataType.DT_LONG)
            length = unionSeq.longVal().length;
        else if (dt == DataType.DT_LONGLONG)
            length = unionSeq.longlongVal().length;
        else if (dt == DataType.DT_SHORT)
            length = unionSeq.shortVal().length;
        else if (dt == DataType.DT_STRING)
            length = unionSeq.stringVal().length;
        else if (dt == DataType.DS_BOOLEAN)
            length = unionSeq.booleanSeq().length;
        else if (dt == DataType.DS_BYTE)
            length = unionSeq.byteSeq().length;
        else if (dt == DataType.DS_BYTESTR)
            length = unionSeq.bytestrSeq().length;
        else if (dt == DataType.DS_COMPLEX)
            length = unionSeq.complexSeq().length;
        else if (dt == DataType.DS_DATE)
            length = unionSeq.dateSeq().length;
        else if (dt == DataType.DS_DCOMPLEX)
            length = unionSeq.dcomplexSeq().length;
        else if (dt == DataType.DS_DOUBLE)
            length = unionSeq.doubleSeq().length;
        else if (dt == DataType.DS_ENUM)
            length = unionSeq.enumSeq().length;
        else if (dt == DataType.DS_EXTERNALREFERENCE)
            length = unionSeq.extRefSeq().length;
        else if (dt == DataType.DS_FLOAT)
            length = unionSeq.floatSeq().length;
        else if (dt == DataType.DS_LONG)
            length = unionSeq.longSeq().length;
        else if (dt == DataType.DS_LONGLONG)
            length = unionSeq.longlongSeq().length;
        else if (dt == DataType.DS_SHORT)
            length = unionSeq.shortSeq().length;
        else if (dt == DataType.DS_STRING)
            length = unionSeq.stringSeq().length;
        return length;
    }

    /*******************************************************************************************************************
     * Methods for datatype conversions.
     ******************************************************************************************************************/

    /**
     * Converts an ASAM ODS value <code>org.asam.ods.TS_Value</code> to a <code>org.asam.ods.TS_ValueSeq</code> having
     * exactly one row.
     * 
     * @param value The input value.
     * @return The output value.
     * @throws AoException Error converting value.
     */
    public static TS_ValueSeq tsValue2tsValueSeq(TS_Value value) throws AoException {
        TS_ValueSeq valueSeq = new TS_ValueSeq();
        valueSeq.flag = new short[] { value.flag };
        valueSeq.u = tsUnion2tsUnionSeq(value.u);
        return valueSeq;
    }

    /**
     * Converts an ASAM ODS value <code>org.asam.ods.TS_Union</code> to a <code>org.asam.ods.TS_UnionSeq</code> having
     * exactly one row.
     * 
     * @param u The input value.
     * @return The output value.
     * @throws AoException Error converting value.
     */
    public static TS_UnionSeq tsUnion2tsUnionSeq(TS_Union u) throws AoException {
        TS_UnionSeq uSeq = new TS_UnionSeq();
        DataType dt = u.discriminator();
        // DT_BLOB
        if (dt == DataType.DT_BLOB) {
            uSeq.blobVal(new Blob[] { u.blobVal() });
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            uSeq.booleanVal(new boolean[] { u.booleanVal() });
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            uSeq.byteVal(new byte[] { u.byteVal() });
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            uSeq.bytestrVal(new byte[][] { u.bytestrVal() });
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            uSeq.complexVal(new T_COMPLEX[] { u.complexVal() });
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            uSeq.dateVal(new String[] { u.dateVal() });
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            uSeq.dcomplexVal(new T_DCOMPLEX[] { u.dcomplexVal() });
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            uSeq.doubleVal(new double[] { u.doubleVal() });
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            uSeq.enumVal(new int[] { u.enumVal() });
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            uSeq.extRefVal(new T_ExternalReference[] { u.extRefVal() });
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            uSeq.floatVal(new float[] { u.floatVal() });
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            uSeq.longVal(new int[] { u.longVal() });
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            uSeq.longlongVal(new T_LONGLONG[] { u.longlongVal() });
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            uSeq.shortVal(new short[] { u.shortVal() });
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            uSeq.stringVal(new String[] { u.stringVal() });
        }
        // DS_BOOLEAN
        else if (dt == DataType.DS_BOOLEAN) {
            uSeq.booleanSeq(new boolean[][] { u.booleanSeq() });
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            uSeq.byteSeq(new byte[][] { u.byteSeq() });
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            uSeq.bytestrSeq(new byte[][][] { u.bytestrSeq() });
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            uSeq.complexSeq(new T_COMPLEX[][] { u.complexSeq() });
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            uSeq.dateSeq(new String[][] { u.dateSeq() });
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            uSeq.dcomplexSeq(new T_DCOMPLEX[][] { u.dcomplexSeq() });
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            uSeq.doubleSeq(new double[][] { u.doubleSeq() });
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            uSeq.enumSeq(new int[][] { u.enumSeq() });
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            uSeq.extRefSeq(new T_ExternalReference[][] { u.extRefSeq() });
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            uSeq.floatSeq(new float[][] { u.floatSeq() });
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            uSeq.longSeq(new int[][] { u.longSeq() });
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            uSeq.longlongSeq(new T_LONGLONG[][] { u.longlongSeq() });
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            uSeq.shortSeq(new short[][] { u.shortSeq() });
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            uSeq.stringSeq(new String[][] { u.stringSeq() });
        }
        // unknown dataType
        else {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "Unknown DataType: " + dt.value());
        }
        return uSeq;
    }

    public static TS_Value tsValueSeq2tsValue(TS_ValueSeq valueSeq, int pos) throws AoException {
        TS_Value tsValue = new TS_Value();
        tsValue.u = tsUnionSeq2tsUnion(valueSeq.u, pos);
        tsValue.flag = valueSeq.flag[pos];
        return tsValue;
    }

    public static java.lang.Object tsValue2jObject(TS_Value value) throws AoException {
        if (value == null || value.flag != 15) {
            return null;
        }

        DataType dt = value.u.discriminator();
        // DT_BLOB
        if (dt == DataType.DT_BLOB) {
            return value.u.blobVal();
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            return value.u.booleanVal();
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            return value.u.byteVal();
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            return value.u.bytestrVal();
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            return value.u.complexVal();
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            return value.u.dateVal();
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            return value.u.dcomplexVal();
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            return value.u.doubleVal();
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            return value.u.enumVal();
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            return value.u.extRefVal();
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            return value.u.floatVal();
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            return value.u.longVal();
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            return value.u.longlongVal();
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            return value.u.shortVal();
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            return value.u.stringVal();
        }
        // DS_BOOLEAN
        else if (dt == DataType.DS_BOOLEAN) {
            return value.u.booleanSeq();
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            return value.u.byteSeq();
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            return value.u.bytestrSeq();
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            return value.u.complexSeq();
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            return value.u.dateSeq();
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            return value.u.dcomplexSeq();
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            return value.u.doubleSeq();
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            return value.u.enumSeq();
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            return value.u.extRefSeq();
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            return value.u.floatSeq();
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            return value.u.longSeq();
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            return value.u.longlongSeq();
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            return value.u.shortSeq();
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            return value.u.stringSeq();
        }
        // unknown dataType
        else {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "Unsupported DataType: " + dataType2String(dt));
        }
    }

    public static TS_Value jObject2tsValue(DataType dt, java.lang.Object obj) throws AoException {
        if (obj == null) {
            return createEmptyTS_Value(dt);
        }

        TS_Value value = new TS_Value();
        value.flag = 15;
        value.u = new TS_Union();

        // DT_BLOB
        if (dt == DataType.DT_BLOB) {
            value.u.blobVal((Blob) obj);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            value.u.booleanVal((Boolean) obj);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            value.u.byteVal((Byte) obj);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            value.u.bytestrVal((byte[]) obj);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            value.u.complexVal((T_COMPLEX) obj);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            value.u.dateVal((String) obj);
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            value.u.dcomplexVal((T_DCOMPLEX) obj);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            value.u.doubleVal((Double) obj);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            value.u.enumVal((Integer) obj);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            value.u.extRefVal((T_ExternalReference) obj);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            value.u.floatVal((Float) obj);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            value.u.longVal((Integer) obj);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            value.u.longlongVal((T_LONGLONG) obj);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            value.u.shortVal((Short) obj);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            value.u.stringVal((String) obj);
        }
        // DS_BOOLEAN
        else if (dt == DataType.DS_BOOLEAN) {
            value.u.booleanSeq((boolean[]) obj);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            value.u.byteSeq((byte[]) obj);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            value.u.bytestrSeq((byte[][]) obj);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            value.u.complexSeq((T_COMPLEX[]) obj);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            value.u.dateSeq((String[]) obj);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            value.u.dcomplexSeq((T_DCOMPLEX[]) obj);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            value.u.doubleSeq((double[]) obj);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            value.u.enumSeq((int[]) obj);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            value.u.extRefSeq((T_ExternalReference[]) obj);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            value.u.floatSeq((float[]) obj);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            value.u.longSeq((int[]) obj);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            value.u.longlongSeq((T_LONGLONG[]) obj);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            value.u.shortSeq((short[]) obj);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            value.u.stringSeq((String[]) obj);
        }
        // unknown dataType
        else {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "Unsupported DataType: " + dataType2String(dt));
        }
        return value;
    }

    public static TS_Union tsUnionSeq2tsUnion(TS_UnionSeq uSeq, int pos) throws AoException {
        TS_Union u = new TS_Union();
        DataType dt = uSeq.discriminator();
        // DT_BLOB
        if (dt == DataType.DT_BLOB) {
            u.blobVal(uSeq.blobVal()[pos]);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            u.booleanVal(uSeq.booleanVal()[pos]);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            u.byteVal(uSeq.byteVal()[pos]);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            u.bytestrVal(uSeq.bytestrVal()[pos]);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            u.complexVal(uSeq.complexVal()[pos]);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            u.dateVal(uSeq.dateVal()[pos]);
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            u.dcomplexVal(uSeq.dcomplexVal()[pos]);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            u.doubleVal(uSeq.doubleVal()[pos]);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            u.enumVal(uSeq.enumVal()[pos]);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            u.extRefVal(uSeq.extRefVal()[pos]);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            u.floatVal(uSeq.floatVal()[pos]);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            u.longVal(uSeq.longVal()[pos]);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            u.longlongVal(uSeq.longlongVal()[pos]);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            u.shortVal(uSeq.shortVal()[pos]);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            u.stringVal(uSeq.stringVal()[pos]);
        }
        // DS_BOOLEAN
        else if (dt == DataType.DS_BOOLEAN) {
            u.booleanSeq(uSeq.booleanSeq()[pos]);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            u.byteSeq(uSeq.byteSeq()[pos]);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            u.bytestrSeq(uSeq.bytestrSeq()[pos]);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            u.complexSeq(uSeq.complexSeq()[pos]);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            u.dateSeq(uSeq.dateSeq()[pos]);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            u.dcomplexSeq(uSeq.dcomplexSeq()[pos]);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            u.doubleSeq(uSeq.doubleSeq()[pos]);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            u.enumSeq(uSeq.enumSeq()[pos]);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            u.extRefSeq(uSeq.extRefSeq()[pos]);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            u.floatSeq(uSeq.floatSeq()[pos]);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            u.longSeq(uSeq.longSeq()[pos]);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            u.longlongSeq(uSeq.longlongSeq()[pos]);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            u.shortSeq(uSeq.shortSeq()[pos]);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            u.stringSeq(uSeq.stringSeq()[pos]);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            u.booleanVal(uSeq.booleanVal()[pos]);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            u.byteVal(uSeq.byteVal()[pos]);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            u.bytestrVal(uSeq.bytestrVal()[pos]);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            u.complexVal(uSeq.complexVal()[pos]);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            u.dateVal(uSeq.dateVal()[pos]);
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            u.dcomplexVal(uSeq.dcomplexVal()[pos]);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            u.doubleVal(uSeq.doubleVal()[pos]);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            u.enumVal(uSeq.enumVal()[pos]);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            u.extRefVal(uSeq.extRefVal()[pos]);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            u.floatVal(uSeq.floatVal()[pos]);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            u.longVal(uSeq.longVal()[pos]);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            u.longlongVal(uSeq.longlongVal()[pos]);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            u.shortVal(uSeq.shortVal()[pos]);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            u.stringVal(uSeq.stringVal()[pos]);
        }
        // unknown dataType
        else {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                                  "Unsupported DataType: " + ODSHelper.dataType2String(dt));
        }
        return u;
    }

    /**
     * Creates an empty <code>org.asam.ods.NameValueUnit</code> with given datatype as discriminator and flag=0.
     * 
     * @param valName The attribute name.
     * @param dt The datatype.
     * @return TS_Value The TS_Value object.
     * @throws AoException Error creating object.
     */
    public static NameValueUnit createEmptyNVU(String valName, DataType dt) throws AoException {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = valName;
        nvu.unit = "";
        nvu.value = createEmptyTS_Value(dt);
        return nvu;
    }

    /**
     * Creates an empty <code>org.asam.ods.TS_Value</code> with given datatype as discriminator and flag=0.
     * 
     * @param dt The datatype.
     * @return TS_Value The TS_Value object.
     * @throws AoException Error creating object.
     */
    public static TS_Value createEmptyTS_Value(DataType dt) throws AoException {
        TS_Value value = new TS_Value();
        value.flag = 0;
        value.u = createEmptyTS_Union(dt);
        return value;
    }

    /**
     * Creates an empty <code>org.asam.ods.TS_Union</code> with given datatype as discrimitor.
     * 
     * @param dt The datatype.
     * @return The TS_Union object.
     * @throws AoException Error creating object.
     */
    public static TS_Union createEmptyTS_Union(DataType dt) throws AoException {
        TS_Union u = new TS_Union();
        // DS_BOOLEAN
        if (dt == DataType.DS_BOOLEAN) {
            u.booleanSeq(new boolean[0]);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            u.byteSeq(new byte[0]);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            u.bytestrSeq(new byte[0][0]);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            u.complexSeq(new T_COMPLEX[0]);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            u.dateSeq(new String[0]);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            u.dcomplexSeq(new T_DCOMPLEX[0]);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            u.doubleSeq(new double[0]);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            u.enumSeq(new int[0]);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            u.extRefSeq(new T_ExternalReference[0]);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            u.floatSeq(new float[0]);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            u.longSeq(new int[0]);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            u.longlongSeq(new T_LONGLONG[0]);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            u.shortSeq(new short[0]);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            u.stringSeq(new String[0]);
        }
        // DT_BLOB
        else if (dt == DataType.DT_BLOB) {
            u.blobVal(null);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            u.booleanVal(false);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            u.byteVal((byte) 0);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            u.bytestrVal(new byte[0]);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            T_COMPLEX complex = new T_COMPLEX();
            complex.i = 0;
            complex.r = 0;
            u.complexVal(complex);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            u.dateVal("");
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            T_DCOMPLEX dcomplex = new T_DCOMPLEX();
            dcomplex.i = 0;
            dcomplex.r = 0;
            u.dcomplexVal(dcomplex);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            u.doubleVal(0);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            u.enumVal(0);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference extRef = new T_ExternalReference();
            extRef.description = "";
            extRef.mimeType = "";
            extRef.location = "";
            u.extRefVal(extRef);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            u.floatVal(0);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            u.longVal(0);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            u.longlongVal(asODSLongLong(0));
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            u.shortVal((short) 0);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            u.stringVal("");
        }
        // unknown
        else {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Unsupported datatype: " + dataType2String(dt));
        }
        return u;
    }

    /*******************************************************************************************************************
     * Methods for string conversions.
     ******************************************************************************************************************/

    /**
     * Converts the value of a <code>org.asam.ods.NameValueUnit</code> the a string representation.
     * 
     * @param nvu The value.
     * @return The string representation.
     * @throws AoException Error converting.
     */
    public static String nameValueUnit2string(NameValueUnit nvu) throws AoException {
        return tsValue2string(nvu.value);
    }

    /**
     * Converts the value of a <code>org.asam.ods.NameValue</code> the a string representation.
     * 
     * @param nv The value.
     * @return The string representation.
     * @throws AoException Error converting.
     */
    public static String nameValue2string(NameValue nv) throws AoException {
        return tsValue2string(nv.value);
    }

    /**
     * Converts the value of a <code>org.asam.ods.TS_Value</code> the a string representation.
     * 
     * @param value The value.
     * @return The string representation.
     * @throws AoException Error converting.
     */
    public static String tsValue2string(TS_Value value) throws AoException {
        if (value.flag != 15) {
            return "";
        }
        return tsUnion2String(value.u);
    }

    /**
     * Converts the value of a <code>org.asam.ods.TS_Union</code> the a string representation.
     * 
     * @param u The value.
     * @return The string representation.
     * @throws AoException Error converting.
     */
    public static String tsUnion2String(TS_Union u) throws AoException {
        DataType dt = u.discriminator();
        // DS_BOOLEAN
        if (dt == DataType.DS_BOOLEAN) {
            boolean[] booleanSeq = u.booleanSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < booleanSeq.length; i++) {
                str.append(booleanSeq[i]);
                if (i < booleanSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            byte[] byteSeq = u.byteSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < byteSeq.length; i++) {
                str.append(byteToHex(byteSeq[i]));
                if (i < byteSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            StringBuffer str = new StringBuffer();
            for (int x = 0; x < u.bytestrSeq().length; x++) {
                byte[] ar = u.bytestrSeq()[x];
                for (int i = 0; i < ar.length; i++) {
                    str.append(byteToHex(ar[i]));
                    if (i < ar.length - 1) {
                        str.append(" ");
                    }
                }
                if (x < u.bytestrSeq().length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            T_COMPLEX[] complexSeq = u.complexSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < complexSeq.length; i++) {
                str.append(complexSeq[i].r);
                str.append(" ");
                str.append(complexSeq[i].i);
                if (i < complexSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            String[] dateSeq = u.dateSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < dateSeq.length; i++) {
                str.append(dateSeq[i]);
                if (i < dateSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            T_DCOMPLEX[] complexSeq = u.dcomplexSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < complexSeq.length; i++) {
                str.append(complexSeq[i].r);
                str.append(" ");
                str.append(complexSeq[i].i);
                if (i < complexSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            double[] doubleSeq = u.doubleSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < doubleSeq.length; i++) {
                str.append(doubleSeq[i]);
                if (i < doubleSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            int[] enumSeq = u.enumSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < enumSeq.length; i++) {
                str.append(enumSeq[i]);
                if (i < enumSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            T_ExternalReference[] extRefs = u.extRefSeq();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < extRefs.length; i++) {
                T_ExternalReference extRef = extRefs[i];
                sb.append(extRef.description);
                sb.append("[");
                sb.append(extRef.mimeType);
                sb.append(", ");
                sb.append(extRef.location);
                sb.append("]");
                if (i < extRefs.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            float[] floatSeq = u.floatSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < floatSeq.length; i++) {
                str.append(floatSeq[i]);
                if (i < floatSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            int[] longSeq = u.longSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < longSeq.length; i++) {
                str.append(longSeq[i]);
                if (i < longSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            long[] longlongSeq = asJLong(u.longlongSeq());
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < longlongSeq.length; i++) {
                str.append(longlongSeq[i]);
                if (i < longlongSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            short[] shortSeq = u.shortSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < shortSeq.length; i++) {
                str.append(shortSeq[i]);
                if (i < shortSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            String[] stringSeq = u.stringSeq();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < stringSeq.length; i++) {
                str.append(stringSeq[i]);
                if (i < stringSeq.length - 1) {
                    str.append(SEQ_SEPARATOR);
                }
            }
            return str.toString();
        }
        // DT_UNKNOWN
        else if (dt == DataType.DT_UNKNOWN) {
            return "";
        }
        // DT_BLOB
        else if (dt == DataType.DT_BLOB) {
            return u.blobVal().getHeader();
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            return String.valueOf(u.booleanVal());
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            return byteToHex(u.byteVal());
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            byte[] bytestr = u.bytestrVal();
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < bytestr.length; i++) {
                str.append(byteToHex(bytestr[i]));
                if (i < bytestr.length - 1) {
                    str.append(" ");
                }
            }
            return str.toString();
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            T_COMPLEX complex = u.complexVal();
            StringBuffer str = new StringBuffer();
            str.append(complex.r);
            str.append(" ");
            str.append(complex.i);
            return str.toString();
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            return String.valueOf(u.dateVal());
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            T_DCOMPLEX complex = u.dcomplexVal();
            StringBuffer str = new StringBuffer();
            str.append(complex.r);
            str.append(" ");
            str.append(complex.i);
            return str.toString();
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            return String.valueOf(u.doubleVal());
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            return String.valueOf(u.enumVal());
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            StringBuffer sb = new StringBuffer();
            T_ExternalReference extRef = u.extRefVal();
            sb.append(extRef.description);
            sb.append("[");
            sb.append(extRef.mimeType);
            sb.append(", ");
            sb.append(extRef.location);
            sb.append("]");
            return sb.toString();
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            return String.valueOf(u.floatVal());
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            return String.valueOf(u.longVal());
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            return String.valueOf(asJLong(u.longlongVal()));
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            return String.valueOf(u.shortVal());
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            return u.stringVal();
        }
        // unknown datatype
        throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0,
                              "Unknown DataType: " + dataType2String(dt));
    }

    public static NameValueUnit string2NameValueUnit(DataType dt, String value, String valName, String unit)
            throws AoException {
        NameValueUnit nvu = new NameValueUnit();
        nvu.valName = valName;
        nvu.unit = unit;
        nvu.value = string2tsValue(dt, value);
        return nvu;
    }

    public static NameValue string2NameValue(DataType dt, String value, String valName) throws AoException {
        NameValue nv = new NameValue();
        nv.valName = valName;
        nv.value = string2tsValue(dt, value);
        return nv;
    }

    public static TS_Value string2tsValue(DataType dt, String value) throws AoException {
        TS_Value tsValue = new TS_Value();
        if (value == null || value.length() < 1) {
            tsValue.flag = 0;
        } else {
            tsValue.flag = 15;
        }
        tsValue.u = string2tsUnion(dt, value);
        return tsValue;
    }

    public static TS_Union string2tsUnion(DataType dt, String value) throws AoException {
        TS_Union union = new TS_Union();
        // DS_BOOLEAN
        if (dt == DataType.DS_BOOLEAN && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            boolean[] res = new boolean[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Boolean.valueOf(strAr[i]);
            }
            union.booleanSeq(res);
        } else if (dt == DataType.DS_BOOLEAN && value.length() < 1) {
            union.booleanSeq(new boolean[0]);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            byte[] res = new byte[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = hexToByte(strAr[i]);
            }
            union.byteSeq(res);
        } else if (dt == DataType.DS_BYTE && value.length() < 1) {
            union.byteSeq(new byte[0]);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 1, "Not implemented");
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            T_COMPLEX[] ar = new T_COMPLEX[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                String[] splitted = strAr[i].split(" ");
                ar[i] = new T_COMPLEX();
                ar[i].r = Float.valueOf(splitted[0]);
                ar[i].i = Float.valueOf(splitted[1]);
            }
            union.complexSeq(ar);
        } else if (dt == DataType.DS_COMPLEX && value.length() < 1) {
            union.complexSeq(new T_COMPLEX[0]);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            union.dateSeq(strAr);
        } else if (dt == DataType.DS_DATE && value.length() < 1) {
            union.dateSeq(new String[0]);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            T_DCOMPLEX[] ar = new T_DCOMPLEX[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                String[] splitted = strAr[i].split(" ");
                ar[i] = new T_DCOMPLEX();
                ar[i].r = Double.valueOf(splitted[0]);
                ar[i].i = Double.valueOf(splitted[1]);
            }
            union.dcomplexSeq(ar);
        } else if (dt == DataType.DS_DCOMPLEX && value.length() < 1) {
            union.dcomplexSeq(new T_DCOMPLEX[0]);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            double[] res = new double[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Double.valueOf(strAr[i]);
            }
            union.doubleSeq(res);
        } else if (dt == DataType.DS_DOUBLE && value.length() < 1) {
            union.doubleSeq(new double[0]);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            int[] res = new int[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Integer.valueOf(strAr[i]);
            }
            union.enumSeq(res);
        } else if (dt == DataType.DS_ENUM && value.length() < 1) {
            union.enumSeq(new int[0]);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            Pattern pattern = Pattern.compile("[^,]*?\\[.*?,.*?\\]");
            Matcher matcher = pattern.matcher(value);
            List<T_ExternalReference> extRefList = new ArrayList<T_ExternalReference>();
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String entry = value.substring(start, end);
                Pattern subpattern = Pattern.compile("(.*?)\\[(.*?),(.*?)\\]");
                Matcher submatcher = subpattern.matcher(entry);
                if (submatcher.find()) {
                    T_ExternalReference extRef = new T_ExternalReference(submatcher.group(1), submatcher.group(2),
                                                                         submatcher.group(3));
                    extRefList.add(extRef);
                }
            }
            if (extRefList.size() > 0) {
                union.extRefSeq(extRefList.toArray(new T_ExternalReference[extRefList.size()]));
            } else {
                union.extRefSeq(new T_ExternalReference[0]);
            }
        } else if (dt == DataType.DS_EXTERNALREFERENCE && value.length() < 1) {
            union.extRefSeq(new T_ExternalReference[0]);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            float[] res = new float[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Float.valueOf(strAr[i]);
            }
            union.floatSeq(res);
        } else if (dt == DataType.DS_FLOAT && value.length() < 1) {
            union.floatSeq(new float[0]);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            int[] res = new int[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Integer.valueOf(strAr[i]);
            }
            union.longSeq(res);
        } else if (dt == DataType.DS_LONG && value.length() < 1) {
            union.longSeq(new int[0]);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            long[] res = new long[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Long.valueOf(strAr[i]);
            }
            union.longlongSeq(ODSHelper.asODSLongLong(res));
        } else if (dt == DataType.DS_LONGLONG && value.length() < 1) {
            union.longlongSeq(new T_LONGLONG[0]);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            short[] res = new short[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                res[i] = Short.valueOf(strAr[i]);
            }
            union.shortSeq(res);
        } else if (dt == DataType.DS_SHORT && value.length() < 1) {
            union.shortSeq(new short[0]);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING && value.length() > 0) {
            String[] strAr = value.split(SEQ_SEPARATOR);
            union.stringSeq(strAr);
        } else if (dt == DataType.DS_STRING && value.length() < 1) {
            union.stringSeq(new String[0]);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN && value.length() > 0) {
            union.booleanVal(Boolean.valueOf(value));
        } else if (dt == DataType.DT_BOOLEAN && value.length() < 1) {
            union.booleanVal(false);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE && value.length() > 0) {
            union.byteVal(hexToByte(value));
        } else if (dt == DataType.DT_BYTE && value.length() < 1) {
            union.byteVal((byte) 0);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            String[] str = value.split(" ");
            byte[] bytestr = new byte[str.length];
            for (int i = 0; i < str.length; i++) {
                bytestr[i] = hexToByte(str[i]);
            }
            union.bytestrVal(bytestr);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            String[] str = value.split(" ");
            T_COMPLEX complex = new T_COMPLEX();
            complex.r = Float.valueOf(str[0]);
            complex.i = Float.valueOf(str[1]);
            union.complexVal(complex);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE && value.length() > 0) {
            union.dateVal(value);
        } else if (dt == DataType.DT_DATE && value.length() < 1) {
            union.dateVal("");
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            String[] str = value.split(" ");
            T_DCOMPLEX complex = new T_DCOMPLEX();
            complex.r = Double.valueOf(str[0]);
            complex.i = Double.valueOf(str[1]);
            union.dcomplexVal(complex);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE && value.length() > 0) {
            union.doubleVal(Double.valueOf(value));
        } else if (dt == DataType.DT_DOUBLE && value.length() < 1) {
            union.doubleVal(0);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM && value.length() > 0) {
            union.enumVal(Integer.valueOf(value));
        } else if (dt == DataType.DT_ENUM && value.length() < 1) {
            union.enumVal(0);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE && value.length() > 0) {
            Pattern pattern = Pattern.compile("(.*?)\\[(.*?),(.*?)\\]");
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                T_ExternalReference extRef = new T_ExternalReference();
                extRef.description = matcher.group(1);
                extRef.mimeType = matcher.group(2);
                extRef.location = matcher.group(3);
                union.extRefVal(extRef);
            } else {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 1,
                                      "Wrong T_ExternalReference string representation: " + value);
            }
        } else if (dt == DataType.DT_EXTERNALREFERENCE && value.length() < 1) {
            T_ExternalReference extRef = new T_ExternalReference();
            extRef.description = "";
            extRef.mimeType = "";
            extRef.location = "";
            union.extRefVal(extRef);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT && value.length() > 0) {
            union.floatVal(Float.valueOf(value));
        } else if (dt == DataType.DT_FLOAT && value.length() < 1) {
            union.floatVal((float) 0);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG && value.length() > 0) {
            union.longVal(Integer.valueOf(value));
        } else if (dt == DataType.DT_LONG && value.length() < 1) {
            union.longVal(0);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG && value.length() > 0) {
            union.longlongVal(ODSHelper.asODSLongLong(Long.valueOf(value)));
        } else if (dt == DataType.DT_LONGLONG && value.length() < 1) {
            union.longlongVal(ODSHelper.asODSLongLong(Long.valueOf(0)));
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT && value.length() > 0) {
            union.shortVal(Short.valueOf(value));
        } else if (dt == DataType.DT_SHORT && value.length() < 1) {
            union.shortVal((short) 0);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING && value.length() > 0) {
            union.stringVal(value);
        } else if (dt == DataType.DT_STRING && value.length() < 1) {
            union.stringVal("");
        }
        // unknown datatype
        else {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 1, "Unsupported datatype: " + dt);
        }
        return union;
    }

    /**
     * Returns the string representation of an ASAM ODS value type enum.
     * 
     * @param e the value type
     * @return the string representation
     */
    public static String valueType2String(int e) {
        if (e == 0) {
            return "dt_boolean";
        } else if (e == 1) {
            return "dt_byte";
        } else if (e == 2) {
            return "dt_short";
        } else if (e == 3) {
            return "dt_long";
        } else if (e == 4) {
            return "dt_longlong";
        } else if (e == 5) {
            return "ieeefloat4";
        } else if (e == 6) {
            return "ieeefloat8";
        } else if (e == 7) {
            return "dt_short_beo";
        } else if (e == 8) {
            return "dt_long_beo";
        } else if (e == 9) {
            return "dt_longlong_beo";
        } else if (e == 10) {
            return "ieeefloat4_beo";
        } else if (e == 11) {
            return "ieeefloat8_beo";
        } else if (e == 12) {
            return "dt_string";
        } else if (e == 13) {
            return "dt_bytestr";
        } else if (e == 14) {
            return "dt_blob";
        } else if (e == 15) {
            return "dt_boolean_flags_beo";
        } else if (e == 16) {
            return "dt_byte_flags_beo";
        } else if (e == 17) {
            return "dt_string_flags_beo";
        } else if (e == 18) {
            return "dt_bytestr_beo";
        } else if (e == 19) {
            return "dt_sbyte";
        } else if (e == 20) {
            return "dt_sbyte_flags_beo";
        } else if (e == 21) {
            return "dt_ushort";
        } else if (e == 22) {
            return "dt_ushort_beo";
        } else if (e == 23) {
            return "dt_ulong";
        } else if (e == 24) {
            return "dt_ulong_beo";
        } else if (e == 25) {
            return "dt_string_utf8";
        } else if (e == 26) {
            return "dt_string_utf8_beo";
        } else if (e == 27) {
            return "dt_bit_int";
        } else if (e == 28) {
            return "dt_bit_int_beo";
        } else if (e == 29) {
            return "dt_bit_uint";
        } else if (e == 30) {
            return "dt_bit_uint_beo";
        } else if (e == 31) {
            return "dt_bit_float";
        } else if (e == 32) {
            return "dt_bit_float_beo";
        }
        throw new IllegalArgumentException("Unknown ODS value type enum: " + e);
    }

    /**
     * Returns the string representation of an <code>org.asam.ods.DataType</code> object.
     * 
     * @param dt the DataType
     * @return the string representation
     */
    public static String dataType2String(DataType dt) {
        if (dt == DataType.DT_BLOB) {
            return "DT_BLOB";
        } else if (dt == DataType.DT_BOOLEAN) {
            return "DT_BOOLEAN";
        } else if (dt == DataType.DT_BYTE) {
            return "DT_BYTE";
        } else if (dt == DataType.DT_BYTESTR) {
            return "DT_BYTESTR";
        } else if (dt == DataType.DT_COMPLEX) {
            return "DT_COMPLEX";
        } else if (dt == DataType.DT_DATE) {
            return "DT_DATE";
        } else if (dt == DataType.DT_DCOMPLEX) {
            return "DT_DCOMPLEX";
        } else if (dt == DataType.DT_DOUBLE) {
            return "DT_DOUBLE";
        } else if (dt == DataType.DT_ENUM) {
            return "DT_ENUM";
        } else if (dt == DataType.DT_EXTERNALREFERENCE) {
            return "DT_EXTERNALREFERENCE";
        } else if (dt == DataType.DT_FLOAT) {
            return "DT_FLOAT";
        } else if (dt == DataType.DT_ID) {
            return "DT_ID";
        } else if (dt == DataType.DT_LONG) {
            return "DT_LONG";
        } else if (dt == DataType.DT_LONGLONG) {
            return "DT_LONGLONG";
        } else if (dt == DataType.DT_SHORT) {
            return "DT_SHORT";
        } else if (dt == DataType.DT_STRING) {
            return "DT_STRING";
        } else if (dt == DataType.DT_UNKNOWN) {
            return "DT_UNKNOWN";
        } else if (dt == DataType.DS_BOOLEAN) {
            return "DS_BOOLEAN";
        } else if (dt == DataType.DS_BYTE) {
            return "DS_BYTE";
        } else if (dt == DataType.DS_BYTESTR) {
            return "DS_BYTESTR";
        } else if (dt == DataType.DS_COMPLEX) {
            return "DS_COMPLEX";
        } else if (dt == DataType.DS_DATE) {
            return "DS_DATE";
        } else if (dt == DataType.DS_DCOMPLEX) {
            return "DS_DCOMPLEX";
        } else if (dt == DataType.DS_DOUBLE) {
            return "DS_DOUBLE";
        } else if (dt == DataType.DS_ENUM) {
            return "DS_ENUM";
        } else if (dt == DataType.DS_EXTERNALREFERENCE) {
            return "DS_EXTERNALREFERENCE";
        } else if (dt == DataType.DS_FLOAT) {
            return "DS_FLOAT";
        } else if (dt == DataType.DS_ID) {
            return "DS_ID";
        } else if (dt == DataType.DS_LONG) {
            return "DS_LONG";
        } else if (dt == DataType.DS_LONGLONG) {
            return "DS_LONGLONG";
        } else if (dt == DataType.DS_SHORT) {
            return "DS_SHORT";
        } else if (dt == DataType.DS_STRING) {
            return "DS_STRING";
        }
        throw new IllegalArgumentException("Unknown ODS datatype: " + dt.value());
    }

    /**
     * Returns the <code>org.asam.ods.DataType</code> object from given string representation.
     * 
     * @param str the string to parse
     * @return the DataType object
     */
    public static DataType string2dataType(String str) {
        if (str == null || str.length() < 1) {
            throw new IllegalArgumentException("str must not be null or empty");
        }
        String s = str.trim().toUpperCase();
        if ("DT_BLOB".equals(s)) {
            return DataType.DT_BLOB;
        } else if ("DT_BOOLEAN".equals(s)) {
            return DataType.DT_BOOLEAN;
        } else if ("DT_BYTE".equals(s)) {
            return DataType.DT_BYTE;
        } else if ("DT_BYTESTR".equals(s)) {
            return DataType.DT_BYTESTR;
        } else if ("DT_COMPLEX".equals(s)) {
            return DataType.DT_COMPLEX;
        } else if ("DT_DATE".equals(s)) {
            return DataType.DT_DATE;
        } else if ("DT_DCOMPLEX".equals(s)) {
            return DataType.DT_DCOMPLEX;
        } else if ("DT_DOUBLE".equals(s)) {
            return DataType.DT_DOUBLE;
        } else if ("DT_ENUM".equals(s)) {
            return DataType.DT_ENUM;
        } else if ("DT_EXTERNALREFERENCE".equals(s)) {
            return DataType.DT_EXTERNALREFERENCE;
        } else if ("DT_FLOAT".equals(s)) {
            return DataType.DT_FLOAT;
        } else if ("DT_ID".equals(s)) {
            return DataType.DT_ID;
        } else if ("DT_LONG".equals(s)) {
            return DataType.DT_LONG;
        } else if ("DT_LONGLONG".equals(s)) {
            return DataType.DT_LONGLONG;
        } else if ("DT_SHORT".equals(s)) {
            return DataType.DT_SHORT;
        } else if ("DT_STRING".equals(s)) {
            return DataType.DT_STRING;
        } else if ("DT_UNKNOWN".equals(s)) {
            return DataType.DT_UNKNOWN;
        } else if ("DS_BOOLEAN".equals(s)) {
            return DataType.DS_BOOLEAN;
        } else if ("DS_BYTE".equals(s)) {
            return DataType.DS_BYTE;
        } else if ("DS_BYTESTR".equals(s)) {
            return DataType.DS_BYTESTR;
        } else if ("DS_COMPLEX".equals(s)) {
            return DataType.DS_COMPLEX;
        } else if ("DS_DATE".equals(s)) {
            return DataType.DS_DATE;
        } else if ("DS_DCOMPLEX".equals(s)) {
            return DataType.DS_DCOMPLEX;
        } else if ("DS_DOUBLE".equals(s)) {
            return DataType.DS_DOUBLE;
        } else if ("DS_ENUM".equals(s)) {
            return DataType.DS_ENUM;
        } else if ("DS_EXTERNALREFERENCE".equals(s)) {
            return DataType.DS_EXTERNALREFERENCE;
        } else if ("DS_FLOAT".equals(s)) {
            return DataType.DS_FLOAT;
        } else if ("DS_ID".equals(s)) {
            return DataType.DS_ID;
        } else if ("DS_LONG".equals(s)) {
            return DataType.DS_LONG;
        } else if ("DS_LONGLONG".equals(s)) {
            return DataType.DS_LONGLONG;
        } else if ("DS_SHORT".equals(s)) {
            return DataType.DS_SHORT;
        } else if ("DS_STRING".equals(s)) {
            return DataType.DS_STRING;
        }
        throw new IllegalArgumentException("Unknown ODS datatype: " + str);
    }

    /**
     * Returns the <code>org.asam.ods.DataType</code> for the corresponding enum value.
     * 
     * @param e the enum value
     * @return the DataType
     */
    public static DataType enum2dataType(int e) {
        if (e == 12) {
            return DataType.DT_BLOB;
        } else if (e == 4) {
            return DataType.DT_BOOLEAN;
        } else if (e == 5) {
            return DataType.DT_BYTE;
        } else if (e == 11) {
            return DataType.DT_BYTESTR;
        } else if (e == 13) {
            return DataType.DT_COMPLEX;
        } else if (e == 10) {
            return DataType.DT_DATE;
        } else if (e == 14) {
            return DataType.DT_DCOMPLEX;
        } else if (e == 7) {
            return DataType.DT_DOUBLE;
        } else if (e == 30) {
            return DataType.DT_ENUM;
        } else if (e == 28) {
            return DataType.DT_EXTERNALREFERENCE;
        } else if (e == 3) {
            return DataType.DT_FLOAT;
        } else if (e == 9) {
            return DataType.DT_ID;
        } else if (e == 6) {
            return DataType.DT_LONG;
        } else if (e == 8) {
            return DataType.DT_LONGLONG;
        } else if (e == 2) {
            return DataType.DT_SHORT;
        } else if (e == 1) {
            return DataType.DT_STRING;
        } else if (e == 0) {
            return DataType.DT_UNKNOWN;
        } else if (e == 18) {
            return DataType.DS_BOOLEAN;
        } else if (e == 19) {
            return DataType.DS_BYTE;
        } else if (e == 27) {
            return DataType.DS_BYTESTR;
        } else if (e == 23) {
            return DataType.DS_COMPLEX;
        } else if (e == 26) {
            return DataType.DS_DATE;
        } else if (e == 24) {
            return DataType.DS_DCOMPLEX;
        } else if (e == 21) {
            return DataType.DS_DOUBLE;
        } else if (e == 31) {
            return DataType.DS_ENUM;
        } else if (e == 29) {
            return DataType.DS_EXTERNALREFERENCE;
        } else if (e == 17) {
            return DataType.DS_FLOAT;
        } else if (e == 25) {
            return DataType.DS_ID;
        } else if (e == 20) {
            return DataType.DS_LONG;
        } else if (e == 22) {
            return DataType.DS_LONGLONG;
        } else if (e == 16) {
            return DataType.DS_SHORT;
        } else if (e == 15) {
            return DataType.DS_STRING;
        }
        throw new IllegalArgumentException("Unknown ODS datatype: " + e);
    }

    /**
     * Returns the string representation of an relation range short value.
     * 
     * @param relRange The relation range value.
     * @return The string value.
     */
    public static String relRange2string(short relRange) {
        if (relRange == 0) {
            return "0";
        } else if (relRange == 1) {
            return "1";
        } else if (relRange == -1) {
            return "Many";
        }
        throw new IllegalArgumentException("Unknown relation range: " + relRange);
    }

    /**
     * Returns the relation range short value for given string.
     * 
     * @param str The string value.
     * @return The relation range value.
     */
    public static short string2relRange(String str) {
        if (str.equals("0")) {
            return (short) 0;
        } else if (str.equals("1")) {
            return (short) 1;
        } else if (str.equals("Many")) {
            return (short) -1;
        }
        // workaround for wrong DIAdem ATFX files
        else if (str.equals("100")) {
            return (short) 1;
        }
        throw new IllegalArgumentException("Unknown relation range: " + str);
    }

    /**
     * Returns the string representation of given <code>org.asam.ods.Relationship</code> enum.
     * 
     * @param relationShip The relation ship.
     * @return The string.
     */
    public static String relationship2string(Relationship relationShip) {
        if (relationShip == Relationship.ALL_REL) {
            return "ALL_REL";
        } else if (relationShip == Relationship.CHILD) {
            return "CHILD";
        } else if (relationShip == Relationship.FATHER) {
            return "FATHER";
        } else if (relationShip == Relationship.INFO_FROM) {
            return "INFO_FROM";
        } else if (relationShip == Relationship.INFO_REL) {
            return "INFO_REL";
        } else if (relationShip == Relationship.INFO_TO) {
            return "INFO_TO";
        } else if (relationShip == Relationship.SUBTYPE) {
            return "SUBTYPE";
        } else if (relationShip == Relationship.SUPERTYPE) {
            return "SUPERTYPE";
        }
        throw new IllegalArgumentException("Unknown Relationship: " + relationShip);
    }

    /**
     * Returns the <code>org.asam.ods.Relationship</code> enum from given string.
     * 
     * @param str The string.
     * @return The relation ship.
     */
    public static Relationship string2relationship(String str) {
        if (str.equals("ALL_REL")) {
            return Relationship.ALL_REL;
        } else if (str.equals("CHILD")) {
            return Relationship.CHILD;
        } else if (str.equals("FATHER")) {
            return Relationship.FATHER;
        } else if (str.equals("INFO_FROM")) {
            return Relationship.INFO_FROM;
        } else if (str.equals("INFO_REL")) {
            return Relationship.INFO_REL;
        } else if (str.equals("INFO_TO")) {
            return Relationship.INFO_TO;
        } else if (str.equals("SUBTYPE")) {
            return Relationship.SUBTYPE;
        } else if (str.equals("SUPERTYPE")) {
            return Relationship.SUPERTYPE;
        }
        throw new IllegalArgumentException("Unknown Relationship: " + str);
    }

    /**
     * Returns the string representation of given <code>org.asam.ods.RelationType</code> enum.
     * 
     * @param relationType The relation type.
     * @return The string.
     */
    public static String relationType2string(RelationType relationType) {
        if (relationType == RelationType.FATHER_CHILD) {
            return "FATHER_CHILD";
        } else if (relationType == RelationType.INFO) {
            return "INFO";
        } else if (relationType == RelationType.INHERITANCE) {
            return "INHERITANCE";
        }
        throw new IllegalArgumentException("Unknown RelationType: " + relationType);
    }

    /**
     * Returns the <code>org.asam.ods.RelationType</code> enum from given string.
     * 
     * @param str The string.
     * @return The relation type.
     */
    public static RelationType string2relationType(String str) {
        if (str.equals("FATHER_CHILD")) {
            return RelationType.FATHER_CHILD;
        } else if (str.equals("INFO")) {
            return RelationType.INFO;
        } else if (str.equals("INHERITANCE")) {
            return RelationType.INHERITANCE;
        }
        throw new IllegalArgumentException("Unknown RelationType: " + str);
    }

    public static String anuSeq2string(SelAIDNameUnitId[] nvu) throws AoException {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < nvu.length; i++) {
            builder.append(selAIDNameUnitId2string(nvu[i]));
        }
        builder.append("]");
        return builder.toString();
    }

    public static String selAIDNameUnitId2string(SelAIDNameUnitId sanui) throws AoException {
        StringBuilder builder = new StringBuilder();
        builder.append("SelAIDNameUnitId [aid=");
        builder.append(ODSHelper.asJLong(sanui.attr.aid));
        builder.append(",aaName=");
        builder.append(sanui.attr.aaName);
        builder.append(",unitId=");
        builder.append(ODSHelper.asJLong(sanui.unitId));
        builder.append(",aggregate=");
        builder.append(aggrFunc2String(sanui.aggregate));
        builder.append("]");
        return builder.toString();
    }

    public static String aidName2String(AIDName aidName) {
        StringBuilder builder = new StringBuilder();
        builder.append("AIDName[aid=");
        builder.append(ODSHelper.asJLong(aidName.aid));
        builder.append(",aaName=");
        builder.append(aidName.aaName);
        builder.append("]");
        return builder.toString();
    }

    public static String aggrFunc2String(AggrFunc aggrFunc) {
        if (aggrFunc == AggrFunc.AVG) {
            return "AVG";
        } else if (aggrFunc == AggrFunc.COUNT) {
            return "COUNT";
        } else if (aggrFunc == AggrFunc.DCOUNT) {
            return "DCOUNT";
        } else if (aggrFunc == AggrFunc.DISTINCT) {
            return "DISTINCT";
        } else if (aggrFunc == AggrFunc.MAX) {
            return "MAX";
        } else if (aggrFunc == AggrFunc.MIN) {
            return "MIN";
        } else if (aggrFunc == AggrFunc.NONE) {
            return "NONE";
        } else if (aggrFunc == AggrFunc.POINT) {
            return "POINT";
        } else if (aggrFunc == AggrFunc.STDDEV) {
            return "STDDEV";
        } else if (aggrFunc == AggrFunc.SUM) {
            return "SUM";
        }
        return "";
    }

    /**
     * Converts a byte to a hexadecimal string.
     * 
     * @param b The byte value.
     * @return The hexadecimal string.
     */
    public static String byteToHex(byte b) {
        String str = "";
        if (((int) b & 0xff) < 0x10) {
            str += "0";
        }
        str += Long.toString((int) b & 0xff, 16);
        return str.toUpperCase();
    }

    /**
     * Converts a hexadecimal string to a byte.
     * 
     * @param s The hexadecimal string.
     * @return The byte.
     */
    public static byte hexToByte(String s) {
        return (byte) ((Character.digit(s.charAt(0), 16) << 4) + Character.digit(s.charAt(1), 16));
    }

    /**
     * Converts an array of TS_Value to a single TS_ValueSeq. All elements in the array must not be null and have the
     * same DataType.
     * 
     * @param tsValues the array of TS_Value
     * @param dt The data type.
     * @return the created TS_ValueSeq
     * @throws AoException if one or more of the array elements are null or one or more of the elements have a different
     *             DataType than the rest.
     */
    public static TS_ValueSeq tsValue2tsValueSeq(TS_Value[] tsValues, DataType dt) throws AoException {
        for (int row = 0; row < tsValues.length; row++) {
            if (tsValues[row] == null) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Null reference found on index '" + row + "'");
            }
            if (dt != tsValues[row].u.discriminator()) {
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                      "Found more than one DataType for result column '"
                                              + ODSHelper.tsValue2string(tsValues[row]) + "'");
            }
        }

        TS_UnionSeq seq = new TS_UnionSeq();

        if (dt == DataType.DS_BOOLEAN) {
            boolean[][] val = new boolean[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.booleanSeq();
            }
            seq.booleanSeq(val);
        }
        // DS_BYTE
        else if (dt == DataType.DS_BYTE) {
            byte[][] val = new byte[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.byteSeq();
            }
            seq.byteSeq(val);
        }
        // DS_BYTESTR
        else if (dt == DataType.DS_BYTESTR) {
            byte[][][] val = new byte[tsValues.length][][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.bytestrSeq();
            }
            seq.bytestrSeq(val);
        }
        // DS_COMPLEX
        else if (dt == DataType.DS_COMPLEX) {
            T_COMPLEX[][] val = new T_COMPLEX[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.complexSeq();
            }
            seq.complexSeq(val);
        }
        // DS_DATE
        else if (dt == DataType.DS_DATE) {
            String[][] val = new String[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.dateSeq();
            }
            seq.dateSeq(val);
        }
        // DS_DCOMPLEX
        else if (dt == DataType.DS_DCOMPLEX) {
            T_DCOMPLEX[][] val = new T_DCOMPLEX[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.dcomplexSeq();
            }
            seq.dcomplexSeq(val);
        }
        // DS_DOUBLE
        else if (dt == DataType.DS_DOUBLE) {
            double[][] val = new double[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.doubleSeq();
            }
            seq.doubleSeq(val);
        }
        // DS_ENUM
        else if (dt == DataType.DS_ENUM) {
            int[][] val = new int[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.enumSeq();
            }
            seq.enumSeq(val);
        }
        // DS_EXTERNALREFERENCE
        else if (dt == DataType.DS_EXTERNALREFERENCE) {
            T_ExternalReference[][] val = new T_ExternalReference[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.extRefSeq();
            }
            seq.extRefSeq(val);
        }
        // DS_FLOAT
        else if (dt == DataType.DS_FLOAT) {
            float[][] val = new float[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.floatSeq();
            }
            seq.floatSeq(val);
        }
        // DS_LONG
        else if (dt == DataType.DS_LONG) {
            int[][] val = new int[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.longSeq();
            }
            seq.longSeq(val);
        }
        // DS_LONGLONG
        else if (dt == DataType.DS_LONGLONG) {
            T_LONGLONG[][] val = new T_LONGLONG[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.longlongSeq();
            }
            seq.longlongSeq(val);
        }
        // DS_SHORT
        else if (dt == DataType.DS_SHORT) {
            short[][] val = new short[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.shortSeq();
            }
            seq.shortSeq(val);
        }
        // DS_STRING
        else if (dt == DataType.DS_STRING) {
            String[][] val = new String[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.stringSeq();
            }
            seq.stringSeq(val);
        }
        // DT_BLOB
        else if (dt == DataType.DT_BLOB) {
            Blob[] val = new Blob[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.blobVal();
            }
            seq.blobVal(val);
        }
        // DT_BOOLEAN
        else if (dt == DataType.DT_BOOLEAN) {
            boolean[] val = new boolean[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.booleanVal();
            }
            seq.booleanVal(val);
        }
        // DT_BYTE
        else if (dt == DataType.DT_BYTE) {
            byte[] val = new byte[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.byteVal();
            }
            seq.byteVal(val);
        }
        // DT_BYTESTR
        else if (dt == DataType.DT_BYTESTR) {
            byte[][] val = new byte[tsValues.length][];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.bytestrVal();
            }
            seq.bytestrVal(val);
        }
        // DT_COMPLEX
        else if (dt == DataType.DT_COMPLEX) {
            T_COMPLEX[] val = new T_COMPLEX[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.complexVal();
            }
            seq.complexVal(val);
        }
        // DT_DATE
        else if (dt == DataType.DT_DATE) {
            String[] val = new String[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.dateVal();
            }
            seq.dateVal(val);
        }
        // DT_DCOMPLEX
        else if (dt == DataType.DT_DCOMPLEX) {
            T_DCOMPLEX[] val = new T_DCOMPLEX[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.dcomplexVal();
            }
            seq.dcomplexVal(val);
        }
        // DT_DOUBLE
        else if (dt == DataType.DT_DOUBLE) {
            double[] val = new double[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.doubleVal();
            }
            seq.doubleVal(val);
        }
        // DT_ENUM
        else if (dt == DataType.DT_ENUM) {
            int[] val = new int[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.enumVal();
            }
            seq.enumVal(val);
        }
        // DT_EXTERNALREFERENCE
        else if (dt == DataType.DT_EXTERNALREFERENCE) {
            T_ExternalReference[] val = new T_ExternalReference[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.extRefVal();
            }
            seq.extRefVal(val);
        }
        // DT_FLOAT
        else if (dt == DataType.DT_FLOAT) {
            float[] val = new float[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.floatVal();
            }
            seq.floatVal(val);
        }
        // DT_LONG
        else if (dt == DataType.DT_LONG) {
            int[] val = new int[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.longVal();
            }
            seq.longVal(val);
        }
        // DT_LONGLONG
        else if (dt == DataType.DT_LONGLONG) {
            T_LONGLONG[] val = new T_LONGLONG[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.longlongVal();
            }
            seq.longlongVal(val);
        }
        // DT_SHORT
        else if (dt == DataType.DT_SHORT) {
            short[] val = new short[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.shortVal();
            }
            seq.shortVal(val);
        }
        // DT_STRING
        else if (dt == DataType.DT_STRING) {
            String[] val = new String[tsValues.length];
            for (int i = 0; i < val.length; i++) {
                val[i] = tsValues[i].u.stringVal();
            }
            seq.stringVal(val);
        }
        // Cannot process given DataType
        else {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Unsupported DataType '" + ODSHelper.dataType2String(dt) + "'");
        }

        short[] flags = new short[ODSHelper.tsUnionSeqLength(seq)];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = tsValues[i].flag;
        }

        return new TS_ValueSeq(seq, flags);
    }

    public static TS_Value convertTsValue(TS_Value source, DataType targetDt) throws AoException {
        DataType sourceDt = source.u.discriminator();
        if (sourceDt == targetDt) {
            return source;
        }

        TS_Value value = new TS_Value();
        value.flag = source.flag;
        value.u = new TS_Union();
        if ((sourceDt == DataType.DS_LONG) && (targetDt == DataType.DS_DOUBLE)) {
            double[] ar = new double[source.u.longSeq().length];
            for (int i = 0; i < ar.length; i++) {
                ar[i] = source.u.longSeq()[i];
            }
            value.u.doubleSeq(ar);
        } else {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Unable to convert value from datatype '" + ODSHelper.dataType2String(sourceDt)
                                          + "' to datatype '" + ODSHelper.dataType2String(targetDt) + "'");
        }

        return value;
    }

    public static int seqRepComp2seqRepExtComp(int seqRep) {
        // explicit => external_component
        if (seqRep == 0) {
            return 7;
        }
        // raw_linear => raw_linear_external
        else if (seqRep == 4) {
            return 8;
        }
        // raw_polynomial => raw_polynomial_external
        else if (seqRep == 5) {
            return 9;
        }
        // raw_linear_calibrated => raw_linear_calibrated_external
        else if (seqRep == 10) {
            return 11;
        }
        return seqRep;
    }

    public static int seqRepExtComp2seqRepComp(int seqRep) {
        // external_component => explicit
        if (seqRep == 7) {
            seqRep = 0;
        }
        // raw_linear_external => raw_linear
        else if (seqRep == 8) {
            seqRep = 4;
        }
        // raw_polynomial_external => raw_polynomial
        else if (seqRep == 9) {
            seqRep = 5;
        }
        // raw_linear_calibrated_external => raw_linear_calibrated
        else if (seqRep == 11) {
            seqRep = 10;
        }

        return seqRep;
    }

    public static void setBit(byte[] data, int pos, boolean val) {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte oldByte = data[posByte];
        if (val) {
            data[posByte] = (byte) (oldByte | (1 << 7 - posBit));
        } else {
            data[posByte] = (byte) (oldByte | (0 << 7 - posBit));
        }
    }

}
