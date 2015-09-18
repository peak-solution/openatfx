package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * Base class for all blocks.
 * 
 * @author Christian Rechner
 */
abstract class BLOCK {

    /** Header section */

    // Block type identifier, always "##HD"
    // CHAR 4
    private String id;

    // Length of block
    // UINT64
    private long length;

    // Number of links
    // UINT64
    private long linkCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(long linkCount) {
        this.linkCount = linkCount;
    }

    /**
     * Returns the block type string at given position.
     * 
     * @param channel The channel to read from.
     * @param pos The position within the channel.
     * @return The block type as string.
     * @throws IOException Error reading block type.
     */
    protected static String getBlockType(SeekableByteChannel channel, long pos) throws IOException {
        // read block header
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos);
        channel.read(bb);
        bb.rewind();
        return MDFUtil.readCharsISO8859(bb, 4);
    }

}