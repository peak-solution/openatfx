package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE DATA BLOCK <code>DTBLOCK</code>
 * </p>
 * The data section of the DTBLOCK contains a sequence of records. It contains records of all channel groups assigned to
 * its parent DGBLOCK.
 * 
 * @author Christian Rechner
 */
class DTBLOCK extends BLOCK {

    public static String BLOCK_ID = "##DT";

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private DTBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "DTBLOCK [pos=" + getPos() + "]";
    }

    /**
     * Reads a DTBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position
     * @return The block data.
     * @throws IOException The exception.
     */
    public static DTBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        DTBLOCK block = new DTBLOCK(channel, pos);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos);
        channel.read(bb);
        bb.rewind();

        // CHAR 4: Block type identifier
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

        return block;
    }

}
