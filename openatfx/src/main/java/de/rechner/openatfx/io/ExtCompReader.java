package de.rechner.openatfx.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;

import de.rechner.openatfx.util.ODSHelper;


public class ExtCompReader {

    private static final Log LOG = LogFactory.getLog(ExtCompReader.class);

    /** The singleton instance */
    private static volatile ExtCompReader instance;

    public TS_Value readValues(InstanceElement[] ieExtComps, DataType targetDataType) throws AoException {
        if (ieExtComps.length != 1) {
            throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                                  "The implementation currently only may read exactly one external component file");
        }

        TS_Value tsValue = new TS_Value();
        tsValue.flag = (short) 15;
        tsValue.u = new TS_Union();

        // DS_LONG
        if (targetDataType == DataType.DS_LONG) {
            List<Integer> list = new ArrayList<Integer>();
            for (InstanceElement ieExtComp : ieExtComps) {
                for (Number value : readNumberValues(ieExtComp)) {
                    list.add(value.intValue());
                }
            }
            int[] ar = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.longSeq(ar);
        }
        // DS_FLOAT
        else if (targetDataType == DataType.DS_FLOAT) {
            List<Float> list = new ArrayList<Float>();
            for (InstanceElement ieExtComp : ieExtComps) {
                for (Number value : readNumberValues(ieExtComp)) {
                    list.add(value.floatValue());
                }
            }
            float[] ar = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.floatSeq(ar);
        }
        // DS_DOUBLE
        else if (targetDataType == DataType.DS_DOUBLE) {
            List<Double> list = new ArrayList<Double>();
            for (InstanceElement ieExtComp : ieExtComps) {
                for (Number value : readNumberValues(ieExtComp)) {
                    list.add(value.doubleValue());
                }
            }
            double[] ar = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ar[i] = list.get(i);
            }
            tsValue.u.doubleSeq(ar);
        }
        // DS_COMPLEX
        else if (targetDataType == DataType.DS_COMPLEX) {
            List<Float> list = new ArrayList<Float>();
            for (InstanceElement ieExtComp : ieExtComps) {
                for (Number value : readNumberValues(ieExtComp)) {
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
            for (InstanceElement ieExtComp : ieExtComps) {
                for (Number value : readNumberValues(ieExtComp)) {
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

    private List<Number> readNumberValues(InstanceElement ieExtComp) throws AoException {
        List<Number> list = new ArrayList<Number>();

        // open file
        AoSession session = ieExtComp.getApplicationElement().getApplicationStructure().getSession();
        File atfxFile = new File(session.getContextByName("FILENAME").value.u.stringVal());
        File extCompFile = new File(atfxFile.getParentFile(),
                                    ieExtComp.getValueByBaseName("filename_url").value.u.stringVal());

        // get datatype
        int valueType = ieExtComp.getValueByBaseName("value_type").value.u.enumVal();

        // read length
        int componentLength = ieExtComp.getValueByBaseName("component_length").value.u.longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        int startOffset = 0;
        NameValueUnit nvuStartOffset = ieExtComp.getValueByBaseName("start_offset");
        if (nvuStartOffset.value.u.discriminator() == DataType.DT_LONG) {
            startOffset = nvuStartOffset.value.u.longVal();
        } else if (nvuStartOffset.value.u.discriminator() == DataType.DT_LONGLONG) {
            startOffset = (int) ODSHelper.asJLong(nvuStartOffset.value.u.longlongVal());
        }

        // value_offset, block_size, valuesperblock
        int valueOffset = ieExtComp.getValueByBaseName("value_offset").value.u.longVal();
        int blockSize = ieExtComp.getValueByBaseName("block_size").value.u.longVal();
        // int valuesperblock = ieExtComp.getValueByBaseName("valuesperblock").value.u.longVal();

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

            // read values
            for (int i = 0; i < componentLength; i++) {
                // calculate index
                int idx = (startOffset + valueOffset) + (i * blockSize);

                // 3=dt_long, 8=dt_long_beo
                if ((valueType == 3) || (valueType == 8)) {
                    list.add(sourceMbb.getInt(idx));
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
