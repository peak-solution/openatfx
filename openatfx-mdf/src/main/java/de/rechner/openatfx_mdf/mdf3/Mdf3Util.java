package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;


/**
 * Utility class having methods to read MDF3 file contents.
 * 
 * @author Christian Rechner
 */
abstract class Mdf3Util {

    private static final String CHARSET_ISO8859 = "ISO-8859-1";

    public static String readChars(SeekableByteChannel channel, int length) throws IOException {
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

        return new String(b, 0, strLength, CHARSET_ISO8859);
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

    /**
     * Read the values from the source records.
     * 
     * @param recordBb
     * @param dgBlock
     * @param cgBlock
     * @param cnBlock
     * @return
     * @throws IOException
     */
    public static ByteBuffer readNumberValues(ByteBuffer recordBb, DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock)
            throws IOException {
        // put pointer to start of record area
        recordBb.rewind();

        // allocate target buffer (byte)
        int targetValueSize = cnBlock.getNumberOfBits() / 8;
        if (cnBlock.getNumberOfBits() % 8 != 0) {
            targetValueSize++;
        }

        int numberOfTotalBytes = (int) (targetValueSize * cgBlock.getNoOfRecords());
        ByteBuffer target = ByteBuffer.allocate(numberOfTotalBytes);

        // iterate over records
        for (int i = 0; i < cgBlock.getNoOfRecords(); i++) {

            // read record
            int recordIdOffset = (dgBlock.getNoRecordIds() > 0) ? 1 : 0;
            byte[] record = new byte[cgBlock.getDataRecordSize() + recordIdOffset];
            recordBb.get(record);

            // skip first bits and read value
            BitInputStream bis = new BitInputStream(record);
            int skipBits = (recordIdOffset * 8) + (cnBlock.getByteOffset() * 8) + cnBlock.getNumberOfFirstBits();
            bis.skip(skipBits);
            byte[] b = bis.readByteArray(cnBlock.getNumberOfBits());

            // add to buffer
            target.put(b);
        }

        target.rewind();
        return target;
    }

    /**
     * @param recordBb
     * @param dgBlock
     * @param cgBlock
     * @param cnBlock
     * @return
     * @throws IOException
     */
    public static ByteBuffer readStringValues(ByteBuffer recordBb, DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock)
            throws IOException {
        // put pointer to start of record area
        recordBb.rewind();

        byte[] target = new byte[0];

        // iterate over records
        for (int i = 0; i < cgBlock.getNoOfRecords(); i++) {

            // read record
            int recordIdOffset = (dgBlock.getNoRecordIds() > 0) ? 1 : 0;
            byte[] record = new byte[cgBlock.getDataRecordSize() + recordIdOffset];
            recordBb.get(record);

            // skip first bits and read value
            BitInputStream bis = new BitInputStream(record);
            int skipBits = (recordIdOffset * 8) + (cnBlock.getByteOffset() * 8) + cnBlock.getNumberOfFirstBits();
            bis.skip(skipBits);
            byte[] b = bis.readByteArray(cnBlock.getNumberOfBits());

            // build string value and append
            StringBuffer sb = new StringBuffer();
            sb.append(Mdf3Util.readChars(ByteBuffer.wrap(b), b.length));
            sb.append((char) '\0');

            // add to target
            byte[] src = sb.toString().getBytes();
            byte[] dest = new byte[target.length + src.length];
            System.arraycopy(target, 0, dest, 0, target.length);
            System.arraycopy(src, 0, dest, target.length, src.length);
            target = dest;
        }

        return ByteBuffer.wrap(target);
    }

    // /**
    // * Read boolean values and return as ByteBuffer containing values of type DT_BYTE
    // *
    // * @param recordBb
    // * @param dgBlock
    // * @param cgBlock
    // * @param cnBlock
    // * @return
    // * @throws IOException
    // */
    // public static ByteBuffer readBooleanAsByteValues(ByteBuffer recordBb, DGBLOCK dgBlock, CGBLOCK cgBlock,
    // CNBLOCK cnBlock) throws IOException {
    // // put pointer to start of record area
    // recordBb.rewind();
    //
    // byte[] target = new byte[(int) cgBlock.getNoOfRecords()];
    //
    // // iterate over records
    // for (int i = 0; i < cgBlock.getNoOfRecords(); i++) {
    // int recordIdOffset = (dgBlock.getNoRecordIds() > 0) ? 1 : 0;
    // byte[] record = new byte[cgBlock.getDataRecordSize() + recordIdOffset];
    // recordBb.get(record);
    // boolean bit = getBit(record, (recordIdOffset * 8) + cnBlock.getNumberOfFirstBits());
    // target[i] = bit ? (byte) 1 : (byte) 0;
    // }
    //
    // return ByteBuffer.wrap(target);
    // }

    public static boolean getBit(byte[] data, int pos) {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte valByte = data[posByte];
        return (valByte & (1 << posBit)) != 0;
    }

}
