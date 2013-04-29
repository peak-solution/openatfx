package de.rechner.openatfx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

        // DS_STRING
        if (targetDataType == DataType.DS_STRING || targetDataType == DataType.DS_BYTESTR) {
            List<String> list = new ArrayList<String>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readStringValues(atfxCache, iidExtComp));
            }
            tsValue.u.stringSeq(list.toArray(new String[0]));
        } // DS_DATE
        else if (targetDataType == DataType.DS_DATE) {
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
                list.addAll(readNumberValues(atfxCache, iidExtComp));
            }
            // DS_BYTE
            if (targetDataType == DataType.DS_BYTE) {
                byte[] ar = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).byteValue();
                }
                tsValue.u.byteSeq(ar);
            }
            // DS_SHORT
            else if (targetDataType == DataType.DS_SHORT) {
                short[] ar = new short[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).shortValue();
                }
                tsValue.u.shortSeq(ar);
            }
            // DS_LONG
            else if (targetDataType == DataType.DS_LONG) {
                int[] ar = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).intValue();
                }
                tsValue.u.longSeq(ar);
            }
            // DS_DOUBLE
            else if (targetDataType == DataType.DS_DOUBLE) {
                double[] ar = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).doubleValue();
                }
                tsValue.u.doubleSeq(ar);
            }
            // DS_LONGLONG
            else if (targetDataType == DataType.DS_LONGLONG) {
                T_LONGLONG[] ar = new T_LONGLONG[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = ODSHelper.asODSLongLong(list.get(i).longValue());
                }
                tsValue.u.longlongSeq(ar);
            }
            // DS_FLOAT
            else if (targetDataType == DataType.DS_FLOAT) {
                float[] ar = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).floatValue();
                }
                tsValue.u.floatSeq(ar);
            }
            // DS_COMPLEX
            else if (targetDataType == DataType.DS_COMPLEX) {
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
            else if (targetDataType == DataType.DS_DCOMPLEX) {
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
        byte[] backingBuffer = new byte[blockSize];
        ByteBuffer sourceMbb = ByteBuffer.wrap(backingBuffer);
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        if ((valueType == 7) || (valueType == 8) || (valueType == 9) || (valueType == 11)) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        }
        sourceMbb.order(byteOrder);
        try {
            // open source channel
            raf = new RandomAccessFile(extCompFile, "r");
            raf.seek(startOffset);

            // loop over blocks
            for (int i = 0; i < componentLength; i += valuesperblock) {
                raf.read(backingBuffer);
                sourceMbb.position(valueOffset);
                // sub blocks are consecutive, puhh!
                for (int j = 0; j < valuesperblock; j++) {

                    // 2=dt_short
                    if (valueType == 2) {
                        list.add(sourceMbb.getShort());
                    }
                    // 3=dt_long, 8=dt_long_beo
                    else if ((valueType == 3) || (valueType == 8)) {
                        list.add(sourceMbb.getInt());
                    }
                    // 4=dt_longlong, 8=dt_long_beo
                    else if ((valueType == 4) || (valueType == 9)) {
                        list.add(sourceMbb.getLong());
                    }
                    // 5=ieeefloat4, 10=ieeefloat4_beo
                    else if ((valueType == 5) || (valueType == 10)) {
                        list.add(sourceMbb.getFloat());
                    }
                    // 6=ieeefloat8, 10=ieeefloat8_beo
                    else if ((valueType == 6) || (valueType == 11)) {
                        list.add(sourceMbb.getDouble());
                    }
                    // 1=dt_byte
                    else if (valueType == 1) {
                        list.add(sourceMbb.get());
                    }
                    // unsupported data type
                    else {
                        raf.close();
                        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                              "Unsupported 'value_type': " + valueType);
                    }
                }
            }

            return list;
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

    private Collection<String> readStringValues(AtfxCache atfxCache, long iidExtComp) throws AoException {
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // get filename
        int attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
        String filenameUrl = atfxCache.getInstanceValue(aidExtComp, attrNo, iidExtComp).u.stringVal();
        File atfxFile = new File(atfxCache.getContext().get("FILENAME").value.u.stringVal());
        File extCompFile = new File(atfxFile.getParentFile(), filenameUrl);

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

        // value_offset is irrelevant ODS Standard 3.42, page 3-51

        // read values
        RandomAccessFile raf = null;
        byte[] backingBuffer = new byte[componentLength];
        List<String> list = new ArrayList<String>();
        try {
            // open source channel
            raf = new RandomAccessFile(extCompFile, "r");
            raf.seek(startOffset);
            raf.read(backingBuffer);

            int startPosition = 0;
            for (int position = 0; position < componentLength; position++) {
                if (backingBuffer[position] == 0) {
                    list.add(new String(backingBuffer, startPosition, position - startPosition, "ISO-8859-1"));
                    startPosition = position + 1;
                }
            }
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
        File atfxFile = new File(atfxCache.getContext().get("FILENAME").value.u.stringVal());
        File flagsFile = new File(atfxFile.getParentFile(), flagsFilenameUrl);

        // read start offset, may be DT_LONG or DT_LONGLONG
        int flagsStartOffset = 0;
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "flags_start_offset");
        TS_Value vStartOffset = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc);
        if (vStartOffset.u.discriminator() == DataType.DT_LONG) {
            flagsStartOffset = vStartOffset.u.longVal();
        } else if (vStartOffset.u.discriminator() == DataType.DT_LONGLONG) {
            flagsStartOffset = (int) ODSHelper.asJLong(vStartOffset.u.longlongVal());
        }

        // read length
        attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
        int componentLength = atfxCache.getInstanceValue(aidExtComp, attrNo, iidLc).u.longVal();

        // read values
        TS_Value tsValue = new TS_Value();
        tsValue.flag = (short) 15;
        tsValue.u = new TS_Union();
        tsValue.u.shortSeq(new short[componentLength]);

        RandomAccessFile raf = null;
        byte[] backingBuffer = new byte[2];
        ByteBuffer sourceMbb = ByteBuffer.wrap(backingBuffer);
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        sourceMbb.order(byteOrder);
        try {
            // open source channel
            raf = new RandomAccessFile(flagsFile, "r");
            raf.seek(flagsStartOffset);

            for (int i = 0; i < componentLength; i++) {
                raf.read(backingBuffer);
                tsValue.u.shortSeq()[i] = sourceMbb.getShort();
            }

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

}
