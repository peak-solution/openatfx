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

    private final MDF4XMLParser xmlParser;

    /**
     * Constructor.
     */
    public AoSessionWriter() {
        this.xmlParser = new MDF4XMLParser();
    }

    /**
     * Appends the content of the MDF4 file to the ASAM ODS session.
     * 
     * @param modelCache The application model cache.
     * @param ieTst The parent 'AoTest' instance.
     * @param hdBlock The HDBLOCK.
     * @return the created AoMeasurement instance element
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
        } else {
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

        // write attachments: not yet supported!
        if (hdBlock.getLnkAtFirst() > 0) {
            LOG.warn("Found ATBLOCK, currently not yet supported!");
        }

        // write events: not yet supported!
        if (hdBlock.getLnkEvFirst() > 0) {
            LOG.warn("Found EVBLOCK, currently not yet supported!");
        }

        return ieMea;
    }

    /**
     * Writes the content of all FHBLOCKS (file history) to the session.
     * 
     * @param modelCache The application model cache.
     * @param ieTst The parent 'AoTest' instance.
     * @param hdBlock The HDBLOCK.
     * @return the created AoMeasurement instance element
     * @throws ConvertException Error converting.
     */
    private void writeFileHistory(ODSModelCache modelCache, InstanceElement ieTst, HDBLOCK hdBlock) throws AoException,
            IOException {
        ApplicationElement aeFh = modelCache.getApplicationElement("fh");
        ApplicationRelation relTstFh = modelCache.getApplicationRelation("tst", "fh", "fh");
        NumberFormat nf = new DecimalFormat("000");

        int no = 1;
        FHBLOCK fhBlock = hdBlock.getFhFirstBlock();
        while (fhBlock != null) {
            InstanceElement ieFh = aeFh.createInstance("fh_" + nf.format(no));
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
