package de.rechner.openatfx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;


class ExtCompWriter {

    private static final Log LOG = LogFactory.getLog(ExtCompWriter.class);

    /** The singleton instance */
    private static volatile ExtCompWriter instance;

    private static String stripExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }

    private static File getExtCompFile(AtfxCache atfxCache, int cnt) {
        Map<String, NameValue> context = atfxCache.getContext();
        String rootPath = context.get("FILE_ROOT").value.u.stringVal();
        File atfxPath = new File(context.get("FILENAME").value.u.stringVal());
        long extCompSize = ODSHelper.asJLong(context.get("EXT_COMP_SEGSIZE").value.u.longlongVal());
        File binFile = new File(rootPath, stripExtension(atfxPath.getName()) + "_" + cnt + ".btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFile(atfxCache, cnt + 1);
        }
        return binFile;
    }

    private static File getExtCompFileString(AtfxCache atfxCache, int cnt) {
        Map<String, NameValue> context = atfxCache.getContext();
        String rootPath = context.get("FILE_ROOT").value.u.stringVal();
        File atfxPath = new File(context.get("FILENAME").value.u.stringVal());
        long extCompSize = ODSHelper.asJLong(context.get("EXT_COMP_SEGSIZE").value.u.longlongVal());
        File binFile = new File(rootPath, stripExtension(atfxPath.getName()) + "_" + cnt + "_string.btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFile(atfxCache, cnt + 1);
        }
        return binFile;
    }

    private static File getExtCompFileFlags(AtfxCache atfxCache, int cnt) {
        Map<String, NameValue> context = atfxCache.getContext();
        String rootPath = context.get("FILE_ROOT").value.u.stringVal();
        File atfxPath = new File(context.get("FILENAME").value.u.stringVal());
        long extCompSize = ODSHelper.asJLong(context.get("EXT_COMP_SEGSIZE").value.u.longlongVal());
        File binFile = new File(rootPath, stripExtension(atfxPath.getName()) + "_" + cnt + "_flags.btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFile(atfxCache, cnt + 1);
        }
        return binFile;
    }

    /**
     * Writes measurement values to a external component file.
     * 
     * @param atfxCache The ATFX cache.
     * @param lcIid The LocalColumn instance id.
     * @param value The value to write.
     * @throws AoException Error writing value.
     */
    public void writeValues(AtfxCache atfxCache, long iidLc, TS_Value value) throws AoException {
        DataType dt = value.u.discriminator();

        // open file, strings have to be in the same file
        File extCompFile = null;
        if (dt == DataType.DS_STRING || dt == DataType.DS_DATE) {
            extCompFile = getExtCompFileString(atfxCache, 1);
        } else {
            extCompFile = getExtCompFile(atfxCache, 1);
        }

        FileOutputStream fos = null;
        FileChannel channel = null;
        try {
            // open source channel
            fos = new FileOutputStream(extCompFile, true);
            channel = fos.getChannel();
            long startOffset = channel.size();

            // write values
            int valueType = 0;
            int length = 0;
            int blockSize = 0;
            int valuesPerBlock = 0;

            // DS_BOOLEAN
            if (dt == DataType.DS_BOOLEAN) {
                valueType = 0; // dt_boolean
                length = value.u.booleanSeq().length;
                blockSize = 1 + ((int) value.u.booleanSeq().length - 1) / 8;
                valuesPerBlock = length;
                byte[] target = new byte[length];
                for (int i = 0; i < value.u.booleanSeq().length; i++) {
                    ODSHelper.setBit(target, i, value.u.booleanSeq()[i]);
                }
                channel.write(ByteBuffer.wrap(target));
            }
            // DS_STRING
            else if (dt == DataType.DS_STRING) {
                valueType = 12; // dt_string
                blockSize = 0;
                length = 0;
                valuesPerBlock = 0;
                for (String str : value.u.stringSeq()) {
                    byte[] b = str.getBytes("ISO-8859-1");
                    length += b.length;
                    ByteBuffer bb = ByteBuffer.wrap(b);
                    channel.write(bb);
                    bb = ByteBuffer.wrap(new byte[] { (byte) 0 });
                    length += 1;
                    channel.write(bb);
                }
            }
            // DS_BYTE
            else if (dt == DataType.DS_BYTE) {
                valueType = 1; // dt_byte
                blockSize = 1;
                valuesPerBlock = 1;
                length = value.u.byteSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                for (int i = 0; i < length; i++) {
                    bb.put(value.u.byteSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_SHORT
            else if (dt == DataType.DS_SHORT) {
                valueType = 2; // dt_short
                blockSize = 2;
                valuesPerBlock = 1;
                length = value.u.shortSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putShort(value.u.shortSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_LONG
            else if (dt == DataType.DS_LONG) {
                valueType = 3; // dt_long
                blockSize = 4;
                valuesPerBlock = 1;
                length = value.u.longSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putInt(value.u.longSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_LONGLONG
            else if (dt == DataType.DS_LONGLONG) {
                valueType = 4; // dt_longlong
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.u.longlongSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putLong(ODSHelper.asJLong(value.u.longlongSeq()[i]));
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DATE
            else if (dt == DataType.DS_DATE) {
                valueType = 12; // dt_date
                blockSize = 0;
                length = 0;
                valuesPerBlock = 1;
                for (String str : value.u.dateSeq()) {
                    byte[] b = str.getBytes("ISO-8859-1");
                    length += b.length;
                    ByteBuffer bb = ByteBuffer.wrap(b);
                    channel.write(bb);
                    bb = ByteBuffer.wrap(new byte[] { (byte) 0 });
                    length += 1;
                    channel.write(bb);
                }
            }
            // DS_FLOAT
            else if (dt == DataType.DS_FLOAT) {
                valueType = 5; // dt_float
                blockSize = 4;
                valuesPerBlock = 1;
                length = value.u.floatSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putFloat(value.u.floatSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_COMPLEX
            else if (dt == DataType.DS_COMPLEX) {
                valueType = 5; // dt_float
                blockSize = 4;
                valuesPerBlock = 1;
                length = value.u.complexSeq().length * 2;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < (length / 2); i++) {
                    bb.putFloat(value.u.complexSeq()[i].r);
                    bb.putFloat(value.u.complexSeq()[i].i);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                valueType = 6; // dt_double
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.u.doubleSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putDouble(value.u.doubleSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DCOMPLEX
            else if (dt == DataType.DS_DCOMPLEX) {
                valueType = 6; // dt_double
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.u.dcomplexSeq().length * 2;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length / 2; i++) {
                    bb.putDouble(value.u.dcomplexSeq()[i].r);
                    bb.putDouble(value.u.dcomplexSeq()[i].i);
                }
                bb.rewind();
                channel.write(bb);
            }
            // not supported
            else {
                throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "DataType '"
                        + ODSHelper.dataType2String(dt) + "' not yet supported for writing to external component file");
            }

            // create 'AoExternalComponent' instance
            long aidLc = atfxCache.getAidsByBaseType("aolocalcolumn").iterator().next();
            long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

            // delete existing 'AoExternalComponent' instances
            ApplicationRelation relLcExtComp = atfxCache.getApplicationRelationByBaseName(aidLc, "external_component");
            if (relLcExtComp == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application relation of type 'external_component' found!");
            }
            for (long relExtCompIid : atfxCache.getRelatedInstanceIds(aidLc, iidLc, relLcExtComp)) {
                atfxCache.removeInstance(aidExtComp, relExtCompIid);
            }

            // create 'AoExternalComponent' instance
            long iidExtComp = atfxCache.nextIid(aidExtComp);
            atfxCache.addInstance(aidExtComp, iidExtComp);
            // id
            Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "id");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'id' found for '" + aidExtComp + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createLongLongNV("", iidExtComp).value);
            // name
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "name");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'name' found for '" + aidExtComp + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createStringNV("", "ExtComp").value);
            // filename_url
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'filename_url' found for '" + aidExtComp + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                       ODSHelper.createStringNV("", extCompFile.getName()).value);
            // value_type
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'value_type' found for '" + aidExtComp + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createEnumNV("", valueType).value);

            // component_length
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'component_length' found for '" + aidExtComp
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createLongNV("", length).value);
            // start_offset
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'start_offset' found for '" + aidExtComp + "'");
            }
            DataType attrDt = atfxCache.getApplicationAttribute(aidExtComp, attrNo).getDataType();
            if (attrDt == DataType.DT_LONG) {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                           ODSHelper.createLongNV("", (int) startOffset).value);
            } else {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                           ODSHelper.createLongLongNV("", startOffset).value);
            }
            // block_size
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "block_size");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'block_size' found for '" + aidExtComp + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                       ODSHelper.createLongNV("", (int) blockSize).value);
            // valuesperblock
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'valuesperblock' found for '" + aidExtComp
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createLongNV("", valuesPerBlock).value);
            // value_offset
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_offset");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'valuesperblock' found for '" + aidExtComp
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createLongNV("", 0).value);

            // relation to LocalColumn
            atfxCache.createInstanceRelations(aidLc, iidLc, relLcExtComp, Arrays.asList(iidExtComp));

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
                if (fos != null) {
                    fos.close();
                }
                fos = null;
                channel = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Writes flag values to an external component file.
     * 
     * @param atfxCache The ATFX cache.
     * @param iidExtComp The ExternalComponent instance id.
     * @param value The value to write.
     * @throws AoException Error writing value.
     */
    public void writeFlags(AtfxCache atfxCache, long iidExtComp, short[] flags) throws AoException {
        long aidExtComp = atfxCache.getAidsByBaseType("aoexternalcomponent").iterator().next();

        // open file
        File flagsFile = getExtCompFileFlags(atfxCache, 1);
        FileOutputStream fos = null;
        FileChannel channel = null;
        try {
            // open source channel
            fos = new FileOutputStream(flagsFile, true);
            channel = fos.getChannel();
            long startOffset = channel.size();

            // DS_SHORT
            ByteBuffer bb = ByteBuffer.allocate(flags.length * 2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < flags.length; i++) {
                bb.putShort(flags[i]);
            }
            bb.rewind();
            channel.write(bb);

            // flags_filename_url
            Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "flags_filename_url");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'flags_filename_url' found for '" + aidExtComp
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                       ODSHelper.createStringNV("", flagsFile.getName()).value);

            // flags_start_offset
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "flags_start_offset");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'flags_start_offset' found for '" + aidExtComp
                                              + "'");
            }
            DataType attrDt = atfxCache.getApplicationAttribute(aidExtComp, attrNo).getDataType();
            if (attrDt == DataType.DT_LONG) {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                           ODSHelper.createLongNV("", (int) startOffset).value);
            } else {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo,
                                           ODSHelper.createLongLongNV("", startOffset).value);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
                if (fos != null) {
                    fos.close();
                }
                fos = null;
                channel = null;
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
