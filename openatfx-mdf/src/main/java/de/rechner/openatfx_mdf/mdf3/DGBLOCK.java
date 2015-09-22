package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * Data group block: Description of data block that may refer to one or several channel groups
 * </p>
 * The DGBlock contains the actual measurement data for one ("sorted") or several ("unsorted") channel groups.
 * 
 * @author Christian Rechner
 */
class DGBLOCK extends BLOCK {

    public static String BLOCK_ID = "DG";

    // LINK 1 Pointer to next data group block (DGBLOCK) (NIL allowed)
    private long lnkNextDgBlock;

    // LINK 1 Pointer to next channel group block (CGBLOCK) (NIL allowed)
    private long lnkNextCgBlock;

    // LINK 1 Pointer to trigger block (TRBLOCK) (NIL allowed)
    private long lnkTrBlock;

    // LINK 1 Pointer to the data records (see separate chapter on data storage)
    private long lnkDataRecords;

    // UINT16 1 Number of channel groups
    private int noChannelGroups;

    // UINT16 1 Number of record IDs in the data block
    // 0 = data records without record ID
    // 1 = record ID (UINT8) before each data record
    // 2 = record ID (UINT8) before and after each data record
    private int noRecordIds;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private DGBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkNextDgBlock() {
        return lnkNextDgBlock;
    }

    private void setLnkNextDgBlock(long lnkNextDgBlock) {
        this.lnkNextDgBlock = lnkNextDgBlock;
    }

    public long getLnkNextCgBlock() {
        return lnkNextCgBlock;
    }

    private void setLnkNextCgBlock(long lnkNextCgBlock) {
        this.lnkNextCgBlock = lnkNextCgBlock;
    }

    public long getLnkDataRecords() {
        return lnkDataRecords;
    }

    private void setLnkDataRecords(long lnkDataRecords) {
        this.lnkDataRecords = lnkDataRecords;
    }

    public int getNoChannelGroups() {
        return noChannelGroups;
    }

    private void setNoChannelGroups(int noChannelGroups) {
        this.noChannelGroups = noChannelGroups;
    }

    public int getNoRecordIds() {
        return noRecordIds;
    }

    private void setNoRecordIds(int noRecordIds) {
        this.noRecordIds = noRecordIds;
    }

    public long getLnkTrBlock() {
        return lnkTrBlock;
    }

    private void setLnkTrBlock(long lnkTrBlock) {
        this.lnkTrBlock = lnkTrBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "DGBLOCK [lnkNextDgBlock=" + lnkNextDgBlock + ", lnkNextCgBlock=" + lnkNextCgBlock + ", lnkTrBlock="
                + lnkTrBlock + ", lnkDataRecords=" + lnkDataRecords + ", noChannelGroups=" + noChannelGroups
                + ", noRecordIds=" + noRecordIds + "]";
    }

    public DGBLOCK getNextDgBlock() throws IOException {
        if (this.lnkNextDgBlock > 0) {
            return DGBLOCK.read(this.sbc, this.lnkNextDgBlock);
        }
        return null;
    }

    public CGBLOCK getNextCgBlock() throws IOException {
        if (this.lnkNextCgBlock > 0) {
            return CGBLOCK.read(this.sbc, this.lnkNextCgBlock);
        }
        return null;
    }

    /**
     * Reads a DGBLOCK from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static DGBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
        DGBLOCK block = new DGBLOCK(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(28);
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

        // LINK 1 Pointer to next data group block (DGBLOCK) (NIL allowed)
        block.setLnkNextDgBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to next channel group block (CGBLOCK) (NIL allowed)
        block.setLnkNextCgBlock(Mdf3Util.readLink(bb));

        // LINK 1 Reserved
        block.setLnkTrBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the data records (see separate chapter on data storage)
        block.setLnkDataRecords(Mdf3Util.readLink(bb));

        // UINT16 1 Number of channel groups
        block.setNoChannelGroups(Mdf3Util.readUInt16(bb));

        // UINT16 1 Number of record IDs in the data block
        // 0 = data records without record ID
        // 1 = record ID (UINT8) before each data record
        // 2 = record ID (UINT8) before and after each data record
        block.setNoRecordIds(Mdf3Util.readUInt16(bb));

        return block;
    }

}
