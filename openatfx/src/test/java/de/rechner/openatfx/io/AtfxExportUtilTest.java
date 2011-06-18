package de.rechner.openatfx.io;

import static org.junit.Assert.assertEquals;

import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.junit.Test;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxExportUtilTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxExportUtilTest {

    @Test
    public void testCreateBooleanString() {
        assertEquals("true", AtfxExportUtil.createBooleanString(true));
        assertEquals("false", AtfxExportUtil.createBooleanString(false));
    }

    @Test
    public void testCreateBooleanSeqString() {
        assertEquals("true false false true",
                     AtfxExportUtil.createBooleanSeqString(new boolean[] { true, false, false, true }));
    }

    @Test
    public void testCreateByteString() {
        assertEquals("18", AtfxExportUtil.createByteString((byte) 18));
        assertEquals("42", AtfxExportUtil.createByteString((byte) 42));
        assertEquals("52", AtfxExportUtil.createByteString((byte) 52));
        assertEquals("222", AtfxExportUtil.createByteString((byte) 222));
    }

    @Test
    public void testCreateByteSeqString() {
        assertEquals("18 42 52 222",
                     AtfxExportUtil.createByteSeqString(new byte[] { (byte) 18, (byte) 42, (byte) 52, (byte) 222 }));
    }

    @Test
    public void testCreateLongLongString() {
        assertEquals("123456789", AtfxExportUtil.createLongLongString(ODSHelper.asODSLongLong(123456789)));
    }

    @Test
    public void testCreateLongLongSeqString() {
        assertEquals("10 11 12 13",
                     AtfxExportUtil.createLongLongSeqString(ODSHelper.asODSLongLong(new long[] { 10, 11, 12, 13 })));
    }

    @Test
    public void testCreateLongString() {
        assertEquals("123456789", AtfxExportUtil.createLongString(123456789));
    }

    @Test
    public void testCreateLongSeqString() {
        assertEquals("10 11 12 13", AtfxExportUtil.createLongSeqString(new int[] { 10, 11, 12, 13 }));
    }

    @Test
    public void testCreateShortString() {
        assertEquals("254", AtfxExportUtil.createShortString((short) 254));
    }

    @Test
    public void testCreateShortSeqString() {
        assertEquals("10 11 12 13", AtfxExportUtil.createShortSeqString(new short[] { 10, 11, 12, 13 }));
    }

    @Test
    public void testCreateFloatString() {
        assertEquals("123.312", AtfxExportUtil.createFloatString(123.312f));
    }

    @Test
    public void testCreateFloatSeqString() {
        assertEquals("1.1 2.2 3.3 4.4", AtfxExportUtil.createFloatSeqString(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }));
    }

    @Test
    public void testCreateDoubleString() {
        assertEquals("123.312", AtfxExportUtil.createDoubleString(123.312));
    }

    @Test
    public void testCreateDoubleSeqString() {
        assertEquals("1.1 2.2 3.3 4.4", AtfxExportUtil.createDoubleSeqString(new double[] { 1.1, 2.2, 3.3, 4.4 }));
    }

    @Test
    public void testCreateComplexString() {
        assertEquals("1.1 2.2", AtfxExportUtil.createComplexString(new T_COMPLEX(1.1f, 2.2f)));
    }

    @Test
    public void testCreateComplexSeqString() {
        assertEquals("1.1 2.2 3.4 4.5",
                     AtfxExportUtil.createComplexSeqString(new T_COMPLEX[] { new T_COMPLEX(1.1f, 2.2f),
                             new T_COMPLEX(3.4f, 4.5f) }));
    }

    @Test
    public void testCreateDComplexString() {
        assertEquals("1.1 2.2", AtfxExportUtil.createDComplexString(new T_DCOMPLEX(1.1, 2.2)));
    }

    @Test
    public void testCreateDComplexSeqString() {
        assertEquals("1.1 2.2 3.4 4.5",
                     AtfxExportUtil.createDComplexSeqString(new T_DCOMPLEX[] { new T_DCOMPLEX(1.1, 2.2),
                             new T_DCOMPLEX(3.4, 4.5) }));
    }

    @Test
    public void testCreateDateSeqString() {
        assertEquals("20100101 20110101", AtfxExportUtil.createDateSeqString(new String[] { "20100101", "20110101" }));
    }

}
