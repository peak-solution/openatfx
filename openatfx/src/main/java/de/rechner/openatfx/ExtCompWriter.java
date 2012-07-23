package de.rechner.openatfx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
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
     * @param atfxCache
     * @param ieLocalColumn
     * @param values
     * @throws AoException
     */
    public void writeValues(AtfxCache atfxCache, InstanceElement ieLocalColumn, TS_Value values) throws AoException {
        // open file
        AoSession aoSession = ieLocalColumn.getApplicationElement().getApplicationStructure().getSession();
        String rootPath = aoSession.getContextByName("FILE_ROOT").value.u.stringVal();
        File extCompFile = new File(rootPath, "binary.bin");

        // read values
        RandomAccessFile raf = null;
        FileChannel channel = null;
        try {
            // open source channel
            channel = new FileOutputStream(extCompFile, true).getChannel();
            long startOffset = channel.size();

            // write values
            DataType dt = values.u.discriminator();
            int valueType = 0;
            int length = 0;
            int blockSize = 0;
            // DS_FLOAT
            if (dt == DataType.DS_FLOAT) {
                valueType = 5;
                blockSize = 4;
                length = values.u.floatSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * 4);
                for (int i = 0; i < length; i++) {
                    bb.putFloat(values.u.floatSeq()[i]);
                }
                bb.rewind();
                channel.write(bb);
            }
            // DS_DOUBLE
            else if (dt == DataType.DS_DOUBLE) {
                valueType = 6;
                blockSize = 8;
                length = values.u.doubleSeq().length;
                ByteBuffer bb = ByteBuffer.allocate(length * 8);
                for (int i = 0; i < length; i++) {
                    bb.putDouble(values.u.doubleSeq()[i]);
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
            ApplicationElement aeLocalColumn = ieLocalColumn.getApplicationElement();
            long aidLc = ODSHelper.asJLong(aeLocalColumn.getId());

            // delete existing 'AoExternalComponent' instances
            ApplicationRelation relLcExtComp = atfxCache.getApplicationRelationByBaseName(aidLc, "external_component");
            if (relLcExtComp == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application relation of type 'external_component' found!");
            }
            ApplicationElement aeExtComp = relLcExtComp.getElem2();
            long aidExtComp = ODSHelper.asJLong(aeExtComp.getId());
            InstanceElementIterator iter = ieLocalColumn.getRelatedInstances(relLcExtComp, "*");
            InstanceElement[] ieExtComps = iter.nextN(iter.getCount());
            iter.destroy();
            for (InstanceElement ieExtComp : ieExtComps) {
                aeExtComp.removeInstance(ieExtComp.getId(), false);
            }

            // create 'AoExternalComponent' instance
            long iidExtComp = atfxCache.nextIid(aidExtComp);
            atfxCache.addInstance(aidExtComp, iidExtComp);
            // id
            Integer attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "id");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'id' found for '" + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.asODSLongLong(iidExtComp));
            // name
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "name");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'name' found for '" + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, "ExtComp");
            // filename_url
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "filename_url");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'filename_url' found for '"
                                              + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, extCompFile.getName());
            // value_type
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_type");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'value_type' found for '" + aeExtComp.getName()
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) valueType);

            // component_length
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "component_length");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'component_length' found for '"
                                              + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) length);
            // start_offset
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "start_offset");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'start_offset' found for '"
                                              + aeExtComp.getName() + "'");
            }
            DataType attrDt = atfxCache.getApplicationAttribute(aidExtComp, attrNo).getDataType();
            if (attrDt == DataType.DT_LONG) {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) startOffset);
            } else {
                atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, ODSHelper.asODSLongLong(startOffset));
            }
            // block_size
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "block_size");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'block_size' found for '" + aeExtComp.getName()
                                              + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) blockSize);
            // valuesperblock
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "valuesperblock");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'valuesperblock' found for '"
                                              + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) 1);
            // value_offset
            attrNo = atfxCache.getAttrNoByBaName(aidExtComp, "value_offset");
            if (attrNo == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                      "No application attribute of type 'valuesperblock' found for '"
                                              + aeExtComp.getName() + "'");
            }
            atfxCache.setInstanceValue(aidExtComp, iidExtComp, attrNo, (int) 0);

            // relation to LocalColumn
            atfxCache.createInstanceRelations(aidLc, ODSHelper.asJLong(ieLocalColumn.getId()), relLcExtComp,
                                              Arrays.asList(iidExtComp));

            // update sequence representation
            NameValueUnit nvuSeqRep = ieLocalColumn.getValueByBaseName("sequence_representation");
            int seqRep = ieLocalColumn.getValueByBaseName("sequence_representation").value.u.enumVal();
            if (seqRep == 0) { // explicit -> external component
                nvuSeqRep.value.u.enumVal(7);
                ieLocalColumn.setValueSeq(new NameValueUnit[] { nvuSeqRep });
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
