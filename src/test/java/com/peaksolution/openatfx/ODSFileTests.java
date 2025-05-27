package com.peaksolution.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.ODSFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.corba.InstanceElementImplTest;
import com.peaksolution.openatfx.util.ODSHelper;

@ExtendWith(GlassfishCorbaExtension.class)
class ODSFileTests {

    @Test
    void testUpcastODSFile() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = InstanceElementImplTest.class.getResource("/com/peaksolution/openatfx/external_with_flags_aofile.atfx");
        try {
            AoSession localAoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                       .newSession("FILENAME=" + new File(url.getFile()));
            ApplicationStructure applicationStructure = localAoSession.getApplicationStructure();
            ApplicationElement aeFile = applicationStructure.getElementByName("ExtCompFile");
            InstanceElement ieFile = aeFile.getInstanceById(ODSHelper.asODSLongLong(1));
            ODSFile file = ieFile.upcastODSFile();
            assertEquals("external_with_flags.bda", file.getName());
            localAoSession.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
