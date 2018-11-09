package de.rechner.openatfx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.BufferedRandomAccessFile;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class for reading values from external component files.
 * 
 * @author Christian Rechner
 */
class ExtCompReader {

    private static final Log LOG = LogFactory.getLog(ExtCompReader.class);
    private static final int BUFFER_SIZE = 32768;

    /** The singleton instance */
    private static volatile ExtCompReader instance;

    public TS_Value readValues(AtfxCache atfxCache, long iidLc, DataType targetDataType) throws AoException {
        // read external component instances
        long aidLc = atfxCache.getAidsByBaseType("aolocalcolumn").iterator().next();
        ApplicationRelation relExtComps = atfxCache.getApplicationRelationByBaseName(aidLc, "external_component");
        List<Long> iidExtComps = atfxCache.getRelatedInstanceIds(aidLc, iidLc, relExtComps);
        if (iidExtComps.size() > 0) {
            Collections.sort(iidExtComps, new ExternalComponentComparator(atfxCache));
        }

        TS_Value tsValue = new TS_Value();
        tsValue.flag = (short) 15;
        tsValue.u = new TS_Union();

        // get raw data type
        DataType rawDataType = targetDataType;
        Integer attrNo = atfxCache.getAttrNoByBaName(aidLc, "raw_datatype");
        if (attrNo != null) {
            TS_Value valRawDatatype = atfxCache.getInstanceValue(aidLc, attrNo, iidLc);
            if (valRawDatatype != null && valRawDatatype.flag == 15) {
                int val = valRawDatatype.u.enumVal();
                if (val == 1) { // DT_STRING
                    rawDataType = DataType.DS_STRING;
                } else if (val == 2) { // DT_SHORT
                    rawDataType = DataType.DS_SHORT;
                } else if (val == 3) { // DT_FLOAT
                    rawDataType = DataType.DS_FLOAT;
                } else if (val == 4) { // DT_BOOLEAN
                    rawDataType = DataType.DS_BOOLEAN;
                } else if (val == 5) { // DT_BYTE
                    rawDataType = DataType.DS_BYTE;
                } else if (val == 6) { // DT_LONG
                    rawDataType = DataType.DS_LONG;
                } else if (val == 7) { // DT_DOUBLE
                    rawDataType = DataType.DS_DOUBLE;
                } else if (val == 8) { // DT_LONGLONG
                    rawDataType = DataType.DS_LONGLONG;
                } else if (val == 10) { // DT_DATE
                    rawDataType = DataType.DS_DATE;
                } else if (val == 11) { // DT_BYTESTR
                    rawDataType = DataType.DS_BYTESTR;
                } else if (val == 13) { // DT_COMPLEX
                    rawDataType = DataType.DS_COMPLEX;
                } else if (val == 14) { // DT_DCOMPLEX
                    rawDataType = DataType.DS_DCOMPLEX;
                } else if (val == 28) { // DT_EXTERNALREFERENCE
                    rawDataType = DataType.DS_EXTERNALREFERENCE;
                } else if (val == 30) { // DT_ENUM
                    rawDataType = DataType.DS_ENUM;
                }
            }
        }

        // DS_STRING
        if (rawDataType == DataType.DS_STRING || rawDataType == DataType.DS_BYTESTR) {
            List<String> list = new ArrayList<String>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readStringValues(atfxCache, iidExtComp));
            }
            tsValue.u.stringSeq(list.toArray(new String[0]));
        } // DS_DATE
        else if (rawDataType == DataType.DS_DATE) {
            List<String> list = new ArrayList<String>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readStringValues(atfxCache, iidExtComp));
            }
            tsValue.u.dateSeq(list.toArray(new String[0]));
        }
        // DS_NUMBER
        else {
            List<Number> list = new ArrayList<Number>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readNumberValues(atfxCache, iidExtComp, rawDataType));
            }
            // DS_BOOLEAN
            if (rawDataType == DataType.DS_BOOLEAN) {
                boolean[] ar = new boolean[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).byteValue() != 0;
                }
                tsValue.u.booleanSeq(ar);
            }
            // DS_BYTE
            else if (rawDataType == DataType.DS_BYTE) {
                byte[] ar = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).byteValue();
                }
                tsValue.u.byteSeq(ar);
            }
            // DS_SHORT
            else if (rawDataType == DataType.DS_SHORT) {
                short[] ar = new short[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).shortValue();
                }
                tsValue.u.shortSeq(ar);
            }
            // DS_LONG
            else if (rawDataType == DataType.DS_LONG) {
                int[] ar = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).intValue();
                }
                tsValue.u.longSeq(ar);
            }
            // DS_DOUBLE
            else if (rawDataType == DataType.DS_DOUBLE) {
                double[] ar = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).doubleValue();
                }
                tsValue.u.doubleSeq(ar);
            }
            // DS_LONGLONG
            else if (rawDataType == DataType.DS_LONGLONG) {
                T_LONGLONG[] ar = new T_LONGLONG[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = ODSHelper.asODSLongLong(list.get(i).longValue());
                }
                tsValue.u.longlongSeq(ar);
            }
            // DS_FLOAT
            else if (rawDataType == DataType.DS_FLOAT) {
                float[] ar = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).floatValue();
                }
                tsValue.u.floatSeq(ar);
            }
            // DS_COMPLEX
            else if (rawDataType == DataType.DS_COMPLEX) {
                int size = list.size() / 2;
                T_COMPLEX[] ar = new T_COMPLEX[size];
                for (int i = 0; i < size; i++) {
                    ar[i] = new T_COMPLEX();
                    ar[i].r = list.get(i * 2).floatValue();
                    ar[i].i = list.get(i * 2 + 1).floatValue();
                }
                tsValue.u.complexSeq(ar);
            }
            // DS_DCOMPLEX
            else if (rawDataType == DataType.DS_DCOMPLEX) {
                int size = list.size() / 2;
                T_DCOMPLEX[] ar = new T_DCOMPLEX[size];
                for (int i = 0; i < size; i++) {
                    ar[i] = new T_DCOMPLEX();
                    ar[i].r = list.get(i * 2).doubleValue();
                    ar[i].i = list.get(i * 2 + 1).doubleValue();
                }
                tsValue.u.dcomplexSeq(ar);
            }
            // unsupported
            else {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                      "Reading values from external component not yet supported for datatype: "
                                              + ODSHelper.dataType2String(targetDataType));
            }
        }
        return tsValue;
    }

    private List<Number> readNumberValues(AtfxCache atfxCache, long iidExtComp, DataType rawDataType)
            throws AoException {
        long start = System.currentTimeMillis();

        List<Number> list = new ArrayList<Number>();
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
        String filenameUrl = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.stringVal();
        File fileRoot = new File(atfxCache.getContext().get("FILE_ROOT").value.u.stringVal());
        File extCompFile = new File(fileRoot, filenameUrl);

        // get value type
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
        int valueType = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.enumVal();

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        int startOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            startOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            startOffset = (int) ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // read value offset
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_offset");
        int valueOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // read bit count
        short bitCount = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "ao_bit_count");
        if (attrNo != null) {
            bitCount = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.shortVal();
        }

        // read bit offset
        short bitOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "ao_bit_offset");
        if (attrNo != null) {
            bitOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.shortVal();
        }

        // block_size, valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "block_size");
        int blockSize = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
        int valuesperblock = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // read values
        RandomAccessFile raf = null;
        try {
            // open source file
            raf = new BufferedRandomAccessFile(extCompFile, "r", BUFFER_SIZE);
            raf.seek(startOffset);

            // initialize buffer
            ByteBuffer sourceMbb = ByteBuffer.allocate(blockSize);
            ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
            // dt_short_beo [7], dt_long_beo [8], dt_longlong_beo [9], ieeefloat4_beo [10], ieeefloat8_beo [11],
            // dt_boolean_flags_beo [15], dt_byte_flags_beo [16], dt_string_flags_beo [17], dt_bytestr_beo [18],
            // dt_sbyte_flags_beo [20], dt_ushort_beo [22], dt_ulong_beo [24], dt_string_utf8_beo [26]
            // dt_bit_int_beo [28], dt_bit_uint_beo [30], dt_bit_float_beo [32]
            if ((valueType == 7) || (valueType == 8) || (valueType == 9) || (valueType == 10) || (valueType == 11)
                    || (valueType == 15) || (valueType == 16) || (valueType == 17) || (valueType == 18)
                    || (valueType == 20) || (valueType == 22) || (valueType == 24) || (valueType == 26)
                    || (valueType == 28) || (valueType == 30) || (valueType == 32)) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            }
            sourceMbb.order(byteOrder);

            // loop over blocks
            for (int i = 0; i < componentLength; i += valuesperblock) {

                // read whole block into memory
                byte[] record = new byte[blockSize];
                raf.read(record, 0, record.length);

                // make buildable with both java8 and java9
                Buffer.class.cast(sourceMbb).clear();
                sourceMbb.put(record);
                sourceMbb.position(valueOffset);

                // sub blocks are consecutive, puhh!
                for (int j = 0; j < valuesperblock; j++) {

                    // 1=dt_byte
                    if (valueType == 1) {
                        // int b = sourceMbb.get() & 0xFF;
                        list.add(sourceMbb.get());
                    }
                    // 19=dt_sbyte
                    else if (valueType == 19) {
                        // short b = (short) (sourceMbb.get() & 0xFF);
                        list.add(sourceMbb.get());
                    }
                    // 2=dt_short, 7=dt_short_beo
                    else if ((valueType == 2) || (valueType == 7)) {
                        list.add(sourceMbb.getShort());
                    }
                    // 3=dt_long, 8=dt_long_beo
                    else if ((valueType == 3) || (valueType == 8)) {
                        list.add(sourceMbb.getInt());
                    }
                    // 4=dt_longlong, 9=dt_longlong_beo
                    else if ((valueType == 4) || (valueType == 9)) {
                        list.add(sourceMbb.getLong());
                    }
                    // 5=ieeefloat4, 10=ieeefloat4_beo
                    else if ((valueType == 5) || (valueType == 10)) {
                        list.add(sourceMbb.getFloat());
                    }
                    // 6=ieeefloat8, 11=ieeefloat8_beo
                    else if ((valueType == 6) || (valueType == 11)) {
                        list.add(sourceMbb.getDouble());
                    }
                    // 21=dt_ushort, 22=dt_ushort_beo
                    else if ((valueType == 21) || (valueType == 22)) {
                        int val = sourceMbb.getShort() & 0xffff;
                        list.add(val);
                    }
                    // 23=dt_ulong, 24=dt_ulong_beo
                    else if ((valueType == 23) || (valueType == 24)) {
                        long val = sourceMbb.getInt() & 0xffffffffL;
                        list.add(val);
                    }
                    // 29=dt_bit_uint, 30=dt_bit_uint_beo
                    else if ((valueType == 29) || (valueType == 30)) {

                        // read that number of bytes from the byte position within the file
                        int bytesToRead = ((bitCount + bitOffset - 1) / 8) + 1;
                        byte[] tmp = new byte[bytesToRead];
                        sourceMbb.get(tmp);

                        // put the byte into an integer to enable bit shifting
                        if (tmp.length <= 4) {
                            ByteBuffer bb = ByteBuffer.allocate(4);
                            bb.order(byteOrder);
                            bb.put(tmp);
                            Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                            int intValue = bb.getInt();
                            intValue = intValue >> bitOffset; // shift right bit offset
                            int mask = 0xFFFFFFFF >>> (32 - bitCount); // mask out unnecessary bits
                            intValue = intValue & mask;
                            list.add(intValue);
                        }
                        // put the byte into a long to enable bit shifting
                        else {
                            ByteBuffer bb = ByteBuffer.allocate(8);
                            bb.order(byteOrder);
                            bb.put(tmp);
                            Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                            long longValue = bb.getLong();
                            longValue = longValue >> bitOffset; // shift right bit offset
                            long mask = 0xFFFFFFFFFFFFFFFFl >>> (64 - bitCount); // mask out unnecessary bits
                            longValue = longValue & mask;
                            list.add(longValue);
                        }
                    }
                    // unsupported data type
                    else {
                        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                              "Unsupported 'value_type': " + ODSHelper.valueType2String(valueType));
                    }
                }
            }

            LOG.info("Read " + list.size() + " numeric values from component file '" + filenameUrl + "' in "
                    + (System.currentTimeMillis() - start) + "ms [value_type=" + ODSHelper.valueType2String(valueType)
                    + "]");
            return list;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ioe) {
                    LOG.error(ioe.getMessage(), ioe);
                }
                raf = null;
            }
        }
    }

    private Collection<String> readStringValues(AtfxCache atfxCache, long iidExtComp) throws AoException {
        long start = System.currentTimeMillis();
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        int attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
        String filenameUrl = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.stringVal();
        File fileRoot = new File(atfxCache.getContext().get("FILE_ROOT").value.u.stringVal());
        File extCompFile = new File(fileRoot, filenameUrl);

        // get datatype
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
        int valueType = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.enumVal();
        if (valueType != 12) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Unsupported 'value_type' for data type DT_STRING or DT_DATE: " + valueType);
        }

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        long startOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            startOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            startOffset = ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // value_offset is irrelevant according ODS Standard 3.42, page 3-51

        // read values
        RandomAccessFile raf = null;
        byte[] backingBuffer = new byte[componentLength];
        List<String> list = new ArrayList<String>();
        try {
            // open source channel
            raf = new BufferedRandomAccessFile(extCompFile, "r", BUFFER_SIZE);
            raf.seek(startOffset);
            raf.read(backingBuffer, 0, backingBuffer.length);

            int startPosition = 0;
            for (int position = 0; position < componentLength; position++) {
                if (backingBuffer[position] == 0) {
                    list.add(new String(backingBuffer, startPosition, position - startPosition, "ISO-8859-1"));
                    startPosition = position + 1;
                }
            }

            LOG.info("Read " + list.size() + " string values from component file '" + filenameUrl + "' in "
                    + (System.currentTimeMillis() - start) + "ms [value_type=" + ODSHelper.valueType2String(valueType)
                    + "]");
            return list;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
                raf = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public TS_Value readFlags(AtfxCache atfxCache, long iidLc) throws AoException {
        long start = System.currentTimeMillis();

        // read external component instances
        long aidLc = atfxCache.getAidsByBaseType("aolocalcolumn").iterator().next();
        ApplicationRelation relExtComps = atfxCache.getApplicationRelationByBaseName(aidLc, "external_component");
        Collection<Long> iidExtComps = atfxCache.getRelatedInstanceIds(aidLc, iidLc, relExtComps);

        if (iidExtComps.size() < 1) {
            return null;
        } else if (iidExtComps.size() != 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "The implementation currently only may read exactly one external component file");
        }
        long iidExtComp = iidExtComps.iterator().next();
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "flags_filename_url");
        if (attrNo == null) {
            return null;
        }
        TS_Value v = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp);
        if ((v == null) || (v.flag != 15) || (v.u.stringVal() == null) || (v.u.stringVal().length() < 1)) {
            return null;
        }
        String flagsFilenameUrl = v.u.stringVal();
        File fileRoot = new File(atfxCache.getContext().get("FILE_ROOT").value.u.stringVal());
        File flagsFile = new File(fileRoot, flagsFilenameUrl);

        // read start offset, may be DT_LONG or DT_LONGLONG
        int flagsStartOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "flags_start_offset");
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Application attribute derived from base attribute 'flags_start_offset' not found");
        }
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            flagsStartOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            flagsStartOffset = (int) ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.longVal();

        // read values
        TS_Value tsValue = new TS_Value();
        tsValue.flag = (short) 15;
        tsValue.u = new TS_Union();
        tsValue.u.shortSeq(new short[componentLength]);

        RandomAccessFile raf = null;
        byte[] backingBuffer = new byte[2];
        try {
            // open source channel
            raf = new BufferedRandomAccessFile(flagsFile, "r", BUFFER_SIZE);
            raf.seek(flagsStartOffset);

            for (int i = 0; i < componentLength; i++) {
                raf.read(backingBuffer, 0, backingBuffer.length);
                ByteBuffer sourceMbb = ByteBuffer.wrap(backingBuffer);
                sourceMbb.order(ByteOrder.LITTLE_ENDIAN);
                tsValue.u.shortSeq()[i] = sourceMbb.getShort();
            }

            LOG.info("Read " + componentLength + " flags from component file '" + flagsFilenameUrl + "' in "
                    + (System.currentTimeMillis() - start) + "ms");
            return tsValue;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            backingBuffer = null;
            try {
                if (raf != null) {
                    raf.close();
                }
                raf = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static ExtCompReader getInstance() {
        if (instance == null) {
            instance = new ExtCompReader();
        }
        return instance;
    }

    /**
     * Custom comparator so sort multiple instances of 'AoExternalComponent' for the same 'AoLocalColumn' instance by
     * the value of the base attribute 'ordinal_number'.
     */
    private static class ExternalComponentComparator implements Comparator<Long> {

        private final AtfxCache atfxCache;

        public ExternalComponentComparator(AtfxCache atfxCache) {
            this.atfxCache = atfxCache;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Long iidExtComp1, Long iidExtComp2) {
            try {
                Integer ordExtComp1 = 0;
                Integer ordExtComp2 = 0;

                long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();
                Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "ordinal_number");
                if (attrNo == null) {
                    LOG.warn("Application attribute derived from 'ordinal_number' not found!");
                    return 0;
                }

                ordExtComp1 = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp1).u.longVal();
                ordExtComp2 = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp2).u.longVal();

                return ordExtComp1.compareTo(ordExtComp2);
            } catch (AoException e) {
                LOG.warn(e.reason, e);
                return 0;
            }
        }
    }

}
