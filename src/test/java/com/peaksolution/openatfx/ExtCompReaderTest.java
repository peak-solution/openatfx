package com.peaksolution.openatfx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static long asJLong(org.asam.ods.T_LONGLONG ll) {
        long tmp;
        if (ll.low >= 0) {
            tmp = (long) ll.high * 0x100000000L + (long) ll.low;
        } else {
            tmp = (long) (ll.high + 1) * 0x100000000L + (long) ll.low;
        }
        return (tmp);
    }

    private static long[] asJLong(org.asam.ods.T_LONGLONG[] ll) {
        long[] ar = new long[ll.length];
        for (int i = 0; i < ll.length; i++) {
            ar[i] = asJLong(ll[i]);
        }
        return ar;
    }

    private static float[] asJFloat(org.asam.ods.T_COMPLEX[] c) {
        float[] ar = new float[c.length * 2];
        for (int i = 0; i < c.length; i++) {
            ar[i * 2] = c[i].r;
            ar[i * 2 + 1] = c[i].i;
        }
        return ar;
    }

    private static double[] asJDouble(org.asam.ods.T_DCOMPLEX[] c) {
        double[] ar = new double[c.length * 2];
        for (int i = 0; i < c.length; i++) {
            ar[i * 2] = c[i].r;
            ar[i * 2 + 1] = c[i].i;
        }
        return ar;
    }

    @Test
    void testExtCompTypes() {
        ORB orb = ORB.init(new String[0], System.getProperties());
        try {
            URL url = InstanceElementImplTest.class
                    .getResource("/com/peaksolution/openatfx/Example_CommonTypespecs.atfx");

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
                    // reading bool is currently not implemented but should not end up in endless
                    // loop
                    AoException e = assertThrows(AoException.class, () -> lcI.getValue(lcValuesA.getName()));
                    org.junit.Assert.assertEquals(org.asam.ods.ErrorCode.AO_NOT_IMPLEMENTED, e.errCode);
                    continue;
                }
                final NameValueUnit values = lcI.getValue(lcValuesA.getName());
                if (name.equals("MyMqBytestrBeo") || name.equals("MyMqBytestr")) {
                    byte[][] vals = values.value.u.bytestrSeq();
                    assertNotNull(vals);
                    byte[][] expected = { { 11, 0, -1, 73 }, { 2, 4, 8, 16, 32, 64, -128 }, { 31, 127 }, { -64 },
                            { 25, 50, 75, 100, 125, -106, -81, -56, -31 } };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqByte")) {
                    byte[] vals = values.value.u.byteSeq();
                    assertNotNull(vals);
                    byte[] expected = { 1, 2, 3, 0, -1 };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqShort") || name.equals("MyMqShortBeo")) {
                    short[] vals = values.value.u.shortSeq();
                    assertNotNull(vals);
                    short[] expected = { 10, 20, 30, Short.MIN_VALUE, Short.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqLong") || name.equals("MyMqLongBeo")) {
                    int[] vals = values.value.u.longSeq();
                    assertNotNull(vals);
                    int[] expected = { 100, 200, 300, Integer.MIN_VALUE, Integer.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqLonglong") || name.equals("MyMqLonglongBeo")) {
                    long[] vals = asJLong(values.value.u.longlongSeq());
                    assertNotNull(vals);
                    long[] expected = { 1000, 2000, 3000, Long.MIN_VALUE, Long.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqFloat") || name.equals("MyMqFloatBeo")) {
                    float[] vals = values.value.u.floatSeq();
                    assertNotNull(vals);
                    float[] expected = { 123.456f, 789.012f, -3333.f, Float.MIN_VALUE, Float.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals, 0.00001f);
                } else if (name.equals("MyMqDouble") || name.equals("MyMqDoubleBeo")) {
                    double[] vals = values.value.u.doubleSeq();
                    assertNotNull(vals);
                    double[] expected = { 456.789012, 345.678901, -6666666., Double.MIN_VALUE, Double.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals, 0.00001);
                } else if (name.equals("MyMqComplex") || name.equals("MyMqComplexBeo")) {
                    float[] vals = asJFloat(values.value.u.complexSeq());
                    assertNotNull(vals);
                    float[] expected = { 1.1f, 0.1f, 2.2f, 1.2f, -3.3f, -2.3f, Float.MIN_VALUE, Float.MIN_VALUE,
                            Float.MAX_VALUE, Float.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals, 0.00001f);
                } else if (name.equals("MyMqDcomplex") || name.equals("MyMqDcomplexBeo")) {
                    double[] vals = asJDouble(values.value.u.dcomplexSeq());
                    assertNotNull(vals);
                    double[] expected = { 1.11, 0.11, 2.22, 1.22, -3.33, -2.33, Double.MIN_VALUE, Double.MIN_VALUE,
                            Double.MAX_VALUE, Double.MAX_VALUE };
                    org.junit.Assert.assertArrayEquals(expected, vals, 0.00001);
                } else if (name.equals("MyMqDate")) {
                    String[] vals = values.value.u.dateSeq();
                    assertNotNull(vals);
                    String[] expected = { "20050130121532123789", "20050129115315", "2010", "201112", "201403040802" };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else if (name.equals("MyMqString")) {
                    String[] vals = values.value.u.stringSeq();
                    assertNotNull(vals);
                    String[] expected = { "val1", "val2", "val3", "val4", "val5" };
                    org.junit.Assert.assertArrayEquals(expected, vals);
                } else {
                    fail("Unknown LocalColumn name: " + name);
                }
            }
            s.close();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

}
