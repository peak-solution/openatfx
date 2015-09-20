package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx_mdf4.util.ODSModelCache;
import de.rechner.openatfx_mdf4.xml.MDF4XMLParser;


/**
 * Main class for writing the MDF4 file content into an ASAM ODS session backed by an ATFX file.
 * 
 * @author Christian Rechner
 */
class AoSessionWriter {

    private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);

    private final NumberFormat countFormat;
    private final MDF4XMLParser xmlParser;

    /**
     * Constructor.
     */
    public AoSessionWriter() {
        this.xmlParser = new MDF4XMLParser();
        this.countFormat = new DecimalFormat("0000");
    }

    /**
     * Appends the content of the MDF4 file to the ASAM ODS session.
     * 
     * @param modelCache The application model cache.
     * @param ieTst The parent 'AoTest' instance.
     * @param hdBlock The HDBLOCK.
     * @return The created AoMeasurement instance.
     * @throws ConvertException Error converting.
     */
    public synchronized InstanceElement writeMea(ODSModelCache modelCache, InstanceElement ieTst, HDBLOCK hdBlock)
            throws AoException, IOException {
        Path fileName = hdBlock.getMdfFilePath().getFileName();
        if (fileName == null) {
            throw new IOException("Unable to obtain file name!");
        }

        // create "AoMeasurement" instance and write descriptive data to instance attributes
        ApplicationElement aeMea = modelCache.getApplicationElement("mea");
        ApplicationRelation relTstMea = modelCache.getApplicationRelation("tst", "mea", "meas");
        InstanceElement ieMea = aeMea.createInstance(getResultName(fileName.toString(), null));
        ieTst.createRelation(relTstMea, ieMea);

        // meta information
        BLOCK block = hdBlock.getMdCommentBlock();
        List<NameValueUnit> nvuList = new ArrayList<NameValueUnit>();
        if (block instanceof TXBLOCK) {
            nvuList.add(ODSHelper.createStringNVU("desc", ((TXBLOCK) block).getTxData()));
        } else if (block instanceof MDBLOCK) {
            this.xmlParser.writeMDCommentToMea(ieMea, ((MDBLOCK) block).getMdData());
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(hdBlock.getStartTimeNs() / 1000000);
        if (!hdBlock.isLocalTime() && hdBlock.isTimeFlagsValid()) { // UTC time given, calc local
            cal.add(Calendar.MINUTE, hdBlock.getTzOffsetMin());
            cal.add(Calendar.MINUTE, hdBlock.getDstOffsetMin());
        }
        nvuList.add(ODSHelper.createDateNVU("date_created", ODSHelper.asODSDate(cal.getTime())));
        nvuList.add(ODSHelper.createDateNVU("mea_begin", ODSHelper.asODSDate(cal.getTime())));
        nvuList.add(ODSHelper.createLongLongNVU("start_time_ns", hdBlock.getStartTimeNs()));
        nvuList.add(ODSHelper.createShortNVU("local_time", hdBlock.isLocalTime() ? (short) 1 : (short) 0));
        nvuList.add(ODSHelper.createShortNVU("time_offsets_valid", hdBlock.isTimeFlagsValid() ? (short) 1 : (short) 0));
        nvuList.add(ODSHelper.createShortNVU("tz_offset_min", hdBlock.getTzOffsetMin()));
        nvuList.add(ODSHelper.createShortNVU("dst_offset_min", hdBlock.getDstOffsetMin()));
        nvuList.add(ODSHelper.createEnumNVU("time_quality_class", hdBlock.getTimeClass()));
        nvuList.add(ODSHelper.createShortNVU("start_angle_valid", hdBlock.isStartAngleValid() ? (short) 1 : (short) 0));
        nvuList.add(ODSHelper.createShortNVU("start_distance_valid", hdBlock.isStartDistanceValid() ? (short) 1
                : (short) 0));
        nvuList.add(ODSHelper.createDoubleNVU("start_angle_rad", hdBlock.getStartAngleRad()));
        nvuList.add(ODSHelper.createDoubleNVU("start_distance_m", hdBlock.getStartDistanceM()));
        ieMea.setValueSeq(nvuList.toArray(new NameValueUnit[0]));

        // write file history (FHBLOCK)
        writeFileHistory(modelCache, ieTst, hdBlock);

        // write channel hierarchy (CHBLOCK): not yet supported!
        if (hdBlock.getLnkChFirst() > 0) {
            LOG.warn("Found CHBLOCK, currently not yet supported!");
        }

        // write attachments (ATBLOCK): not yet supported!
        if (hdBlock.getLnkAtFirst() > 0) {
            LOG.warn("Found ATBLOCK, currently not yet supported!");
        }

        // write events (EVBLOCK): not yet supported!
        if (hdBlock.getLnkEvFirst() > 0) {
            LOG.warn("Found EVBLOCK, currently not yet supported!");
        }

        // write submatrices
        writeSm(modelCache, ieMea, hdBlock);

        return ieMea;
    }

    /**
     * Writes the content of all FHBLOCKS (file history) to the session.
     * 
     * @param modelCache The application model cache.
     * @param ieTst The parent 'AoTest' instance.
     * @param hdBlock The HDBLOCK.
     * @throws AoException Error writing to session.
     * @throws IOException Error reading from MDF file.
     */
    private void writeFileHistory(ODSModelCache modelCache, InstanceElement ieTst, HDBLOCK hdBlock) throws AoException,
            IOException {
        ApplicationElement aeFh = modelCache.getApplicationElement("fh");
        ApplicationRelation relTstFh = modelCache.getApplicationRelation("tst", "fh", "fh");

        int no = 1;
        FHBLOCK fhBlock = hdBlock.getFhFirstBlock();
        while (fhBlock != null) {
            InstanceElement ieFh = aeFh.createInstance("fh_" + countFormat.format(no));
            ieTst.createRelation(relTstFh, ieFh);

            // meta information
            List<NameValueUnit> nvuList = new ArrayList<NameValueUnit>();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(hdBlock.getStartTimeNs() / 1000000);
            if (!hdBlock.isLocalTime() && hdBlock.isTimeFlagsValid()) { // UTC time given, calc local
                cal.add(Calendar.MINUTE, hdBlock.getTzOffsetMin());
                cal.add(Calendar.MINUTE, hdBlock.getDstOffsetMin());
            }
            nvuList.add(ODSHelper.createDateNVU("date", ODSHelper.asODSDate(cal.getTime())));
            nvuList.add(ODSHelper.createLongLongNVU("start_time_ns", hdBlock.getStartTimeNs()));
            nvuList.add(ODSHelper.createShortNVU("local_time", hdBlock.isLocalTime() ? (short) 1 : (short) 0));
            nvuList.add(ODSHelper.createShortNVU("time_offsets_valid", hdBlock.isTimeFlagsValid() ? (short) 1
                    : (short) 0));
            nvuList.add(ODSHelper.createShortNVU("tz_offset_min", hdBlock.getTzOffsetMin()));
            nvuList.add(ODSHelper.createShortNVU("dst_offset_min", hdBlock.getDstOffsetMin()));
            ieFh.setValueSeq(nvuList.toArray(new NameValueUnit[0]));

            this.xmlParser.writeMDCommentToFh(ieFh, fhBlock.getMdCommentBlock().getMdData());

            no++;
            fhBlock = fhBlock.getFhNextBlock();
        }
    }

    /**
     * Write the instances of 'AoSubMatrix'.
     * 
     * @param modelCache The application model cache.
     * @param ieTst The parent 'AoTest' instance.
     * @param hdBlock The HDBLOCK.
     * @throws AoException Error writing to session.
     * @throws IOException Error reading from MDF file.
     */
    private void writeSm(ODSModelCache modelCache, InstanceElement ieMea, HDBLOCK hdBlock) throws AoException,
            IOException {
        ApplicationElement aeSm = modelCache.getApplicationElement("sm");
        ApplicationRelation relMeaSm = modelCache.getApplicationRelation("mea", "sm", "sms");

        // iterate over data group blocks
        int grpNo = 1;
        DGBLOCK dgBlock = hdBlock.getDgFirstBlock();
        while (dgBlock != null) {

            // if sorted, only one channel group block is available
            CGBLOCK cgBlock = dgBlock.getCgFirstBlock();
            if (cgBlock.getLnkCgNext() > 0) {
                throw new IOException(
                                      "Currently only 'sorted' MDF4 files are supported, found 'unsorted' data! [DGBLOCK="
                                              + dgBlock + "]");
            }

            // skip channel groups having no channels (or optionally no values)
            if (cgBlock != null) {

                // check flags (not yet supported)
                if (cgBlock.getFlags() != 0) {
                    throw new IOException("VLSD or bus event data currently not supported! [DGBLOCK=" + dgBlock + "]");
                }
                // check invalidation bits (not yet supported)
                if (cgBlock.getInvalBytes() != 0) {
                    throw new IOException("Invalidation bits currently not supported! [DGBLOCK=" + dgBlock + "]");
                }

                // create SubMatrix instance
                InstanceElement ieSm = aeSm.createInstance("sm_" + countFormat.format(grpNo));
                List<NameValueUnit> nvuList = new ArrayList<>();
                TXBLOCK txAcqName = cgBlock.getTxAcqNameBlock();
                if (txAcqName != null) {
                    nvuList.add(ODSHelper.createStringNVU("acq_name", txAcqName.getTxData()));
                }
                SIBLOCK siAcqSource = cgBlock.getSiAcqSourceBlock();
                if (siAcqSource != null) {
                    writeSiBlock(modelCache, ieSm, siAcqSource);
                }
                BLOCK block = cgBlock.getMdCommentBlock();
                if (block instanceof TXBLOCK) {
                    nvuList.add(ODSHelper.createStringNVU("desc", ((TXBLOCK) block).getTxData()));
                } else if (block instanceof MDBLOCK) {
                    System.out.println("MEHR! " + block);
                    // this.xmlParser.writeMDCommentToMea(ieMea, ((MDBLOCK) block).getMdData());
                }
                nvuList.add(ODSHelper.createLongNVU("rows", (int) cgBlock.getCycleCount()));
                ieSm.setValueSeq(nvuList.toArray(new NameValueUnit[0]));
                ieMea.createRelation(relMeaSm, ieSm);

                // write LocalColumns
                // writeLcs(ieMea, ieSm, sourceFile, mdfChannel, binChannel, dgBlock, cgBlock, meqNames);
            }

            dgBlock = dgBlock.getDgNextBlock();
            grpNo++;
        }
    }

    /**************************************************************************************
     * helper methods
     **************************************************************************************/

    /**
     * Writes the content of all FHBLOCKS (file history) to the session.
     * 
     * @param modelCache The application model cache.
     * @param ie The instance.
     * @param siBlock The SIBLOCK.
     * @throws AoException Error writing to session.
     * @throws IOException Error reading from MDF file.
     */
    private void writeSiBlock(ODSModelCache modelCache, InstanceElement ie, SIBLOCK siBlock) throws AoException,
            IOException {
        List<NameValueUnit> nvuList = new ArrayList<>();

        // si_tx_name
        TXBLOCK txName = siBlock.getTxNameBlock();
        if (txName != null) {
            nvuList.add(ODSHelper.createStringNVU("src_name", txName.getTxData()));
        }
        // si_tx_path
        TXBLOCK txPath = siBlock.getTxPath();
        if (txPath != null) {
            nvuList.add(ODSHelper.createStringNVU("src_path", txPath.getTxData()));
        }
        // si_md_comment
        BLOCK block = siBlock.getMdCommentBlock();
        if (block instanceof TXBLOCK) {
            nvuList.add(ODSHelper.createStringNVU("src_cmt", ((TXBLOCK) block).getTxData()));
        } else if (block instanceof MDBLOCK) {
            System.out.println("MEHR! " + block);
        }
        // si_type
        nvuList.add(ODSHelper.createEnumNVU("src_type", siBlock.getSourceType()));
        // si_bus_type
        nvuList.add(ODSHelper.createEnumNVU("src_bus", siBlock.getBusType()));
        // si_flags
        nvuList.add(ODSHelper.createShortNVU("src_sim", siBlock.getFlags() > 0 ? (short) 1 : (short) 0));

        ie.setValueSeq(nvuList.toArray(new NameValueUnit[0]));
    }

    /**************************************************************************************
     * helper methods
     **************************************************************************************/

    private static String getResultName(String fileName, String resultSuffix) {
        String meaResultName = fileName.trim();
        if (resultSuffix != null && resultSuffix.length() > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(getFileNameWithoutExtension(fileName));
            sb.append(resultSuffix);
            sb.append(".");
            sb.append(getFileExtension(fileName));
            meaResultName = sb.toString();
        }
        return meaResultName;
    }

    private static String getFileNameWithoutExtension(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    private static String getFileExtension(String fileName) {
        String ext = null;
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1);
        }
        return ext;
    }

}
