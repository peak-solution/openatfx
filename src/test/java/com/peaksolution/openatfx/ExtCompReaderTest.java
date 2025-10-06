package com.peaksolution.openatfx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.corba.InstanceElementImplTest;

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
                final NameValueUnit values = lcI.getValue(lcValuesA.getName());
                if (name.equals("MyMqBytestrBeo")) {
                    byte[][] vals = values.value.u.bytestrSeq();
                    assertNotNull(vals);
                    byte[][] expected = { { 11, 0, -1, 73 }, { 2, 4, 8, 16, 32, 64, -128 }, { 31, 127 }, { -64 },
                            { 25, 50, 75, 100, 125, -106, -81, -56, -31 } };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                }
                else if (name.equals("MyMqBytestr")) {
                    byte[][] vals = values.value.u.bytestrSeq();
                    assertNotNull(vals);
                    byte[][] expected = { { 11, 0, -1, 73 }, { 2, 4, 8, 16, 32, 64, -128 }, { 31, 127 }, { -64 },
                            { 25, 50, 75, 100, 125, -106, -81, -56, -31 } };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                }
            }
            s.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
