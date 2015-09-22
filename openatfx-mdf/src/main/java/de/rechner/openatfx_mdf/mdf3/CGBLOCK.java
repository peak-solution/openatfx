package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * Channel group block: Description of a channel group, i.e. signals which are always measured jointly
 * </p>
 * This block describes the structure of a channel group. A channel group consists of different channels which are
 * measured jointly at the same rate.
 * 
 * @author Christian Rechner
 */
class CGBLOCK extends BLOCK {

    public static String BLOCK_ID = "CG";

    // LINK 1 Pointer to next data Channel group block (CGBLOCK) (NIL allowed)
    private long lnkNextCgBlock;

    // LINK 1 Pointer to first channel block (CNBLOCK) (NIL allowed)
    private long lnkFirstCnBlock;

    // LINK 1 Pointer to channel group comment text (TXBLOCK) (NIL allowed)
    private long lnkChannelGroupComment;

    // UINT16 1 Record ID
    private int recordId;

    // UINT16 1 Number of channels
    private int noOfChannels;

    // UINT16 1 Data record size in bytes (without the record ID), i.e. data size of the channel group for each sample
    private int dataRecordSize;

    // UINT32 1 Number of records
    private long noOfRecords;

    // LINK 1 Pointer to first sample reduction block (SRBLOCK) (NIL allowed)
    private long lnkFirstSrBlock;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CGBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkNextCgBlock() {
        return lnkNextCgBlock;
    }

    private void setLnkNextCgBlock(long lnkNextCgBlock) {
        this.lnkNextCgBlock = lnkNextCgBlock;
    }

    public long getLnkFirstCnBlock() {
        return lnkFirstCnBlock;
    }

    private void setLnkFirstCnBlock(long lnkFirstCnBlock) {
        this.lnkFirstCnBlock = lnkFirstCnBlock;
    }

    public long getLnkChannelGroupComment() {
        return lnkChannelGroupComment;
    }

    private void setLnkChannelGroupComment(long lnkChannelGroupComment) {
        this.lnkChannelGroupComment = lnkChannelGroupComment;
    }

    public int getRecordId() {
        return recordId;
    }

    private void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getNoOfChannels() {
        return noOfChannels;
    }

    private void setNoOfChannels(int noOfChannels) {
        this.noOfChannels = noOfChannels;
    }

    public int getDataRecordSize() {
        return dataRecordSize;
    }

    private void setDataRecordSize(int dataRecordSize) {
        this.dataRecordSize = dataRecordSize;
    }

    public long getNoOfRecords() {
        return noOfRecords;
    }

    private void setNoOfRecords(long noOfRecords) {
        this.noOfRecords = noOfRecords;
    }

    public long getLnkFirstSrBlock() {
        return lnkFirstSrBlock;
    }

    private void setLnkFirstSrBlock(long lnkFirstSrBlock) {
        this.lnkFirstSrBlock = lnkFirstSrBlock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "CGBLOCK [lnkNextCgBlock=" + lnkNextCgBlock + ", lnkFirstCnBlock=" + lnkFirstCnBlock
                + ", lnkChannelGroupComment=" + lnkChannelGroupComment + ", recordId=" + recordId + ", noOfChannels="
                + noOfChannels + ", dataRecordSize=" + dataRecordSize + ", noOfRecords=" + noOfRecords
                + ", lnkFirstSrBlock=" + lnkFirstSrBlock + "]";
    }

    public CGBLOCK getNextCgBlock() throws IOException {
        if (this.lnkNextCgBlock > 0) {
            return CGBLOCK.read(this.sbc, this.lnkNextCgBlock);
        }
        return null;
    }

    public CNBLOCK getFirstCnBlock() throws IOException {
        if (this.lnkFirstCnBlock > 0) {
            return CNBLOCK.read(this.sbc, this.lnkFirstCnBlock);
        }
        return null;
    }

    public TXBLOCK getChannelGroupComment() throws IOException {
        if (this.lnkChannelGroupComment > 0) {
            return TXBLOCK.read(this.sbc, this.lnkChannelGroupComment);
        }
        return null;
    }

    /**
     * Reads a CGBLOCK from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CGBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
        CGBLOCK block = new CGBLOCK(sbc, pos);

        // read block header
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

        // read block
        bb = ByteBuffer.allocate(block.getLength() - 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(pos + 4);
        sbc.read(bb);
        bb.rewind();

        // LINK 1 Pointer to next data Channel group block (CGBLOCK) (NIL allowed)
        block.setLnkNextCgBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to first channel block (CNBLOCK) (NIL allowed)
        block.setLnkFirstCnBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to channel group comment text (TXBLOCK) (NIL allowed)
        block.setLnkChannelGroupComment(Mdf3Util.readLink(bb));

        // UINT16 1 Record ID
        block.setRecordId(Mdf3Util.readUInt16(bb));

        // UINT16 1 Number of channels
        block.setNoOfChannels(Mdf3Util.readUInt16(bb));

        // UINT16 1 Data record size in bytes (without the record ID), i.e. data size of the channel group for each
        // sample
        block.setDataRecordSize(Mdf3Util.readUInt16(bb));

        // UINT32 1 Number of records
        block.setNoOfRecords(Mdf3Util.readUInt32(bb));

        // LINK 1 Pointer to first sample reduction block (SRBLOCK) (NIL allowed)
        // Valid since version 3.30. Default value: NIL.
        if (block.getLength() > 26) {
            block.setLnkFirstSrBlock(Mdf3Util.readLink(bb));
        }

        return block;
    }

}
