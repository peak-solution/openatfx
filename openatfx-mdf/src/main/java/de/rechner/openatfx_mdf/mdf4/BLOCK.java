package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * Base class for all blocks.
 * 
 * @author Christian Rechner
 */
abstract class BLOCK {

    // the position of the block within the MDF file
    private final long pos;

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

    protected final SeekableByteChannel sbc;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    protected BLOCK(SeekableByteChannel sbc, long pos) {
        this.sbc = sbc;
        this.pos = pos;
    }

    public long getPos() {
        return pos;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public long getLength() {
        return length;
    }

    protected void setLength(long length) {
        this.length = length;
    }

    public long getLinkCount() {
        return linkCount;
    }

    protected void setLinkCount(long linkCount) {
        this.linkCount = linkCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BLOCK [pos=" + pos + ", id=" + id + ", length=" + length + ", linkCount=" + linkCount + "]";
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (pos ^ (pos >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BLOCK other = (BLOCK) obj;
        if (pos != other.pos)
            return false;
        return true;
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
        return MDF4Util.readCharsISO8859(bb, 4);
    }

}