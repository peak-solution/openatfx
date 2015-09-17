package de.rechner.openatfx_mdf4;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;

import de.rechner.openatfx.util.ODSHelper;


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
     * Appends the data of an ATFX file to
     * 
     * @param iePrj The parent 'AoTest' instance.
     * @param atfxFile The target ATFX file.
     * @param sourceFile THe source file.
     * @param mdfChannel The MDF read channel.
     * @param binChannel The ATFX binary file channel to write to.
     * @param properties External properties.
     * @return the created AoMeasurement instance element
     * @throws ConvertException Error converting.
     * @throws IOException Error reading file content.
     */
    public synchronized InstanceElement writeDataToAoTest(InstanceElement iePrj, File atfxFile, File sourceFile,
            FileChannel mdfChannel, FileChannel binChannel, Properties properties) throws ConvertException, IOException {
        try {
            ApplicationStructure as = iePrj.getApplicationElement().getApplicationStructure();
            ApplicationElement aePrj = as.getElementByName("prj");
            ApplicationElement aeTst = as.getElementByName("tst");
            ApplicationRelation relPrjTsts = as.getRelations(aePrj, aeTst)[0];

            // read and validate header block
            IDBLOCK.read(mdfChannel);

            // create "AoSubTest" instance
            String fileName = sourceFile.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            InstanceElement ieTst = aeTst.createInstance(fileName);
            iePrj.createRelation(relPrjTsts, ieTst);

            String tplTestStepName = properties.getProperty("tpl_teststep_name", null);
            if (tplTestStepName != null && tplTestStepName.length() > 0) {
                ieTst.addInstanceAttribute(ODSHelper.createStringNVU("tpl_teststep_name", tplTestStepName));
            }

            // write "AoMeasurement" instances
            // return writeMea(ieTst, sourceFile, mdfChannel, binChannel);
            return null;
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        }
    }

}
