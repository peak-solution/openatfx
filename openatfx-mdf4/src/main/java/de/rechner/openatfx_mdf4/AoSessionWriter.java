package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.InstanceElement;

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
     * @param mdfChannel The MDF read channel.
     * @return the created AoMeasurement instance element
     * @throws ConvertException Error converting.
     */
    public synchronized InstanceElement writeMea(ODSModelCache modelCache, InstanceElement ieTst,
            SeekableByteChannel mdfChannel) throws AoException, IOException {
        // read and validate header block
        HDBLOCK hdBlock = HDBLOCK.read(mdfChannel);

        // create "AoMeasurement" instance and write descriptive data to instance attributes
        ApplicationElement aeTst = modelCache.getApplicationElement("tst");
        ApplicationElement aeMea = modelCache.getApplicationElement("mea");
        ApplicationRelation relTstMea = modelCache.getApplicationRelation("tst", "mea", "meas");
        // InstanceElement ieMea = aeMea.createInstance(this.resultName);
        // ieTst.createRelation(relTstMea, ieMea);

        // meta information
        BLOCK block = hdBlock.getMdCommentBlock(mdfChannel);
        if (block instanceof TXBLOCK) {
            // System.out.println(((TXBLOCK) block).getTxData());
        } else {
            String mdCommentXML = ((MDBLOCK) block).getMdData();
            // for (int i = 0; i < 10000; i++) {
            this.xmlParser.writeMDCommentToMea(null, mdCommentXML);
            // }
        }

        return null;
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
