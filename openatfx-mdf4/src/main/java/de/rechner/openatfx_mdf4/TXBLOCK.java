package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE TEXT BLOCK <code>TXBLOCK<code>
 * </p>
 * The TXBLOCK is very similar to the MDBLOCK but only contains a plain string encoded in UTF-8. The text length results
 * from the block size.
 * 
 * @author Christian Rechner
 */
class TXBLOCK extends BLOCK {

    public static String BLOCK_ID = "##TX";

    /** Data section */

    // Plain text string
    // UTF-8 encoded, zero terminated, new line indicated by CR and LF.
    // CHAR
    private String txData;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     */
    public TXBLOCK(SeekableByteChannel sbc) {
        super(sbc);
    }

    public String getTxData() {
        return txData;
    }

    public void setTxData(String txData) {
        this.txData = txData;
    }

    @Override
    public String toString() {
        return "TXBLOCK [txData=" + txData + "]";
    }

    /**
     * Reads a HDBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position
     * @return The block data.
     * @throws IOException The exception.
     */
    public static TXBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        TXBLOCK block = new TXBLOCK(channel);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos);
        channel.read(bb);
        bb.rewind();

        // CHAR 4: Block type identifier, always "##HD"
        block.setId(MDFUtil.readCharsISO8859(bb, 4));
        if (!block.getId().equals(BLOCK_ID)) {
            throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
        }

        // BYTE 4: Reserved used for 8-Byte alignment
        bb.get(new byte[4]);

        // UINT64: Length of block
        block.setLength(MDFUtil.readUInt64(bb));

        // UINT64: Number of links
        block.setLinkCount(MDFUtil.readUInt64(bb));

        // read block content
        bb = ByteBuffer.allocate((int) block.getLength() + 24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos + 24);
        channel.read(bb);
        bb.rewind();

        // XML String
        block.setTxData(MDFUtil.readCharsUTF8(bb, (int) (block.getLength() - 24)));

        return block;
    }

}
