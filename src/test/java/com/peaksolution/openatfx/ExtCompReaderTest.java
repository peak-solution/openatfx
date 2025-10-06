package com.peaksolution.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.asam.ods.AIDName;
import org.asam.ods.AggrFunc;
import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.JoinDef;
import org.asam.ods.QueryStructureExt;
import org.asam.ods.SelAIDNameUnitId;
import org.asam.ods.SelItem;
import org.asam.ods.SelOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.corba.InstanceElementImplTest;
import com.peaksolution.openatfx.util.ODSHelper;

@ExtendWith(GlassfishCorbaExtension.class)
class ExtCompReaderTest {

    @Test
	void testExtCompTypes() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        try {
            URL url = InstanceElementImplTest.class.getResource("/com/peaksolution/openatfx/Example_CommonTypespecs.atfx");

        	final AoFactory factory = AoServiceFactory.getInstance().newAoFactory(orb);
            AoSession s = factory.newSession("FILENAME=" + new File(url.getFile()));
            ApplicationStructure as = s.getApplicationStructure();
            assertNotNull(as);
            final ApplicationElement lcE = as.getElementsByBaseType("AoLocalColumn")[0];
            ApplicationAttribute lcValuesA = lcE.getAttributeByBaseName("values");
            final InstanceElementIterator lcIs = lcE.getInstances("*");
            for (int i = 0; i < lcIs.getCount(); i++) {
                InstanceElement lcI = lcIs.nextOne();
                String name = lcI.getValueByBaseName("name").value.u.stringVal();
                System.out.println("LC.Name=" + name);
                if (name.equals("MyMqBoolean")) {
                    // not implemented
                    continue;
                }
                lcI.getValue(lcValuesA.getName());
            }
            s.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
