package de.rechner.openatfx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.util.ODSHelper;


class ExtCompWriter {

    private static final Log LOG = LogFactory.getLog(ExtCompWriter.class);

    /** The singleton instance */
    private static volatile ExtCompWriter instance;

    /**
     * @param atfxCache
     * @param lcIid
     * @param value
     * @throws AoException
     */
    public void writeValues(AtfxCache atfxCache, long iidLc, TS_Value value) throws AoException {
        // open file
        // AoSession aoSession = ieLocalColumn.getApplicationElement().getApplicationStructure().getSession();
        // String rootPath = aoSession.getContextByName("FILE_ROOT").value.u.stringVal();
        String rootPath = "D:/PUBLIC";
        File extCompFile = new File(rootPath, "binary.bin");

        // read values
        RandomAccessFile raf = null;
        FileChannel channel = null;
        try {
            // open source channel
            channel = new FileOutputStream(extCompFile, true).getChannel();
            long startOffset = channel.size();

            // write values
            DataType dt = value.u.discriminator();
            int valueType = 0;
            int length = 0;
            int typeSize = 0;
            // DS_SHORT
            if (dt == DataType.DS_SHORT) {
                valueType = 2;
                typeSize = 2;
                length = value.u.shortSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * typeSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putShort(value.u.shortSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_LONG
            else if (dt == DataType.DS_LONG) {
                valueType = 3;
                typeSize = 4;
                length = value.u.longSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * typeSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putInt(value.u.longSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_FLOAT
            else if (dt == DataType.DS_FLOAT) {
                valueType = 5;
                typeSize = 4;
                length = value.u.floatSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * typeSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putFloat(value.u.floatSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                valueType = 6;
                typeSize = 8;
                length = value.u.doubleSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * typeSize);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < length; i++) {
                    bb.putDouble(value.u.doubleSeq()[i]);
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
                                       ODSHelper.createLongNV("", (int) (typeSize * length)).value);
            // valuesperblock
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'valuesperblock' found for '" + aidExtComp
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.createLongNV("", length).value);
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

            // update sequence representation
            int seqRepAttrNo = atfxCache.getAttrNoByBaName(aidLc, "sequence_representation");
            TS_Value seqRepValue = atfxCache.getInstanceValue(aidLc, seqRepAttrNo, iidLc);

            int seqRep = seqRepValue.u.enumVal();
            if (seqRep == 0) { // explicit -> external component
                seqRepValue.u.enumVal(7);
                atfxCache.setInstanceValue(aidLc, iidLc, seqRepAttrNo, seqRepValue);
            }

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
