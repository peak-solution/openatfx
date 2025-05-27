package com.peaksolution.openatfx.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.api.OpenAtfxException;


/**
 * Test case for <code>com.peaksolution.openatfx.io.AtfxParseUtil</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
class AtfxParseUtilTest {

    @Test
    void testParseBoolean() {
        assertThat(AtfxParseUtil.parseBoolean("true")).isTrue();
        assertThat(AtfxParseUtil.parseBoolean("True")).isTrue();
        assertThat(AtfxParseUtil.parseBoolean("TRUE")).isTrue();
        assertThat(AtfxParseUtil.parseBoolean("1")).isTrue();
        assertThat(AtfxParseUtil.parseBoolean("false")).isFalse();
        assertThat(AtfxParseUtil.parseBoolean("False")).isFalse();
        assertThat(AtfxParseUtil.parseBoolean("FALSE")).isFalse();
        assertThat(AtfxParseUtil.parseBoolean("0")).isFalse();
        assertThat(AtfxParseUtil.parseBoolean("")).isFalse();
        assertThat(AtfxParseUtil.parseBoolean("asd")).isFalse();
    }

    @Test
    void testParseBooleanSeq() {
        assertThat(AtfxParseUtil.parseBooleanSeq(" true 1  FALSE 1  0")).containsExactly(new boolean[] { true, true, false, true, false });
        assertThat(AtfxParseUtil.parseBooleanSeq(" ")).containsExactly(new boolean[0]);
    }

    @Test
    void testParseLongLong() {
        assertThat((long)AtfxParseUtil.parseLongLong("-2147483648")).isEqualTo(-2147483648);
        assertThat((long)AtfxParseUtil.parseLongLong("2147483647")).isEqualTo(2147483647);
        assertThat((long)AtfxParseUtil.parseLongLong("0")).isZero();
        assertThat(AtfxParseUtil.parseLongLong("")).isNull();
        try {
            AtfxParseUtil.parseLongLong("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseLongLongSeq() {
        assertThat(AtfxParseUtil.parseLongLongSeq("  1 2 3  4 5")).containsExactly(new long[] { 1, 2, 3, 4, 5 });
        assertThat(AtfxParseUtil.parseLongLongSeq(" ")).containsExactly(new long[0]);
    }

    @Test
    void testParseLong() {
        assertThat(AtfxParseUtil.parseLong("-2147483648")).isEqualTo(-2147483648);
        assertThat(AtfxParseUtil.parseLong("2147483647")).isEqualTo(2147483647);
        assertThat(AtfxParseUtil.parseLong("0")).isZero();
        assertThat(AtfxParseUtil.parseLong("")).isNull();
        try {
            AtfxParseUtil.parseLong("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseLongSeq() {
        assertThat(AtfxParseUtil.parseLongSeq("  1 2 3  4 5")).containsExactly(new int[] { 1, 2, 3, 4, 5 });
        assertThat(AtfxParseUtil.parseLongSeq(" ")).containsExactly(new int[0]);
    }

    @Test
    void testParseFloat() {
        assertThat(AtfxParseUtil.parseFloat(" -123.456")).isEqualTo(-123.456f);
        assertThat(AtfxParseUtil.parseFloat("65432.1")).isEqualTo(65432.1f);
        assertThat(AtfxParseUtil.parseFloat("0")).isZero();
        assertThat(AtfxParseUtil.parseFloat("")).isNull();
        try {
            AtfxParseUtil.parseFloat("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseFloatSeq() {
        assertThat(AtfxParseUtil.parseFloatSeq("  1.1 2.2 3.3")).containsExactly(new float[] { 1.1f, 2.2f, 3.3f });
        assertThat(AtfxParseUtil.parseFloatSeq(" ")).containsExactly(new float[0]);
    }

    @Test
    void testParseDouble() {
        assertThat(AtfxParseUtil.parseDouble(" -123.456")).isEqualTo(-123.456);
        assertThat(AtfxParseUtil.parseDouble("65432.1")).isEqualTo(65432.1);
        assertThat(AtfxParseUtil.parseDouble("0")).isZero();
        assertThat(AtfxParseUtil.parseDouble("")).isNull();
        try {
            AtfxParseUtil.parseDouble("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseDoubleSeq() {
        assertThat(AtfxParseUtil.parseDoubleSeq("  1.1 2.2 3.3")).containsExactly(new double[] { 1.1, 2.2, 3.3 });
        assertThat(AtfxParseUtil.parseDoubleSeq(" ")).containsExactly(new double[0]);
    }

    @Test
    void testParseShort() {
        assertThat(AtfxParseUtil.parseShort("-32768")).isEqualTo((short)-32768);
        assertThat(AtfxParseUtil.parseShort("32767")).isEqualTo((short)32767);
        assertThat(AtfxParseUtil.parseShort("0")).isZero();
        assertThat(AtfxParseUtil.parseShort("")).isNull();
        try {
            AtfxParseUtil.parseShort("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseShortSeq() {
        assertThat(AtfxParseUtil.parseShortSeq("  1 2 3  4 5")).containsExactly(new short[] { 1, 2, 3, 4, 5 });
        assertThat(AtfxParseUtil.parseShortSeq(" ")).containsExactly(new short[0]);
    }

    @Test
    void testParseByte() {
        assertThat(AtfxParseUtil.parseByte("18")).isEqualTo((byte) 18);
        assertThat(AtfxParseUtil.parseByte("42")).isEqualTo((byte) 42);
        assertThat(AtfxParseUtil.parseByte("52")).isEqualTo((byte) 52);
        assertThat(AtfxParseUtil.parseByte("222")).isEqualTo((byte) 222);
    }

    @Test
    void testParseByteSeq() {
        assertThat(AtfxParseUtil.parseByteSeq("  1 2 3  4 222")).containsExactly(new byte[] { 1, 2, 3, 4, (byte) 222 });
        assertThat(AtfxParseUtil.parseByteSeq(" ")).containsExactly(new byte[0]);
    }

    @Test
    void testParseComplex() {
        assertThat(AtfxParseUtil.parseComplex(" 1.1   2.2 ").getR()).isEqualTo(1.1f);
        assertThat(AtfxParseUtil.parseComplex(" 1.1   2.2 ").getI()).isEqualTo(2.2f);
        assertThat(AtfxParseUtil.parseComplex("")).isNull();
        try {
            AtfxParseUtil.parseComplex("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseComplexSeq() {
        assertThat(AtfxParseUtil.parseComplexSeq("  1  2 4  5")).hasSize(2);
        assertThat(AtfxParseUtil.parseComplexSeq("")).isEmpty();
        try {
            AtfxParseUtil.parseComplexSeq("  1 2  2 4  5");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
        try {
            AtfxParseUtil.parseComplexSeq("  1 2a x 2 4  5");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseDComplex() {
        assertThat(AtfxParseUtil.parseDComplex(" 1.1   2.2 ").getR()).isEqualTo(1.1);
        assertThat(AtfxParseUtil.parseDComplex(" 1.1   2.2 ").getI()).isEqualTo(2.2);
        assertThat(AtfxParseUtil.parseDComplex("")).isNull();
        try {
            AtfxParseUtil.parseDComplex("asd");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }

    @Test
    void testParseDComplexSeq() {
        assertThat(AtfxParseUtil.parseDComplexSeq("  1  2 4  5")).hasSize(2);
        assertThat(AtfxParseUtil.parseDComplexSeq(" ")).isEmpty();
        try {
            AtfxParseUtil.parseDComplexSeq("  1 2  2 4  5");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
        try {
            AtfxParseUtil.parseDComplexSeq("  1 2a x 2 4  5");
            fail("Exception expected");
        } catch (OpenAtfxException e) {
        }
    }
}
