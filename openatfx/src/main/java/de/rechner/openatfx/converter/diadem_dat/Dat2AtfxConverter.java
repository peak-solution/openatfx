package de.rechner.openatfx.converter.diadem_dat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.converter.ConvertException;
import de.rechner.openatfx.converter.IConverter;


public class Dat2AtfxConverter implements IConverter {

    private static final Log LOG = LogFactory.getLog(Dat2AtfxConverter.class);
    private static final String ATFX_TEMPLATE_FILE = "model.atfx";

    public void convert(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException {
        long start = System.currentTimeMillis();

        // configure ORB
        ORB orb = ORB.init(new String[0], System.getProperties());

        // open ATFX session on target file
        AoSession aoSession = createSessionFromTemplateAtfx(orb, atfxFile);

        DatHeaderReader datHeaderReader = DatHeaderReader.getInstance();
        AoSessionWriter writer = new AoSessionWriter();
        try {
            aoSession.startTransaction();

            // create instance of 'AoTest'
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeEnv = as.getElementByName("env");
            ApplicationElement aePrj = as.getElementByName("prj");
            ApplicationRelation relEnvPrj = as.getRelations(aeEnv, aePrj)[0];
            InstanceElement ieEnv = aeEnv.getInstanceById(new T_LONGLONG(0, 1));
            InstanceElement iePrj = aePrj.createInstance("prj");
            ieEnv.createRelation(relEnvPrj, iePrj);

            // create an 'AoSubTest' instance for each DAT file
            for (File datFile : sourceFiles) {
                DatHeader datHeader = datHeaderReader.readFile(datFile);
                writer.writeDataToAoTest(iePrj, atfxFile, datHeader);
            }

            aoSession.commitTransaction();

            LOG.info("Performed conversion from DIAdem DAT files to ATFX in " + (System.currentTimeMillis() - start)
                    + "ms");
        } catch (AoException e) {
            LOG.error(e.reason, e);
            try {
                aoSession.abortTransaction();
            } catch (AoException ex) {
                LOG.error(ex.reason, ex);
            }
            throw new ConvertException(e.reason, e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            try {
                aoSession.abortTransaction();
            } catch (AoException ex) {
                LOG.error(ex.reason, ex);
            }
            throw new ConvertException(e.getMessage(), e);
        } catch (ConvertException e) {
            LOG.error(e.getMessage(), e);
            try {
                aoSession.abortTransaction();
            } catch (AoException ex) {
                LOG.error(ex.reason, ex);
            }
            throw e;
        } finally {
            if (aoSession != null) {
                try {
                    aoSession.close();
                } catch (AoException e) {
                    LOG.error(e.reason, e);
                }
            }
        }
    }

    /**
     * Creates an new AoSession by copying the template ATFX file to specified target file.
     * 
     * @param orb The ORB.
     * @param targetAtfxFile The target file.
     * @return The session.
     * @throws ConvertException An error occurred.
     */
    private AoSession createSessionFromTemplateAtfx(ORB orb, File targetAtfxFile) throws ConvertException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            // open template file
            InputStream in = getClass().getResourceAsStream(ATFX_TEMPLATE_FILE);
            if (in == null) {
                throw new ConvertException("Unable to open ATFX template file: " + ATFX_TEMPLATE_FILE);
            }
            bis = new BufferedInputStream(in);

            // create target directories
            if (!targetAtfxFile.getParentFile().exists() && !targetAtfxFile.getParentFile().mkdirs()) {
                throw new ConvertException("Unable to create directories for file: " + targetAtfxFile.getAbsolutePath());
            }
            if (!targetAtfxFile.createNewFile()) {
                LOG.warn("File '" + targetAtfxFile.getAbsolutePath() + "' already exists and will be overridden!");
            }

            FileOutputStream fos = new FileOutputStream(targetAtfxFile);
            bos = new BufferedOutputStream(fos);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, read);
            }

        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            throw new ConvertException(ioe.getMessage(), ioe);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }

        // create new Session
        try {
            return AoServiceFactory.getInstance().newAoSession(orb, targetAtfxFile);
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

}
