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
import org.asam.ods.AoFactory;
import org.asam.ods.AoFactoryHelper;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx_mdf4.util.ODSModelCache;


/**
 * Main class for opening / converting MDF4 files with the ASAM ODS OO-API abstraction layer.
 * 
 * @author Christian Rechner
 */
public class MDF4Converter {

    private static final Log LOG = LogFactory.getLog(MDF4Converter.class);

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
     * Opens an MDF4-file and gives full access to all its contents via the ASAM ODS OO-API interface.
     * 
     * @param orb The ORB.
     * @param mdfFile The source file.
     * @return The ASAM ODS session object.
     * @throws AoException Error creating ASAM ODS session.
     * @throws IOException Error reading MDF4 file.
     */
    public AoSession getAoSessionForMDF4(ORB orb, Path mdfFile) throws ConvertException {
        SeekableByteChannel sbc = null;
        try {
            // copy ATFX file to temporary directory
            File tmpAtfx = File.createTempFile("openatfx-mdf4-", ".atfx");
            tmpAtfx.deleteOnExit();
            copyATFXfromTemplate(tmpAtfx);

            // create new AoSession
            AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, tmpAtfx);
            ODSModelCache modelCache = new ODSModelCache(aoSession);

            // open MDF4 file
            sbc = Files.newByteChannel(mdfFile, StandardOpenOption.READ);
            // TODO: check performance difference from direct channel
            // MappedByteBuffer mbb = sbc.map(FileChannel.MapMode.READ_WRITE, 0, 100);

            // write MDF4 file content to session
            ApplicationElement aeEnv = modelCache.getApplicationElement("env");
            ApplicationElement aeTst = modelCache.getApplicationElement("tst");
            ApplicationRelation relEnvPrj = modelCache.getApplicationRelation("env", "tst", "tsts");
            InstanceElement ieEnv = aeEnv.getInstanceById(new T_LONGLONG(0, 1));
            InstanceElement ieTst = aeTst.createInstance(mdfFile.getFileName().toString());
            ieEnv.createRelation(relEnvPrj, ieTst);

            // read and validate id block
            IDBLOCK idBlock = IDBLOCK.read(sbc);
            NameValueUnit[] nvu = new NameValueUnit[6];
            nvu[0] = ODSHelper.createStringNVU("mdf_file_id", idBlock.getIdFile());
            nvu[1] = ODSHelper.createStringNVU("mdf_version_str", idBlock.getIdVers());
            nvu[2] = ODSHelper.createLongNVU("mdf_version", idBlock.getIdVer());
            nvu[3] = ODSHelper.createStringNVU("mdf_program", idBlock.getIdProg());
            nvu[4] = ODSHelper.createLongNVU("mdf_unfin_flags", idBlock.getIdUnfinFlags());
            nvu[5] = ODSHelper.createLongNVU("mdf_custom_unfin_flags", idBlock.getIdCustomUnfinFlags());
            ieTst.setValueSeq(nvu);

            // write data to AoTest
            AoSessionWriter writer = new AoSessionWriter();
            writer.writeDataToAoTest(modelCache, ieTst, sbc);

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
