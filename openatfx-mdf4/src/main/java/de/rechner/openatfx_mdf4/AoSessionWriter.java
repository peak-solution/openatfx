package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;

import de.rechner.openatfx_mdf4.util.ODSModelCache;


/**
 * Main class for writing the MDF4 file content into an ASAM ODS session backed by an ATFX file.
 * 
 * @author Christian Rechner
 */
class AoSessionWriter {

    private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);

    /**
     * Constructor.
     */
    public AoSessionWriter() {}

    /**
     * Appends the content of the MDF4 file to the ASAM ODS session.
     * 
     * @param modelCache The application model cache.
     * @param iePrj The parent 'AoTest' instance.
     * @param mdfChannel The MDF read channel.
     * @return the created AoMeasurement instance element
     * @throws ConvertException Error converting.
     */
    public synchronized InstanceElement writeDataToAoTest(ODSModelCache modelCache, InstanceElement iePrj,
            SeekableByteChannel mdfChannel) throws AoException, IOException {
        // read and validate header block

        // // create "AoSubTest" instance
        // String fileName = sourceFile.getName();
        // fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        // InstanceElement ieTst = aeTst.createInstance(fileName);
        // iePrj.createRelation(relPrjTsts, ieTst);
        //
        // String tplTestStepName = properties.getProperty("tpl_teststep_name", null);
        // if (tplTestStepName != null && tplTestStepName.length() > 0) {
        // ieTst.addInstanceAttribute(ODSHelper.createStringNVU("tpl_teststep_name", tplTestStepName));
        // }

        // write "AoMeasurement" instances
        // return writeMea(ieTst, sourceFile, mdfChannel, binChannel);
        return null;
    }

}
