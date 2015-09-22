package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * THE META DATA BLOCK <code>MDBLOCK</code>
 * </p>
 * The MDBLOCK contains information encoded as XML string. For example this can be comments for the measured data file,
 * file history information or the identification of a channel. This information is ruled by the parent block and
 * follows specific XML schemas definitions.
 * 
 * @author Christian Rechner
 */
class MDBLOCK extends BLOCK {

    public static String BLOCK_ID = "##MD";

    /** Data section */

    // XML string
    // UTF-8 encoded, zero terminated, new line indicated by CR and LF.
    // CHAR
    private String mdData;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private MDBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public String getMdData() {
        return mdData;
    }

    private void setMdData(String mdData) {
        this.mdData = mdData;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf.mdf4.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "MDBLOCK [mdData=" + mdData + "]";
    }

    /**
     * Reads a HDBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position within the channel.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static MDBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        MDBLOCK block = new MDBLOCK(channel, pos);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos);
        channel.read(bb);
        bb.rewind();

        // CHAR 4: Block type identifier
        block.setId(MDF4Util.readCharsISO8859(bb, 4));
        if (!block.getId().equals(BLOCK_ID)) {
            throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
        }

        // BYTE 4: Reserved used for 8-Byte alignment
        bb.get(new byte[4]);

        // UINT64: Length of block
        block.setLength(MDF4Util.readUInt64(bb));

        // UINT64: Number of links
        block.setLinkCount(MDF4Util.readUInt64(bb));

        // read block content
        bb = ByteBuffer.allocate((int) block.getLength() + 24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos + 24);
        channel.read(bb);
        bb.rewind();

        // XML String
        block.setMdData(MDF4Util.readCharsUTF8(bb, (int) (block.getLength() - 24)));

        return block;
    }

}
