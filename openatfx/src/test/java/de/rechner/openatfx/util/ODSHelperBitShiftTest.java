package de.rechner.openatfx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.asam.ods.AoException;
import org.junit.jupiter.api.Test;


/**
 * Test case for getBitShiftedValue() method from <code>de.rechner.openatfx.util.ODSHelper</code>.
 * 
 * @author Martin Fleischer
 */
public class ODSHelperBitShiftTest {

    @Test
    public void testGetBitIntValue40Bit0Offset() {
        byte[][] bytes = new byte[][] { new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, // 0
                new byte[] { (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }, // 1
                new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, // -1
                new byte[] { (byte) 0x80, (byte) 0x82, (byte) 0x83, (byte) 0x5C, (byte) 0x72 }, // 491_178_394_240
                new byte[] { (byte) 0xEA, (byte) 0x1E, (byte) 0x5C, (byte) 0x59, (byte) 0xC4 }, // -256_198_828_310
                new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80 }, // -549_755_813_888
                                                                                                // (min)
                new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F } // 549_755_813_887 (max)
        };

        long[] expected = new long[] { 0L, 1L, -1L, 491178394240L, -256198828310L, -549755813888L, 549755813887L };

        try {
            for (int i = bytes.length - 1; i >= 0; --i) {
                Number value = ODSHelper.getBitShiftedIntegerValue(bytes[i], 27, 40, 0);
                assertTrue(value instanceof Long);
                assertEquals(expected[i], value.longValue());
            }
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testGetBitIntValue24Bit0Offset() {
        byte[][] bytes = new byte[][] { new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00 }, // 0
                new byte[] { (byte) 0x01, (byte) 0x00, (byte) 0x00 }, // 1
                new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, // -1
                new byte[] { (byte) 0xE8, (byte) 0x7E, (byte) 0x07 }, // 491_240
                new byte[] { (byte) 0x6A, (byte) 0x5C, (byte) 0xF3 }, // -828_310
                new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x80 }, // -8_388_608 (min)
                new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0x7F } // 8_388_607 (max)
        };

        int[] expected = new int[] { 0, 1, -1, 491_240, -828310, -8388608, 8388607 };

        try {
            for (int i = bytes.length - 1; i >= 0; --i) {
                Number value = ODSHelper.getBitShiftedIntegerValue(bytes[i], 27, 24, 0);
                assertTrue(value instanceof Integer);
                assertEquals(expected[i], value.intValue());
            }
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testGetBitIntValueUnsigned32Bit0OffsetBE() {
        byte[] bytes = new byte[] { (byte) 0xFF, (byte) 0x07, (byte) 0x07E, (byte) 0xE8 };

        try {
            // Most significant bit in most significant byte is set, should not be interpreted as sign:
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 30, 32, 0);

            assertTrue(value instanceof Long);
            assertEquals(4278681320L, value.longValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testGetBitIntValueSigned64Bit2OffsetBE() {
        byte[] bytes = new byte[] { (byte) 0x03, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFC };

        try {
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 28, 64, 2);

            assertTrue(value instanceof Long);
            assertEquals(-1L, value.longValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    /**
     * Example from ASAM MDF v4.1.0 section 5.21.5
     */
    @Test
    public void testGetBitIntValueUnsigned14Bit6OffsetLE() {
        byte[] bytes = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        try {
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 29, 14, 6);

            assertTrue(value instanceof Integer);
            assertEquals(16182, value.intValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    /**
     * Example from ASAM MDF v4.1.0 section 5.21.6
     */
    @Test
    public void testGetBitIntValueUnsigned14Bit6OffsetBE() {
        byte[] bytes = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        try {
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 30, 14, 6);

            assertTrue(value instanceof Integer);
            assertEquals(12087, value.intValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    /**
     * Example from ASAM MDF v4.1.0 section 5.21.5
     */
    @Test
    public void testGetBitIntValueSigned14Bit6OffsetLE() {
        byte[] bytes = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        try {
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 27, 14, 6);

            assertTrue(value instanceof Integer);
            assertEquals(-202, value.intValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }

    /**
     * Example from ASAM MDF v4.1.0 section 5.21.6
     */
    @Test
    public void testGetBitIntValueSigned14Bit6OffsetBE() {
        byte[] bytes = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };

        try {
            Number value = ODSHelper.getBitShiftedIntegerValue(bytes, 28, 14, 6);

            assertTrue(value instanceof Integer);
            assertEquals(-4297, value.intValue());
        } catch (AoException exc) {
            fail(exc.getMessage());
        }
    }
}
