package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE CHANNEL HIERARCHY BLOCK <code>CHBLOCK</code>
 * </p>
 * The CHBLOCKs describe a logical ordering of the channels in a tree-like structure. This only serves to structure the
 * channels and is totally independent to the data group and channel group structuring. A channel even may not be
 * referenced at all or more than one time.<br/>
 * Each CHBLOCK can be seen as a node in a tree which has a number of channels as leafs and which has a reference to its
 * next sibling and its first child node (both CHBLOCKs). The reference to a channel is always a triple link to the
 * CNBLOCK of the channel and its parent CGBLOCK and DGBLOCK. Each CHBLOCK can have a name.
 * 
 * @author Christian Rechner
 */
class CHBLOCK extends BLOCK {

    public static String BLOCK_ID = "##CH";

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CHBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    /** Link section */

    // // Link to next sibling CHBLOCK (can be NIL)
    // // LINK
    // private long lnkChNext;
    //
    // // Link to first child CHBLOCK (can be NIL, must be NIL for ch_type = 3 ("map list")).
    // // LINK
    // private long lnkChFirst;
    //
    // // Link to TXBLOCK with the name of the hierarchy level. Must be NIL for ch_type ≥ 4, must not be NIL for all
    // other
    // // types.
    // // LINK
    // private long lnkTxName;
    //
    // // Link to TXBLOCK or MDBLOCK with comment and other information for the hierarchy level (can be NIL)
    // // LINK
    // private long lnkMdComment;

    /**
     * Reads a CHBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CHBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        CHBLOCK block = new CHBLOCK(channel, pos);

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

        // read block content
        bb = ByteBuffer.allocate((int) block.getLength() + 24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos + 24);
        channel.read(bb);
        bb.rewind();

        // TODO: implement reading

        return block;
    }

}
