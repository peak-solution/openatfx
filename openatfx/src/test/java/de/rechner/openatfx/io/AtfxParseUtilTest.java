package de.rechner.openatfx.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.junit.Test;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxParseUtil</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxParseUtilTest {

    @Test
    public void testParseBoolean() {
        try {
            assertEquals(true, AtfxParseUtil.parseBoolean("true"));
            assertEquals(true, AtfxParseUtil.parseBoolean("True"));
            assertEquals(true, AtfxParseUtil.parseBoolean("TRUE"));
            assertEquals(true, AtfxParseUtil.parseBoolean("1"));
            assertEquals(false, AtfxParseUtil.parseBoolean("false"));
            assertEquals(false, AtfxParseUtil.parseBoolean("False"));
            assertEquals(false, AtfxParseUtil.parseBoolean("FALSE"));
            assertEquals(false, AtfxParseUtil.parseBoolean("0"));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseBoolean(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseBoolean("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseBooleanSeq() {
        try {
            assertEquals(true,
                         Arrays.equals(new boolean[] { true, true, false, true, false },
                                       AtfxParseUtil.parseBooleanSeq(" true 1  FALSE 1  0")));
            assertEquals(true, Arrays.equals(new boolean[0], AtfxParseUtil.parseBooleanSeq(" ")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseLongLong() {
        try {
            assertEquals(-2147483648, ODSHelper.asJLong(AtfxParseUtil.parseLongLong("-2147483648")));
            assertEquals(2147483647, ODSHelper.asJLong(AtfxParseUtil.parseLongLong("2147483647")));
            assertEquals(0, ODSHelper.asJLong(AtfxParseUtil.parseLongLong("0")));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseLongLong(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseLong("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseLongLongSeq() {
        try {
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5 },
                              ODSHelper.asJLong(AtfxParseUtil.parseLongLongSeq("  1 2 3  4 5")));
            assertArrayEquals(new long[0], ODSHelper.asJLong(AtfxParseUtil.parseLongLongSeq(" ")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseLong() {
        try {
            assertEquals(-2147483648, AtfxParseUtil.parseLong("-2147483648"));
            assertEquals(2147483647, AtfxParseUtil.parseLong("2147483647"));
            assertEquals(0, AtfxParseUtil.parseLong("0"));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseLong(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseLong("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseLongSeq() {
        try {
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, AtfxParseUtil.parseLongSeq("  1 2 3  4 5"));
            assertArrayEquals(new int[0], AtfxParseUtil.parseLongSeq(" "));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseFloat() {
        try {
            assertEquals(-123.456, AtfxParseUtil.parseFloat(" -123.456"), 0.00001);
            assertEquals(65432.1, AtfxParseUtil.parseFloat("65432.1"), 0.01);
            assertEquals(0, AtfxParseUtil.parseFloat("0"), 0.0000000001);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseFloat(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseFloat("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseFloatSeq() {
        try {
            assertArrayEquals("fail", new float[] { 1.1f, 2.2f, 3.3f }, AtfxParseUtil.parseFloatSeq("  1.1 2.2 3.3"),
                              0.01f);
            assertArrayEquals(new int[0], AtfxParseUtil.parseLongSeq(" "));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseDouble() {
        try {
            assertEquals(-123.456, AtfxParseUtil.parseDouble(" -123.456"), 0.00001);
            assertEquals(65432.1, AtfxParseUtil.parseDouble("65432.1"), 0.01);
            assertEquals(0, AtfxParseUtil.parseDouble("0"), 0.0000000001);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseDouble(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseDouble("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseDoubleSeq() {
        try {
            assertArrayEquals("fail", new double[] { 1.1, 2.2, 3.3 }, AtfxParseUtil.parseDoubleSeq("  1.1 2.2 3.3"),
                              0.01);
            assertArrayEquals(new int[0], AtfxParseUtil.parseLongSeq(" "));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseShort() {
        try {
            assertEquals(-32768, AtfxParseUtil.parseShort("-32768"));
            assertEquals(32767, AtfxParseUtil.parseShort("32767"));
            assertEquals(0, AtfxParseUtil.parseShort("0"));
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseShort(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseShort("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseShortSeq() {
        try {
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, AtfxParseUtil.parseShortSeq("  1 2 3  4 5"));
            assertArrayEquals(new short[0], AtfxParseUtil.parseShortSeq(" "));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseByte() {
        try {
            assertEquals((byte) 18, AtfxParseUtil.parseByte("18"));
            assertEquals((byte) 42, AtfxParseUtil.parseByte("42"));
            assertEquals((byte) 52, AtfxParseUtil.parseByte("52"));
            assertEquals((byte) 222, AtfxParseUtil.parseByte("222"));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseByteSeq() {
        try {
            assertArrayEquals(new byte[] { 1, 2, 3, 4, (byte) 222 }, AtfxParseUtil.parseByteSeq("  1 2 3  4 222"));
            assertArrayEquals(new byte[0], AtfxParseUtil.parseByteSeq(" "));
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    public void testParseComplex() {
        try {
            assertEquals(1.1, AtfxParseUtil.parseComplex(" 1.1   2.2 ").r, 0.00001);
            assertEquals(2.2, AtfxParseUtil.parseComplex(" 1.1   2.2 ").i, 0.00001);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseComplex(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseComplex("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseComplexSeq() {
        try {
            assertEquals(2, AtfxParseUtil.parseComplexSeq("  1  2 4  5").length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(0, AtfxParseUtil.parseComplexSeq("").length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(2, AtfxParseUtil.parseComplexSeq("  1 2  2 4  5").length);
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(2, AtfxParseUtil.parseComplexSeq("  1 2a x 2 4  5").length);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseDComplex() {
        try {
            assertEquals(1.1, AtfxParseUtil.parseDComplex(" 1.1   2.2 ").r, 0.00001);
            assertEquals(2.2, AtfxParseUtil.parseDComplex(" 1.1   2.2 ").i, 0.00001);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(false, AtfxParseUtil.parseDComplex(""));
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(false, AtfxParseUtil.parseDComplex("asd"));
        } catch (AoException e) {
        }
    }

    @Test
    public void testParseDComplexSeq() {
        try {
            assertEquals(2, AtfxParseUtil.parseDComplexSeq("  1  2 4  5").length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(0, AtfxParseUtil.parseDComplexSeq(" ").length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            assertEquals(2, AtfxParseUtil.parseDComplexSeq("  1 2  2 4  5").length);
            fail("AoException expected");
        } catch (AoException e) {
        }
        try {
            assertEquals(2, AtfxParseUtil.parseDComplexSeq("  1 2a x 2 4  5").length);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(AtfxParseUtilTest.class);
    }

}
