package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * Text block: Contains a String with variable length
 * </p>
 * The TXBLOCK contains an optional comment for the measured data file, channel group or signal, or the long name of a
 * signal. The text length results from the block size.
 * 
 * @author Christian Rechner
 */
class TXBLOCK extends BLOCK {

    public static String BLOCK_ID = "TX";

    // CHAR variable Text (new line indicated by CR and LF; end of text indicated by 0)
    private String text;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private TXBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public String getText() {
        return text;
    }

    private void setText(String text) {
        this.text = text;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TXBLOCK [text=" + text + "]";
    }

    /**
     * Reads a TXBLOCK from the channel starting at pos
     * 
     * @param channel The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static TXBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
        TXBLOCK block = new TXBLOCK(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(pos);
        sbc.read(bb);
        bb.rewind();

        // CHAR 2 Block type identifier
        block.setId(Mdf3Util.readChars(bb, 2));
        if (!block.getId().equals(BLOCK_ID)) {
            throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
        }

        // UINT16 1 Block size of this block in bytes
        block.setLength(Mdf3Util.readUInt16(bb));

        // CHAR variable Text (new line indicated by CR and LF; end of text indicated by 0)
        block.setText(Mdf3Util.readChars(sbc, block.getLength() - 5));

        return block;
    }

}
