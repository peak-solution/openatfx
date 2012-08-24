package de.rechner.openatfx.converter.digatron_csv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.TS_Value;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.converter.ConvertException;
import de.rechner.openatfx.converter.IConverter;


/**
 * @author Christian Rechner
 */
public class DigatronCSV2AtfxConverter implements IConverter {

    private static final Log LOG = LogFactory.getLog(DigatronCSV2AtfxConverter.class);
    private static final String ATFX_TEMPLATE_FILE = "/de/rechner/openatfx/converter/model.atfx";
    private static final String CSV_FILE_PATTERN = "*.csv";

    /**
     * Constructor.
     */
    public DigatronCSV2AtfxConverter() {}

    /**
     * @see IConverter
     */
    public void convertFiles(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException {
        convert(sourceFiles, atfxFile, props);
    }

    /**
     * @see IConverter
     */
    public void convertDirectory(File directory, File atfxFile, Properties props) throws ConvertException {
        // collect CSV files
        Collection<File> csvFiles = new ArrayList<File>();
        FileFilter csvFileFilter = new PatternFileFilter(CSV_FILE_PATTERN);
        for (File file : directory.listFiles(csvFileFilter)) {
            csvFiles.add(file);
        }

        convert(csvFiles.toArray(new File[0]), atfxFile, props);
    }

    public void convert(File[] sourceFiles, File atfxFile, Properties props) throws ConvertException {
        long start = System.currentTimeMillis();

        // configure ORB
        ORB orb = ORB.init(new String[0], System.getProperties());

        // open ATFX session on target file
        AoSession aoSession = createSessionFromTemplateAtfx(orb, atfxFile);

        // AoSessionWriter writer = new AoSessionWriter();
        try {
            aoSession.startTransaction();
            aoSession.setContextString("INDENT_XML", "FALSE");

            // create instance of 'AoTest'
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeEnv = as.getElementByName("env");
            ApplicationElement aePrj = as.getElementByName("prj");
            ApplicationRelation relEnvPrj = as.getRelations(aeEnv, aePrj)[0];
            InstanceElement ieEnv = aeEnv.getInstanceById(new T_LONGLONG(0, 1));
            InstanceElement iePrj = aePrj.createInstance("prj");
            ieEnv.createRelation(relEnvPrj, iePrj);

            for (File sourceFile : sourceFiles) {
                DigatronCSVReader csvReader = new DigatronCSVReader(sourceFile);

                Map<String, List<TS_Value>> dataBlock = csvReader.readNextStepDataBlock();
                while (dataBlock != null) {
                    System.out.println(csvReader.getLastReadTestStepName() + " - " + dataBlock);
                    dataBlock = csvReader.readNextStepDataBlock();
                }
                
            }

            aoSession.commitTransaction();

            LOG.info("Performed conversion from Digatron CSV files to ATFX in " + (System.currentTimeMillis() - start)
                    + "ms");
        } catch (AoException e) {
            LOG.error(e.reason, e);
            try {
                aoSession.abortTransaction();
            } catch (AoException ex) {
                LOG.error(ex.reason, ex);
            }
            throw new ConvertException(e.reason, e);
        } catch (ConvertException ce) {
            try {
                aoSession.abortTransaction();
            } catch (AoException ex) {
                LOG.error(ex.reason, ex);
            }
            throw ce;
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

    public static void copyFile(File source, File dest) throws IOException {
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(source);
            FileChannel fic = fi.getChannel();
            MappedByteBuffer mbuf = fic.map(FileChannel.MapMode.READ_ONLY, 0, source.length());
            fo = new FileOutputStream(dest);
            FileChannel foc = fo.getChannel();
            foc.write(mbuf);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            throw ioe;
        } finally {
            if (fi != null) {
                fi.close();
            }
            if (fo != null) {
                fo.close();
            }
        }
    }

}
