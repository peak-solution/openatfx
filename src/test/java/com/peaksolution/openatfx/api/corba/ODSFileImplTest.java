package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Date;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ODSFile;
import org.asam.ods.ODSWriteTransfer;
import org.asam.ods.T_LONGLONG;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.UnitTestFileHandler;
import com.peaksolution.openatfx.util.ODSHelper;


@ExtendWith(GlassfishCorbaExtension.class)
public class ODSFileImplTest {

    private static AoSession aoSession;
    private static ODSFile file;
    private static long fileSize;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ODSFileImplTest.class.getResource("/com/peaksolution/openatfx/external_with_flags.bda");
        File selectedFile = new File(url.getFile());
        fileSize = selectedFile.length();

        url = ODSFileImplTest.class.getResource("/com/peaksolution/openatfx/external_with_flags_aofile.atfx");
        File atfxFile = new File(url.getFile());
        aoSession = AoServiceFactory.getInstance().newAoSession(orb, new UnitTestFileHandler(atfxFile.getParentFile()),
                                                                atfxFile.getAbsolutePath());
        ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
        ApplicationElement aeFile = applicationStructure.getElementByName("ExtCompFile");
        file = aeFile.getInstanceById(ODSHelper.asODSLongLong(1)).upcastODSFile();
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @Test
    void testExists() {
        try {
            assertTrue(file.exists());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testCanRead() {
        try {
            assertTrue(file.canRead());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testCanWrite() {
        try {
            assertTrue(file.canWrite());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetDate() {
        try {
            String dateString = file.getDate();
            Date date = ODSHelper.asJDate(dateString);
            assertNotNull(date);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetSize() {
        try {
            T_LONGLONG size = file.getSize();
            assertEquals(ODSHelper.asJLong(size), fileSize);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testCreate() {
        try {
            ODSWriteTransfer writeTransfer = file.create();
            assertNotNull(writeTransfer);
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
