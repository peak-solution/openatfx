package de.rechner.openatfx_mdf4.util;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Utility class having methods to read MDF file contents.
 * 
 * @author Christian Rechner
 */
public abstract class MDFUtil {

    // For the strings in the IDBLOCK and for the block identifiers, always single byte character (SBC) encoding is used
    // (standard ASCII extension ISO-8859-1 Latin character set).
    private static final String CHARSET = "ISO-8859-1";

    /**
     * Read an 8-bit unsigned integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static byte readUInt8(ByteBuffer bb) {
        return bb.get();
    }

    /**
     * Read an 16-bit unsigned integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static int readUInt16(ByteBuffer bb) {
        return (bb.getShort() & 0xffff);
    }

    /**
     * Read an 16-bit signed integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static int readInt16(ByteBuffer bb) {
        return bb.getShort();
    }

    /**
     * Read an 32-bit unsigned integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static long readUInt32(ByteBuffer bb) {
        return (bb.getInt() & 0xffffffffL);
    }

    /**
     * Read an 32-bit signed integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static int readInt32(ByteBuffer bb) {
        return bb.getInt();
    }

    /**
     * Read an 64-bit unsigned integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static long readUInt64(ByteBuffer bb) {
        byte[] data = new byte[8];
        bb.get(data);
        long l1 = (((long) data[0] & 0xff) << 0) | (((long) data[1] & 0xff) << 8) | (((long) data[2] & 0xff) << 16)
                | (((long) data[3] & 0xff) << 24);
        long l2 = (((long) data[4] & 0xff) << 0) | (((long) data[5] & 0xff) << 8) | (((long) data[6] & 0xff) << 16)
                | (((long) data[7] & 0xff) << 24);
        return (l1 << 0) | (l2 << 32);
    }

    /**
     * Read an 64-bit signed integer from the byte buffer.
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static long readInt64(ByteBuffer bb) {
        return bb.getLong();
    }

    /**
     * Read a floating-point value compliant with IEEE 754, double precision (64 bits) (see [IEEE-FP]) from the byte
     * buffer. An infinite value (e.g. for tabular ranges in conversion rule) can be expressed using the NaNs INFINITY
     * resp. –INFINITY
     * 
     * @param bb The byte buffer.
     * @return The value.
     */
    public static double readReal(ByteBuffer bb) {
        return bb.getDouble();
    }

    /**
     * Read a 64-bit signed integer from the byte buffer, used as byte position within the file. If a LINK is NIL
     * (corresponds to 0), this means the LINK cannot be de-referenced. A link must be a multiple of 8.
     * 
     * @param bb The byte buffer.
     * @return The value.
     * @throws IOException
     */
    public static long readLink(ByteBuffer bb) {
        return ((long) bb.getInt() & 0xffffffffL);
    }

    // public static String readChars(FileChannel channel, int length) throws IOException {
    // ByteBuffer bb = ByteBuffer.allocate(length);
    // bb.order(ByteOrder.LITTLE_ENDIAN);
    // channel.read(bb);
    // bb.rewind();
    // return readChars(bb, bb.remaining());
    // }

    public static String readCharsISO8859(ByteBuffer bb, int length) throws IOException {
        byte[] b = new byte[length];
        bb.get(b);

        // lookup null character for string termination
        int strLength = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 0) {
                break;
            }
            strLength++;
        }

        return new String(b, 0, strLength, CHARSET);
    }

    // public static double readReal(FileChannel channel) throws IOException {
    // ByteBuffer bb = ByteBuffer.allocate(8);
    // bb.order(ByteOrder.LITTLE_ENDIAN);
    // channel.read(bb);
    // bb.rewind();
    // return bb.getDouble();
    // }

    // public static boolean readBool(ByteBuffer bb) throws IOException {
    // return bb.getShort() > 0;
    // }

}
