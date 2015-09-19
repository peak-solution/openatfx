package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    // private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);

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
        Date date = new Date(hdBlock.getStartTimeNs() / 1000000); // convert from ns to ms
        nvuList.add(ODSHelper.createDateNVU("date_created", ODSHelper.asODSDate(date)));
        nvuList.add(ODSHelper.createDateNVU("mea_begin", ODSHelper.asODSDate(date)));
        nvuList.add(ODSHelper.createLongLongNVU("start_time_ns", hdBlock.getStartTimeNs()));
        nvuList.add(ODSHelper.createShortNVU("tz_offset_min", hdBlock.getTzOffsetMin()));
        nvuList.add(ODSHelper.createShortNVU("dst_offset_min", hdBlock.getDstOffsetMin()));
        nvuList.add(ODSHelper.createEnumNVU("time_quality_class", hdBlock.getTimeClass()));

        ieMea.setValueSeq(nvuList.toArray(new NameValueUnit[0]));

        return ieMea;
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
