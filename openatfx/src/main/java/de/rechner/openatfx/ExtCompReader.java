package de.rechner.openatfx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collection;
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

import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class for reading values from external component files.
 * 
 * @author Christian Rechner
 */
class ExtCompReader {

    private static final Log LOG = LogFactory.getLog(ExtCompReader.class);

    /** The singleton instance */
    private static volatile ExtCompReader instance;

    public TS_Value readValues(AtfxCache atfxCache, long iidLc, DataType targetDataType) throws AoException {
        // read external component instances
        long aidLc = atfxCache.getAidsByBaseType("aolocalcolumn").iterator().next();
        ApplicationRelation relExtComps = atfxCache.getApplicationRelationByBaseName(aidLc, "external_component");
        Collection<Long> iidExtComps = atfxCache.getRelatedInstanceIds(aidLc, iidLc, relExtComps);
        if (iidExtComps.size() != 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "The implementation currently only may read exactly one external component file");
        }

        TS_Value tsValue = new TS_Value();
        tsValue.flag = (short) 15;
        tsValue.u = new TS_Union();

        // DS_BYTE
        if (targetDataType == DataType.DS_BYTE) {
            List<Byte> list = new ArrayList<Byte>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readByteValues(atfxCache, iidExtComp));
            }
            byte[] ar = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.byteSeq(ar);
        }

        // DS_SHORT
        else if (targetDataType == DataType.DS_SHORT) {
            List<Short> list = new ArrayList<Short>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.shortValue());
                }
            }
            short[] ar = new short[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.shortSeq(ar);
        }
        // DS_LONG
        else if (targetDataType == DataType.DS_LONG) {
            List<Integer> list = new ArrayList<Integer>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.intValue());
                }
            }
            int[] ar = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.longSeq(ar);
        }
        // DS_DOUBLE
        else if (targetDataType == DataType.DS_DOUBLE) {
            List<Double> list = new ArrayList<Double>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.doubleValue());
                }
            }
            double[] ar = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.doubleSeq(ar);
        }
        // DS_LONGLONG
        else if (targetDataType == DataType.DS_LONGLONG) {
            List<Long> list = new ArrayList<Long>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.longValue());
                }
            }
            T_LONGLONG[] ar = new T_LONGLONG[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = ODSHelper.asODSLongLong(list.get(i));
            }
            tsValue.u.longlongSeq(ar);
        }
        // DS_FLOAT
        else if (targetDataType == DataType.DS_FLOAT) {
            List<Float> list = new ArrayList<Float>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.floatValue());
                }
            }
            float[] ar = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.floatSeq(ar);
        }
        // DS_COMPLEX
        else if (targetDataType == DataType.DS_COMPLEX) {
            List<Float> list = new ArrayList<Float>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.floatValue());
                }
            }
            int size = list.size() / 2;
            T_COMPLEX[] ar = new T_COMPLEX[size];
            for (int i = 0; i < size; i++) {
                ar[i] = new T_COMPLEX();
                ar[i].r = list.get(i * 2);
                ar[i].i = list.get(i * 2 + 1);
            }
            tsValue.u.complexSeq(ar);
        }
        // DS_DCOMPLEX
        else if (targetDataType == DataType.DS_DCOMPLEX) {
            List<Double> list = new ArrayList<Double>();
            for (long iidExtComp : iidExtComps) {
                for (Number value : readNumberValues(atfxCache, iidExtComp)) {
                    list.add(value.doubleValue());
                }
            }
            int size = list.size() / 2;
            T_DCOMPLEX[] ar = new T_DCOMPLEX[size];
            for (int i = 0; i < size; i++) {
                ar[i] = new T_DCOMPLEX();
                ar[i].r = list.get(i * 2);
                ar[i].i = list.get(i * 2 + 1);
            }
            tsValue.u.dcomplexSeq(ar);
        }
        // unsupported
        else {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "Reading values from external component not yet supported for datatype: "
                                          + ODSHelper.dataType2String(targetDataType));
        }
        return tsValue;
    }

    private List<Number> readNumberValues(AtfxCache atfxCache, long iidLc) throws AoException {
        List<Number> list = new ArrayList<Number>();
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        int attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
        String filenameUrl = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.stringVal();
        File atfxFile = new File(atfxCache.getContext().get("FILENAME").value.u.stringVal());
        File extCompFile = new File(atfxFile.getParentFile(), filenameUrl);

        // get datatype
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
        int valueType = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.enumVal();

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        int startOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            startOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            startOffset = (int) ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // value_offset
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_offset");
        int valueOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // block_size, valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "block_size");
        int blockSize = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
        int valuesperblock = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // read values
        RandomAccessFile raf = null;
        FileChannel sourceChannel = null;
        MappedByteBuffer sourceMbb = null;
        try {
            // open source channel
            raf = new RandomAccessFile(extCompFile, "r");
            sourceChannel = raf.getChannel();
            sourceMbb = sourceChannel.map(MapMode.READ_ONLY, valueOffset, extCompFile.length());

            // set byte order
            ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
            // dt_short_beo,dt_long_beo,dt_longlong_beo,ieeefloat4_beo,ieeefloat8_beo
            if ((valueType == 7) || (valueType == 8) || (valueType == 9) || (valueType == 11)) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            }
            sourceMbb.order(byteOrder);

            // int typeSize = 0;
            // // 2=dt_short
            // if (valueType == 2) {
            // typeSize = 2;
            // }
            // // 3=dt_long, 8=dt_long_beo, 5=ieeefloat4, 10=ieeefloat4_beo
            // else if ((valueType == 3) || (valueType == 8) || (valueType == 5) || (valueType == 10)) {
            // typeSize = 4;
            // }
            // // 6=ieeefloat8, 10=ieeefloat8_beo
            // else if ((valueType == 6) || (valueType == 10)) {
            // typeSize = 8;
            // }

            // read values
            for (int i = 0; i < componentLength; i++) {
                // calculate index
                // int idx = (startOffset + valueOffset) + (i * typeSize);
                int idx = (startOffset + valueOffset) + (i * (blockSize / valuesperblock));

                // int block = i / valuesperblock;

                // 2=dt_short
                if (valueType == 2) {
                    list.add(sourceMbb.getShort(idx));
                }
                // 3=dt_long, 8=dt_long_beo
                else if ((valueType == 3) || (valueType == 8)) {
                    list.add(sourceMbb.getInt(idx));
                }
                // 4=dt_longlong, 8=dt_long_beo
                else if ((valueType == 4) || (valueType == 9)) {
                    list.add(sourceMbb.getLong(idx));
                }
                // 5=ieeefloat4, 10=ieeefloat4_beo
                else if ((valueType == 5) || (valueType == 10)) {
                    list.add(sourceMbb.getFloat(idx));
                }
                // 6=ieeefloat8, 10=ieeefloat8_beo
                else if ((valueType == 6) || (valueType == 11)) {
                    list.add(sourceMbb.getDouble(idx));
                }
                // unsupported data type
                else {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "Unsupported 'value_type': " + valueType);
                }

            }

            return list;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                // unmap(sourceChannel, sourceMbb)
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (raf != null) {
                    raf.close();
                }
                sourceMbb = null;
                raf = null;
                sourceChannel = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private Collection<Byte> readByteValues(AtfxCache atfxCache, long iidLc) throws AoException {
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        int attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
        String filenameUrl = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.stringVal();
        File atfxFile = new File(atfxCache.getContext().get("FILENAME").value.u.stringVal());
        File extCompFile = new File(atfxFile.getParentFile(), filenameUrl);

        // get datatype
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
        int valueType = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.enumVal();

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        int startOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            startOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            startOffset = (int) ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // value_offset
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_offset");
        int valueOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // block_size, valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "block_size");
        int blockSize = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // valuesperblock
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
        int valuesperblock = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // read values
        RandomAccessFile raf = null;
        FileChannel sourceChannel = null;
        MappedByteBuffer sourceMbb = null;
        try {
            // open source channel
            raf = new RandomAccessFile(extCompFile, "r");
            sourceChannel = raf.getChannel();
            sourceMbb = sourceChannel.map(MapMode.READ_ONLY, valueOffset, extCompFile.length());

            // read values
            List<Byte> list = new ArrayList<Byte>();
            for (int i = 0; i < componentLength; i++) {
                // calculate index
                int idx = (startOffset + valueOffset) + (i * (blockSize / valuesperblock));
                if (valueType == 1) {
                    list.add(sourceMbb.get(idx));
                }
                // unsupported data type
                else {
                    throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                          "Unsupported 'value_type' for data type DT_BYTE: " + valueType);
                }
            }
            return list;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                // unmap(sourceChannel, sourceMbb)
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (raf != null) {
                    raf.close();
                }
                sourceMbb = null;
                raf = null;
                sourceChannel = null;
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

}
