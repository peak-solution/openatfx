package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;


/**
 * A bit-wise input stream.
 * 
 * @author Christian Rechner
 */
class BitInputStream {

    // The byte array being read from.
    private byte[] mBuf;

    // The current position offset, in bits, from the msb in byte 0.
    private int mPos;

    // The last valid bit offset.
    private int mEnd;

    /**
     * Create object from byte array.
     * 
     * @param buf a byte array containing data
     */
    public BitInputStream(byte buf[]) {
        mBuf = buf;
        mEnd = buf.length << 3;
        mPos = 0;
    }

    /**
     * Return the number of bit still available for reading.
     */
    public int available() {
        return mEnd - mPos;
    }

    /**
     * Read some data and increment the current position. The 8-bit limit on access to bitwise streams is intentional to
     * avoid endianness issues.
     * 
     * @param bits the amount of data to read (gte 0, lte 8)
     * @return byte of read data (possibly partially filled, from lsb)
     */
    public int read(int bits) throws IOException {
        int index = mPos >>> 3;
        int offset = 16 - (mPos & 0x07) - bits; // &7==%8
        if ((bits < 0) || (bits > 8) || ((mPos + bits) > mEnd)) {
            throw new IOException("illegal read " + "(pos " + mPos + ", end " + mEnd + ", bits " + bits + ")");
        }
        int data = (mBuf[index] & 0xFF) << 8;
        if (offset < 8) {
            data |= mBuf[index + 1] & 0xFF;
        }
        data >>>= offset;
        data &= (-1 >>> (32 - bits));
        mPos += bits;
        return data;
    }

    /**
     * Read data in bulk into a byte array and increment the current position.
     * 
     * @param bits the amount of data to read
     * @return newly allocated byte array of read data
     */
    public byte[] readByteArray(int bits) throws IOException {
        int bytes = (bits >>> 3) + ((bits & 0x07) > 0 ? 1 : 0); // &7==%8
        byte[] arr = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            int increment = Math.min(8, bits - (i << 3));
            arr[i] = (byte) (read(increment) << (8 - increment));
        }
        return arr;
    }

    /**
     * Increment the current position and ignore contained data.
     * 
     * @param bits the amount by which to increment the position
     */
    public void skip(int bits) throws IOException {
        if ((mPos + bits) > mEnd) {
            throw new IOException("illegal skip " + "(pos " + mPos + ", end " + mEnd + ", bits " + bits + ")");
        }
        mPos += bits;
    }

}
