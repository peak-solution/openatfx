package de.rechner.openatfx_mdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoFactoryHelper;
import org.asam.ods.AoSession;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.IFileHandler;
import de.rechner.openatfx_mdf.util.FileUtil;
import de.rechner.openatfx_mdf.util.ODSModelCache;


/**
 * Main class for opening / converting MDF4 files with the ASAM ODS OO-API abstraction layer.
 * 
 * @author Christian Rechner
 */
public class MDFConverter {

    private static final Log LOG = LogFactory.getLog(MDFConverter.class);

    private static final String ATFX_TEMPLATE = "model.atfx";

    /**
     * Creates a new AoFactory that may be used to open new MDF4 files on the fly.
     * 
     * @param orb The ORB.
     * @return The AoFactory instance.
     * @throws AoException Error creating factory.
     */
    public AoFactory newAoFactory(ORB orb) throws AoException {
        try {
            // get reference to rootpoa & activate the POAManager
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            AoFactoryImpl aoFactoryImpl = new AoFactoryImpl(orb);
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(aoFactoryImpl);
            AoFactory aoFactory = AoFactoryHelper.narrow(ref);

            return aoFactory;
        } catch (InvalidName e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterInactive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Writes the ATFX header file for given ATFX.<br/>
     * The file will get the same file name as the MDF file with suffix '.atfx'.<br/>
     * If the file already exists
     * 
     * @param orb The ORB.
     * @param mdfPath The source file, may point to a MDF3 or MDF4 file.
     * @throws ConvertException
     */
    public void writeATFXHeader(ORB orb, Path mdfPath) throws ConvertException {
        if (orb == null) {
            throw new ConvertException("orb must not be null!");
        }
        if (mdfPath == null) {
            throw new ConvertException("mdfFile must not be null!");
        }

        long start = System.currentTimeMillis();
        AoSession aoSession = null;
        SeekableByteChannel sbc = null;
        try {
            // obtain target file name, overwrite if existing
            File mdfFile = mdfPath.toFile();
            String fileName = FileUtil.getFileNameWithoutExtension(mdfFile.getName()) + ".atfx";
            File targetAtfxFile = new File(mdfFile.getParentFile(), fileName);
            copyATFXfromTemplate(targetAtfxFile);

            // create new AoSession
            aoSession = AoServiceFactory.getInstance().newAoSession(orb, targetAtfxFile);
            aoSession.setContextString("WRITE_EXTERNALCOMPONENTS", "TRUE");
            aoSession.setContextString("write_mode", "database");
            ODSModelCache modelCache = new ODSModelCache(aoSession);
            aoSession.startTransaction();

            // open MDF file
            sbc = Files.newByteChannel(mdfPath, StandardOpenOption.READ);

            // check whether MDF3 or MDF4 and write MDF content to session
            String version = readMDFVersion(sbc);
            if (version.startsWith("3")) {
                de.rechner.openatfx_mdf.mdf3.AoSessionWriter writer = new de.rechner.openatfx_mdf.mdf3.AoSessionWriter();
                de.rechner.openatfx_mdf.mdf3.IDBLOCK idBlock = de.rechner.openatfx_mdf.mdf3.IDBLOCK.read(mdfPath, sbc);
                writer.writeTst(modelCache, idBlock);
            } else if (version.startsWith("4")) {
                de.rechner.openatfx_mdf.mdf4.AoSessionWriter writer = new de.rechner.openatfx_mdf.mdf4.AoSessionWriter();
                de.rechner.openatfx_mdf.mdf4.IDBLOCK idBlock = de.rechner.openatfx_mdf.mdf4.IDBLOCK.read(mdfPath, sbc);
                writer.writeTst(modelCache, idBlock);
            }

            aoSession.commitTransaction();

            LOG.info("Wrote ATFX header '" + targetAtfxFile + "' in " + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } finally {
            if (sbc != null) {
                try {
                    sbc.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw new ConvertException(e.getMessage(), e);
                }
            }
            if (aoSession != null) {
                try {
                    aoSession.close();
                } catch (AoException e) {
                    LOG.error(e.reason, e);
                    throw new ConvertException(e.reason, e);
                }
            }
        }
    }

    /**
     * Opens an MDF file and gives full access to all its contents via the ASAM ODS OO-API interface.
     * 
     * @param orb The ORB.
     * @param mdfPath The source file, may point to a MDF3 or MDF4 file.
     * @return The ASAM ODS session object.
     * @throws ConvertException Error opening the session.
     */
    public AoSession getAoSessionForMDF(ORB orb, Path mdfPath) throws ConvertException {
        if (orb == null) {
            throw new ConvertException("orb must not be null!");
        }
        if (mdfPath == null) {
            throw new ConvertException("mdfFile must not be null!");
        }

        long start = System.currentTimeMillis();
        SeekableByteChannel sbc = null;
        try {
            // create new AoSession
            IFileHandler fileHandler = new TmpFileHandler();
            AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, fileHandler, mdfPath.toString());
            ODSModelCache modelCache = new ODSModelCache(aoSession);
            aoSession.setContextString("WRITE_EXTERNALCOMPONENTS", "TRUE");
            aoSession.setContextString("write_mode", "database");

            // open MDF file
            sbc = Files.newByteChannel(mdfPath, StandardOpenOption.READ);

            // check whether MDF3 or MDF4
            String version = readMDFVersion(sbc);
            if (version.startsWith("3")) {
                de.rechner.openatfx_mdf.mdf3.AoSessionWriter writer = new de.rechner.openatfx_mdf.mdf3.AoSessionWriter();
                de.rechner.openatfx_mdf.mdf3.IDBLOCK idBlock = de.rechner.openatfx_mdf.mdf3.IDBLOCK.read(mdfPath, sbc);
                writer.writeTst(modelCache, idBlock);
            } else if (version.startsWith("4")) {
                de.rechner.openatfx_mdf.mdf4.AoSessionWriter writer = new de.rechner.openatfx_mdf.mdf4.AoSessionWriter();
                de.rechner.openatfx_mdf.mdf4.IDBLOCK idBlock = de.rechner.openatfx_mdf.mdf4.IDBLOCK.read(mdfPath, sbc);
                writer.writeTst(modelCache, idBlock);
            }

            LOG.info("Read MDF header in " + (System.currentTimeMillis() - start) + "ms");
            return aoSession;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } finally {
            if (sbc != null) {
                try {
                    sbc.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    throw new ConvertException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Reads the MDF version string from the byte channel.
     * 
     * @param sbc The byte channel.
     * @return The MDF version string.
     * @throws IOException Error reading from channel.
     */
    private static String readMDFVersion(SeekableByteChannel sbc) throws IOException {
        // read block
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(0);
        sbc.read(bb);
        bb.rewind();

        // CHAR 8: File identifier
        byte[] b = new byte[8];
        bb.get(b);
        String fileIdent = new String(b, "ISO-8859-1");
        if (!fileIdent.equals("MDF     ")) {
            throw new IOException("Invalid or corrupt MDF file: " + fileIdent);
        }

        // CHAR 8: Format identifier
        b = new byte[8];
        bb.get(b);
        return new String(b, "ISO-8859-1");
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
