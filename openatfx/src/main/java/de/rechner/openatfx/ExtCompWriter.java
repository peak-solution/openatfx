package de.rechner.openatfx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class for writing values to external component files.
 * 
 * @author Christian Rechner
 */
class ExtCompWriter {

    private static final Log LOG = LogFactory.getLog(ExtCompWriter.class);

    /** The singleton instance */
    private static volatile ExtCompWriter instance;

    /**
     * Determines the file name for the external component.
     * 
     * @param ieLocalColumn The LocalColumn instance.
     * @return
     * @throws AoException
     */
    private File getExtCompFile(InstanceElement ieLocalColumn) throws AoException {
        // retrieve AoMeasurement instance id
        InstanceElementIterator iter = ieLocalColumn.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
        if (iter.getCount() < 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "No parent SubMatrix found for LocalColumn instance '" + ieLocalColumn.getAsamPath()
                                          + "'");
        }
        InstanceElement ieSubMatrix = iter.nextOne();
        iter.destroy();
        iter = ieSubMatrix.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
        if (iter.getCount() < 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "No parent AoMeasurement found for SubMatrix instance '" + ieSubMatrix.getAsamPath()
                                          + "'");
        }
        InstanceElement ieMeasurement = iter.nextOne();
        iter.destroy();

        // retrieve root path from context parameter
        AoSession aoSession = ieLocalColumn.getApplicationElement().getApplicationStructure().getSession();
        String rootPath = aoSession.getContextByName("FILE_ROOT").value.u.stringVal();

        // file name is build with measurement id
        StringBuffer sb = new StringBuffer();
        sb.append("mea_");
        sb.append(ODSHelper.asJLong(ieMeasurement.getId()));
        sb.append(".bin");

        return new File(rootPath, sb.toString());
    }

    public void writeValues(AtfxCache atfxCache, InstanceElement ieLocalColumn, TS_Value values) throws AoException {
        // open file
        File extCompFile = getExtCompFile(ieLocalColumn);

        // read values
        RandomAccessFile raf = null;
        FileChannel channel = null;
        try {
            // open source channel
            channel = new FileOutputStream(extCompFile, true).getChannel();

            // write values
            DataType dt = values.u.discriminator();
            // DS_FLOAT
            if (dt == DataType.DS_FLOAT) {
                ByteBuffer bb = ByteBuffer.allocate(values.u.floatSeq().length * 4);
                for (int i = 0; i < values.u.floatSeq().length; i++) {
                    bb.putFloat(values.u.floatSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                ByteBuffer bb = ByteBuffer.allocate(values.u.doubleSeq().length * 8);
                for (int i = 0; i < values.u.doubleSeq().length; i++) {
                    bb.putDouble(values.u.doubleSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }

            // create extComp instance
            Set<Long> set = atfxCache.getAidsByBaseType("aoexternalcomponent");
            if (set == null || set.isEmpty()) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application element of type' AoExternalComponent' found!");
            }
            long aidExtComp = set.iterator().next();
            
            System.out.println(aidExtComp);

            // Name (1111) ExtComp
            // OrdinalNumber (1111) 1
            // TypeSpecification (1111) ieeefloat4
            // Length (1111) 165
            // StartOffset (1111) 0
            // Blocksize (1111) 4
            // ValuesPerBlock (1111) 1
            // ValueOffset (1111) 0
            // FilenameURL (1111) binary1.dat
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
                if (raf != null) {
                    raf.close();
                }
                raf = null;
                channel = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        System.out.println("write to file! " + getExtCompFile(ieLocalColumn));
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
    public static ExtCompWriter getInstance() {
        if (instance == null) {
            instance = new ExtCompWriter();
        }
        return instance;
    }

}
