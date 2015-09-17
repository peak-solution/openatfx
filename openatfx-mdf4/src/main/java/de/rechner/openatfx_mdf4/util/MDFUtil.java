package de.rechner.openatfx_mdf4.util;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;


/**
 * Utility class having methods to read MDF file contents.
 * 
 * @author Christian Rechner
 */
public abstract class MDFUtil {

    private static final String CHARSET = "UTF-8";

    public static String readChars(FileChannel channel, int length) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.read(bb);
        bb.rewind();
        return readChars(bb, bb.remaining());
    }

    public static String readChars(ByteBuffer bb, int length) throws IOException {
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

    public static double readReal(FileChannel channel) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.read(bb);
        bb.rewind();
        return bb.getDouble();
    }

    public static double readReal(ByteBuffer bb) throws IOException {
        return bb.getDouble();
    }

    public static boolean readBool(ByteBuffer bb) throws IOException {
        return bb.getShort() > 0;
    }

    public static BigInteger readUInt64(ByteBuffer bb) {
        byte[] data = new byte[8];
        bb.get(data);
        long l1 = (((long) data[0] & 0xff) << 0) | (((long) data[1] & 0xff) << 8) | (((long) data[2] & 0xff) << 16)
                | (((long) data[3] & 0xff) << 24);
        long l2 = (((long) data[4] & 0xff) << 0) | (((long) data[5] & 0xff) << 8) | (((long) data[6] & 0xff) << 16)
                | (((long) data[7] & 0xff) << 24);
        return BigInteger.valueOf((l1 << 0) | (l2 << 32));
    }

    public static long readUInt32(ByteBuffer bb) throws IOException {
        return (bb.getInt() & 0xffffffffL);
    }

    public static int readUInt16(ByteBuffer bb) throws IOException {
        return (bb.getShort() & 0xffff);
    }

    public static int readInt16(ByteBuffer bb) throws IOException {
        return bb.getShort();
    }

    public static long readLink(ByteBuffer bb) throws IOException {
        return ((long) bb.getInt() & 0xffffffffL);
    }

}
