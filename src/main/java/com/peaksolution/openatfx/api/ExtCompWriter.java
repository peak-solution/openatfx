package com.peaksolution.openatfx.api;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.asam.ods.ErrorCode;
import org.asam.ods.SetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.util.FileUtil;
import com.peaksolution.openatfx.util.ODSHelper;


public class ExtCompWriter {

    private static final String EXTERNAL_COMPONENT = "external_component";
    private static final String AOLOCALCOLUMN = "aolocalcolumn";
    private static final String AOEXTERNALCOMPONENT = "aoexternalcomponent";
    private static final String EXT_COMP_SEGSIZE = "EXT_COMP_SEGSIZE";
    private static final String FILE_ROOT = "FILE_ROOT";
    private static final Logger LOG = LoggerFactory.getLogger(ExtCompWriter.class);

    private OpenAtfxAPIImplementation api;

    public ExtCompWriter(OpenAtfxAPIImplementation api) {
        this.api = api;
    }

    private File getExtCompFile(int cnt) {
        Map<String, NameValueUnit> context = api.getContext();
        String rootPath = context.get(FILE_ROOT).getValue().stringVal();
        File atfxPath = new File(context.get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal());
        long extCompSize = context.get(EXT_COMP_SEGSIZE).getValue().longlongVal();
        File binFile = new File(rootPath, FileUtil.stripExtension(atfxPath.getName()) + "_" + cnt + ".btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFile(cnt + 1);
        }
        return binFile;
    }

    private File getExtCompFileString(int cnt) {
        Map<String, NameValueUnit> context = api.getContext();
        String rootPath = context.get(FILE_ROOT).getValue().stringVal();
        File atfxPath = new File(context.get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal());
        long extCompSize = context.get(EXT_COMP_SEGSIZE).getValue().longlongVal();
        File binFile = new File(rootPath, FileUtil.stripExtension(atfxPath.getName()) + "_" + cnt + "_string.btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFileString(cnt + 1);
        }
        return binFile;
    }

    private File getExtCompFileUTF8String(int cnt) {
        Map<String, NameValueUnit> context = api.getContext();
        String rootPath = context.get(FILE_ROOT).getValue().stringVal();
        File atfxPath = new File(context.get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal());
        long extCompSize = context.get(EXT_COMP_SEGSIZE).getValue().longlongVal();
        File binFile = new File(rootPath, FileUtil.stripExtension(atfxPath.getName()) + "_" + cnt + "_utf8string.btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFileUTF8String(cnt + 1);
        }
        return binFile;
    }

    private File getExtCompFileBytestr(boolean getNextUnused) {
        Map<String, NameValueUnit> context = api.getContext();
        String rootPath = context.get(FILE_ROOT).getValue().stringVal();
        File atfxPath = new File(context.get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal());
        String pattern = FileUtil.stripExtension(atfxPath.getName()) + "_%d_bytestream.btf";
        int nextUnusedNo = 1;
        while (Files.exists(Paths.get(rootPath, String.format(pattern, nextUnusedNo)))) {
            nextUnusedNo++;
        }

        return new File(rootPath,
                        String.format(pattern, (getNextUnused ? nextUnusedNo : Math.max(1, nextUnusedNo - 1))));
    }

    private File getExtCompFileFlags(int cnt) {
        Map<String, NameValueUnit> context = api.getContext();
        String rootPath = context.get(FILE_ROOT).getValue().stringVal();
        File atfxPath = new File(context.get(OpenAtfxConstants.CONTEXT_FILENAME).getValue().stringVal());
        long extCompSize = context.get(EXT_COMP_SEGSIZE).getValue().longlongVal();
        File binFile = new File(rootPath, FileUtil.stripExtension(atfxPath.getName()) + "_" + cnt + "_flags.btf");
        if (binFile.length() > extCompSize) {
            binFile = getExtCompFileFlags(cnt + 1);
        }
        return binFile;
    }

    /**
     * Writes measurement values to a external component file.
     * 
     * @param iidLc The LocalColumn instance id.
     * @param value The value to write.
     * @throws OpenAtfxException Error writing value.
     */
    public void writeValues(long iidLc, SingleValue value) throws OpenAtfxException {
        if (!value.isValid()) {
            return;
        }

        DataType dt = value.discriminator();

        // open file, strings have to be in the same file
        File extCompFile = null;
        if (dt == DataType.DS_STRING) {
            extCompFile = getExtCompFileUTF8String(1);
        } else if (dt == DataType.DS_DATE) {
            extCompFile = getExtCompFileString(1);
        } else if (dt == DataType.DS_BYTESTR) {
            extCompFile = getExtCompFileBytestr(false);
        } else {
            extCompFile = getExtCompFile(1);
        }

        FileOutputStream fos = null;
        FileChannel channel = null;
        try {
            // delete existing 'AoExternalComponent' instances first, because new values have been set
            Element lcElement = api.getUniqueElementByBaseType(AOLOCALCOLUMN);
            Element extCompElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
            Relation relLcExtComp = api.getRelationByBaseName(lcElement.getId(), EXTERNAL_COMPONENT);
            if (relLcExtComp == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                            "No application relation of type '" + EXTERNAL_COMPONENT + "' found!");
            }
            for (long relExtCompIid : api.getRelatedInstanceIds(lcElement.getId(), iidLc, relLcExtComp)) {
                api.removeInstance(extCompElement.getId(), relExtCompIid);
            }

            // open source channel
            fos = new FileOutputStream(extCompFile, true);
            channel = fos.getChannel();
            long startOffset = channel.size();

            // write values
            int valueType = 0;
            int length = 0;
            int blockSize = 0;
            int valuesPerBlock = 0;

            int ordinalNumber = 1;

            // DS_BYTESTR
            if (dt == DataType.DS_BYTESTR) {
                valueType = 33; // dt_bytestr_leo

                long extCompSize = api.getContext().get(EXT_COMP_SEGSIZE).getValue().longlongVal();

                for (byte[] currentByteStream : value.bytestrSeq()) {
                    int lengthOfByteStream = currentByteStream.length;

                    int dataLength = 4 + lengthOfByteStream;
                    if (startOffset + length + dataLength > extCompSize) {
                        // allowed ext comp size exceeded -> write to new ext comp file:
                        // finish old ext comp file:
                        channel.close();
                        fos.close();

                        // write external component for old ext comp file:
                        createAoExternalComponent(iidLc, extCompFile, valueType, length, startOffset, blockSize,
                                                  valuesPerBlock, ordinalNumber++);

                        // get new ext comp file:
                        extCompFile = getExtCompFileBytestr(true);
                        fos = new FileOutputStream(extCompFile, true);
                        channel = fos.getChannel();
                        startOffset = channel.size();

                        valuesPerBlock = 0;
                        length = 0;
                    }

                    ByteBuffer bb = ByteBuffer.allocate(dataLength);
                    // length information must be big endian for dt_bytestr typespec, see recent clarification in ODS
                    // documentation:
                    bb.order(ByteOrder.BIG_ENDIAN);
                    // write 4 byte length block
                    bb.putInt(lengthOfByteStream);

                    bb.put(currentByteStream, 0, lengthOfByteStream);
                    Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                    length += channel.write(bb);
                    valuesPerBlock++;
                }
            }
            // DS_BOOLEAN
            else if (dt == DataType.DS_BOOLEAN) {
                valueType = 0; // dt_boolean
                length = value.booleanSeq().length;
                blockSize = 1 + (value.booleanSeq().length - 1) / 8;
                valuesPerBlock = length;
                byte[] target = new byte[length];
                for (int i = 0; i < value.booleanSeq().length; i++) {
                    ODSHelper.setBit(target, i, value.booleanSeq()[i]);
                }
                channel.write(ByteBuffer.wrap(target));
            }
            // DS_STRING
            else if (dt == DataType.DS_STRING) {
                valueType = 25; // dt_string_utf8
                length = 0;
                valuesPerBlock = value.stringSeq().length;
                for (String str : value.stringSeq()) {
                    byte[] b = str.getBytes(UTF_8);
                    length += b.length;
                    ByteBuffer bb = ByteBuffer.wrap(b);
                    channel.write(bb);
                    bb = ByteBuffer.wrap(new byte[] { (byte) 0 });
                    length += 1;
                    channel.write(bb);
                }
                blockSize = length;
            }
            // DS_BYTE
            else if (dt == DataType.DS_BYTE) {
                valueType = 1; // dt_byte
                blockSize = 1;
                valuesPerBlock = 1;
                length = value.byteSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                for (int i = 0; i < length; i++) {
                    bb.put(value.byteSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_SHORT
            else if (dt == DataType.DS_SHORT) {
                valueType = 2; // dt_short
                blockSize = 2;
                valuesPerBlock = 1;
                length = value.shortSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putShort(value.shortSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_LONG
            else if (dt == DataType.DS_LONG) {
                valueType = 3; // dt_long
                blockSize = 4;
                valuesPerBlock = 1;
                length = value.longSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putInt(value.longSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_LONGLONG
            else if (dt == DataType.DS_LONGLONG) {
                valueType = 4; // dt_longlong
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.longlongSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putLong(value.longlongSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_DATE
            else if (dt == DataType.DS_DATE) {
                valueType = 12; // dt_date (dt_string)
                blockSize = 0;
                length = 0;
                valuesPerBlock = 1;
                for (String str : value.dateSeq()) {
                    byte[] b = str.getBytes(ISO_8859_1);
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
                length = value.floatSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putFloat(value.floatSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_COMPLEX
            else if (dt == DataType.DS_COMPLEX) {
                valueType = 5; // dt_float
                blockSize = 4;
                valuesPerBlock = 1;
                length = value.complexSeq().length * 2;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < (length / 2); i++) {
                    bb.putFloat(value.complexSeq()[i].getR());
                    bb.putFloat(value.complexSeq()[i].getI());
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                valueType = 6; // dt_double
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.doubleSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putDouble(value.doubleSeq()[i]);
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // DS_DCOMPLEX
            else if (dt == DataType.DS_DCOMPLEX) {
                valueType = 6; // dt_double
                blockSize = 8;
                valuesPerBlock = 1;
                length = value.dcomplexSeq().length * 2;
                ByteBuffer bb = ByteBuffer.allocate(length * blockSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length / 2; i++) {
                    bb.putDouble(value.dcomplexSeq()[i].getR());
                    bb.putDouble(value.dcomplexSeq()[i].getI());
                }
                Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
                channel.write(bb);
            }
            // not supported
            else {
                throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "DataType '" + dt
                        + "' not yet supported for writing to external component file");
            }

            createAoExternalComponent(iidLc, extCompFile, valueType, length, startOffset, blockSize, valuesPerBlock,
                                      ordinalNumber);
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, e.getMessage());
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
     * Creates an AoExternalComponent element based on the given information.
     * 
     * @param iidLc
     * @param extCompFile
     * @param valueType
     * @param length
     * @param startOffset
     * @param blockSize
     * @param valuesPerBlock
     * @param ordinalNumber
     * @throws OpenAtfxException
     */
    private void createAoExternalComponent(long iidLc, File extCompFile, int valueType, int length, long startOffset,
            int blockSize, int valuesPerBlock, int ordinalNumber) throws OpenAtfxException {
        // create 'AoExternalComponent' instance
        Element lcElement = api.getUniqueElementByBaseType(AOLOCALCOLUMN);
        Element extCompElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
        Relation relLcExtComp = api.getRelationByBaseName(lcElement.getId(), EXTERNAL_COMPONENT);

        // collect attribute values
        Collection<NameValueUnit> values = new ArrayList<>();
        // name
        values.add(new NameValueUnit("name", DataType.DT_STRING, "ExtComp"));
        // filename_url
        values.add(new NameValueUnit("filename_url", DataType.DT_STRING, extCompFile.getName()));
        // value_type
        values.add(new NameValueUnit("value_type", DataType.DT_ENUM, valueType));
        // component_length
        values.add(new NameValueUnit("component_length", DataType.DT_LONG, length));
        // start_offset
        Attribute soAttr = extCompElement.getAttributeByBaseName("start_offset");
        if (DataType.DT_LONG == soAttr.getDataType()) {
            values.add(new NameValueUnit(soAttr.getName(), DataType.DT_LONG, Math.toIntExact(startOffset)));
        } else {
            values.add(new NameValueUnit(soAttr.getName(), DataType.DT_LONG, startOffset));
        }
        // block_size
        values.add(new NameValueUnit("block_size", DataType.DT_LONG, blockSize));
        // valuesperblock
        values.add(new NameValueUnit("valuesperblock", DataType.DT_LONG, valuesPerBlock));
        // value_offset
        values.add(new NameValueUnit("value_offset", DataType.DT_LONG, 0));
        // ordinal_number
        values.add(new NameValueUnit("ordinal_number", DataType.DT_LONG, ordinalNumber));

        // create 'AoExternalComponent' instance
        Instance extComp = api.createInstance(extCompElement.getId(), values);

        // relation to LocalColumn
        api.setRelatedInstances(lcElement.getId(), iidLc, relLcExtComp.getRelationName(),
                                Arrays.asList(extComp.getIid()), SetType.INSERT);
    }

    /**
     * Writes flag values to an external component file.
     * 
     * @param iidExtComp The ExternalComponent instance id.
     * @param flags The flags to write.
     * @throws OpenAtfxException Error writing value.
     */
    public void writeFlags(long iidExtComp, short[] flags) throws OpenAtfxException {
        Element extCompElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
        long aidExtComp = extCompElement.getId();
        Instance extComp = api.getInstanceById(aidExtComp, iidExtComp);
        ByteOrder flagsByteOrder = api.getByteOrder(aidExtComp, iidExtComp);

        // open file
        File flagsFile = getExtCompFileFlags(1);
        try (FileOutputStream fos = new FileOutputStream(flagsFile, true); FileChannel channel = fos.getChannel()) {
            long startOffset = channel.size();

            // DS_SHORT
            ByteBuffer bb = ByteBuffer.allocate(flags.length * 2);
            bb.order(flagsByteOrder);
            for (int i = 0; i < flags.length; i++) {
                bb.putShort(flags[i]);
            }
            Buffer.class.cast(bb).rewind(); // workaround: make buildable with both java8 and java9
            channel.write(bb);

            // flags_filename_url
            extComp.setAttributeValue(new NameValueUnit("flags_filename_url", DataType.DT_STRING, flagsFile.getName()));
            // flags_start_offset
            Attribute fsoAttr = extComp.getElement().getAttributeByBaseName("flags_start_offset");
            if (DataType.DT_LONG == fsoAttr.getDataType()) {
                extComp.setAttributeValue(new NameValueUnit(fsoAttr.getName(), DataType.DT_LONG,
                                                            Math.toIntExact(startOffset)));
            } else {
                extComp.setAttributeValue(new NameValueUnit(fsoAttr.getName(), DataType.DT_LONG, startOffset));
            }
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, e.getMessage());
        }
    }
}
