package com.peaksolution.openatfx.io;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.api.Complex;
import com.peaksolution.openatfx.api.DoubleComplex;


/**
 * Test case for <code>com.peaksolution.openatfx.io.AtfxExportUtilTest</code>.
 * 
 * @author Christian Rechner
 */
class AtfxExportUtilTest {

    @Test
    void testCreateBooleanString() {
        assertEquals("true", AtfxExportUtil.createBooleanString(true));
        assertEquals("false", AtfxExportUtil.createBooleanString(false));
    }

    @Test
    void testCreateBooleanSeqString() {
        assertEquals("true false false true",
                     AtfxExportUtil.createBooleanSeqString(new boolean[] { true, false, false, true }));
    }

    @Test
    void testCreateByteString() {
        assertEquals("18", AtfxExportUtil.createByteString((byte) 18));
        assertEquals("42", AtfxExportUtil.createByteString((byte) 42));
        assertEquals("52", AtfxExportUtil.createByteString((byte) 52));
        assertEquals("222", AtfxExportUtil.createByteString((byte) 222));
    }

    @Test
    void testCreateByteSeqString() {
        assertEquals("18 42 52 222",
                     AtfxExportUtil.createByteSeqString(new byte[] { (byte) 18, (byte) 42, (byte) 52, (byte) 222 }));
    }

    @Test
    void testCreateLongLongString() {
        assertEquals("123456789", AtfxExportUtil.createLongLongString(123456789));
    }

    @Test
    void testCreateLongLongSeqString() {
        assertEquals("10 11 12 13",
                     AtfxExportUtil.createLongLongSeqString(new long[] { 10, 11, 12, 13 }));
    }

    @Test
    void testCreateLongString() {
        assertEquals("123456789", AtfxExportUtil.createLongString(123456789));
    }

    @Test
    void testCreateLongSeqString() {
        assertEquals("10 11 12 13", AtfxExportUtil.createLongSeqString(new int[] { 10, 11, 12, 13 }));
    }

    @Test
    void testCreateShortString() {
        assertEquals("254", AtfxExportUtil.createShortString((short) 254));
    }

    @Test
    void testCreateShortSeqString() {
        assertEquals("10 11 12 13", AtfxExportUtil.createShortSeqString(new short[] { 10, 11, 12, 13 }));
    }

    @Test
    void testCreateFloatString() {
        assertEquals("123.312", AtfxExportUtil.createFloatString(123.312f));
    }

    @Test
    void testCreateFloatSeqString() {
        assertEquals("1.1 2.2 3.3 4.4", AtfxExportUtil.createFloatSeqString(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }));
    }

    @Test
    void testCreateDoubleString() {
        assertEquals("123.312", AtfxExportUtil.createDoubleString(123.312));
    }

    @Test
    void testCreateDoubleSeqString() {
        assertEquals("1.1 2.2 3.3 4.4", AtfxExportUtil.createDoubleSeqString(new double[] { 1.1, 2.2, 3.3, 4.4 }));
    }

    @Test
    void testCreateComplexString() {
        assertEquals("1.1 2.2", AtfxExportUtil.createComplexString(new Complex(1.1f, 2.2f)));
    }

    @Test
    void testCreateComplexSeqString() {
        assertEquals("1.1 2.2 3.4 4.5",
                     AtfxExportUtil.createComplexSeqString(new Complex[] { new Complex(1.1f, 2.2f),
                             new Complex(3.4f, 4.5f) }));
    }

    @Test
    void testCreateDComplexString() {
        assertEquals("1.1 2.2", AtfxExportUtil.createDComplexString(new DoubleComplex(1.1, 2.2)));
    }

    @Test
    void testCreateDComplexSeqString() {
        assertEquals("1.1 2.2 3.4 4.5",
                     AtfxExportUtil.createDComplexSeqString(new DoubleComplex[] { new DoubleComplex(1.1, 2.2),
                             new DoubleComplex(3.4, 4.5) }));
    }

    @Test
    void testCreateDateSeqString() {
        assertEquals("20100101 20110101", AtfxExportUtil.createDateSeqString(new String[] { "20100101", "20110101" }));
    }

}
