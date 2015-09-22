package de.rechner.openatfx_mdf.mdf3;

import java.nio.channels.SeekableByteChannel;


/**
 * Base class for all blocks.
 * 
 * @author Christian Rechner
 */
abstract class BLOCK {

    // the position of the block within the MDF file
    private final long pos;

    // CHAR 2 Block type identifier, always "CC"
    private String id;

    // UINT16 1 Block size of this block in bytes
    private int length;

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

    public int getLength() {
        return length;
    }

    protected void setLength(int length) {
        this.length = length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BLOCK [pos=" + pos + ", id=" + id + ", length=" + length + "]";
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

}