package de.rechner.openatfx_mdf4;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;


/**
 * Main class for opening / converting MDF4 files with the ASAM ODS OO-API abstraction layer.
 * 
 * @author Christian Rechner
 */
public class MDF4Reader {

    private static final Log LOG = LogFactory.getLog(MDF4Reader.class);

    private static final String ATFX_TEMPLATE = "model.atfx";

    /**
     * Opens an MDF4-file and gives full access to all its contents via the ASAM ODS OO-API interface.
     * 
     * @param orb The ORB.
     * @param mdfFile The source file.
     * @return The ASAM ODS session object.
     * @throws AoException Error creating ASAM ODS session.
     * @throws IOException Error reading MDF4 file.
     */
    public AoSession getAoSessionForMDF4(ORB orb, Path mdfFile) throws AoException, IOException {
        // copy ATFX file to temporary directory
        File tmpAtfx = File.createTempFile("openatfx-mdf4-", ".atfx");
        tmpAtfx.deleteOnExit();
        copyATFXfromTemplate(tmpAtfx);

        // create new AoSession
        AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, tmpAtfx);

        // read MDF4 file to session
        SeekableByteChannel sbc = null;
        try {
            sbc = Files.newByteChannel(mdfFile, StandardOpenOption.READ);

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            if (sbc != null) {
                sbc.close();
            }
        }
        return aoSession;
    }

    /**
     * Copies the ATFX template file from the classpath to the target file.
     * 
     * @param targetAtfxFile The target file.
     * @throws IOException Error copying file.
     */
    private void copyATFXfromTemplate(File targetAtfxFile) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            // get stream for source file
            InputStream in = getClass().getResourceAsStream(ATFX_TEMPLATE);
            if (in == null) {
                throw new IOException("Unable to read ATFX template file: " + ATFX_TEMPLATE);
            }
            bis = new BufferedInputStream(in);

            // open target stream
            FileOutputStream fos = new FileOutputStream(targetAtfxFile);
            bos = new BufferedOutputStream(fos);

            // perform copy
            int read = 0;
            byte[] bytes = new byte[4096];
            while ((read = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, read);
            }
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
    }

}
