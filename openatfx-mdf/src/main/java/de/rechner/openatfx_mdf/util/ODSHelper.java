package de.rechner.openatfx_mdf.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.asam.ods.NameValue;
import org.asam.ods.NameValueUnit;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;


/**
 * Helper class with ODS specific functions.
 * 
 * @author Christian Rechner
 */
public abstract class ODSHelper {

    // prepare dateformats to avoid instantiation a single object every time parsing a date.
    private static Map<Integer, DateFormat> ODS_DATEFORMATS = new HashMap<Integer, DateFormat>();
    static {
        ODS_DATEFORMATS.put(4, new SimpleDateFormat("yyyy"));
        ODS_DATEFORMATS.put(6, new SimpleDateFormat("yyyyMM"));
        ODS_DATEFORMATS.put(8, new SimpleDateFormat("yyyyMMdd"));
        ODS_DATEFORMATS.put(10, new SimpleDateFormat("yyyyMMddHH"));
        ODS_DATEFORMATS.put(12, new SimpleDateFormat("yyyyMMddHHmm"));
        ODS_DATEFORMATS.put(14, new SimpleDateFormat("yyyyMMddHHmmss"));
        ODS_DATEFORMATS.put(17, new SimpleDateFormat("yyyyMMddHHmmssSSS"));
    }

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
