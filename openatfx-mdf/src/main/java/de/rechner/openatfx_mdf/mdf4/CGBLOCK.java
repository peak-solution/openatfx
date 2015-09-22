package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * THE CHANNEL GROUP BLOCK <code>CGBLOCK</code>
 * </p>
 * The CGBLOCK contains a collection of channels which are stored in one record, i.e. which have equal sampling. The
 * only exception is a channel group for variable length signal data (VLSD), called "VLSD channel group". It has no
 * channel collection and can only occur in an unsorted data group. It describes signal values of variable length which
 * are stored as variable length records in a normal DTBLOCK.
 * 
 * @author Christian Rechner
 */
class CGBLOCK extends BLOCK {

    public static String BLOCK_ID = "##CG";

    /** Link section */

    // Pointer to next channel group block (CGBLOCK) (can be NIL)
    // LINK
    private long lnkCgNext;

    // Pointer to first channel block (CNBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK, i.e. if "VLSD channel group")
    // flag (bit 0) is set)
    // LINK
    private long lnkCnFirst;

    // Pointer to acquisition name (TXBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
    // LINK
    private long lnkTxAcqName;

    // Pointer to acquisition source (SIBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
    // LINK
    private long lnkSiAcqSource;

    // Pointer to first sample reduction block (SRBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
    // LINK
    private long lnkSrFirst;

    // Pointer to comment and additional information (TXBLOCK or MDBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
    // LINK
    private long lnkMdComment;

    /** Data section */

    // Record ID, value must be less than maximum unsigned integer value allowed by dg_rec_id_size in parent DGBLOCK.
    // UINT64
    private long recordId;

    // Number of cycles, i.e. number of samples for this channel group.
    // This specifies the number of records of this type in the data block.
    // UINT64
    private long cycleCount;

    // Flags
    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: VLSD channel group flag.
    // Bit 1: Bus event channel group flag
    // Bit 2: Plain bus event channel group flag
    // UINT16
    private int flags;

    // Value of character to be used as path separator, 0 if no path separator specified.
    // UINT16
    private int pathSeparator;

    // Normal CGBLOCK:
    // Number of data Bytes (after record ID) used for signal values in record, i.e. size of plain data for each
    // recorded sample of this channel group.
    // VLSD CGBLOCK:
    // Low part of a UINT64 value that specifies the total size in Bytes of all variable length signal values for the
    // recorded samples of this channel group.
    // UINT32
    private long dataBytes;

    // Normal CGBLOCK:
    // Number of additional Bytes for record used for invalidation bits. Can be zero if no invalidation bits are used at
    // all. Invalidation bits may only occur in the specified number of Bytes after the data Bytes, not within the data
    // Bytes that contain the signal values.
    // VLSD CGBLOCK:
    // High part of UINT64 value that specifies the total size in Bytes of all variable length signal values for the
    // recorded samples of this channel group, i.e. the total size in Bytes can be calculated by cg_data_bytes +
    // (cg_inval_bytes << 32)
    // Note: this value does not include the Bytes used to specify the length of each VLSD value!
    private long invalBytes;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CGBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkCgNext() {
        return lnkCgNext;
    }

    public long getLnkCnFirst() {
        return lnkCnFirst;
    }

    public long getLnkTxAcqName() {
        return lnkTxAcqName;
    }

    public long getLnkSiAcqSource() {
        return lnkSiAcqSource;
    }

    public long getLnkSrFirst() {
        return lnkSrFirst;
    }

    public long getLnkMdComment() {
        return lnkMdComment;
    }

    public long getRecordId() {
        return recordId;
    }

    public long getCycleCount() {
        return cycleCount;
    }

    public int getFlags() {
        return flags;
    }

    public int getPathSeparator() {
        return pathSeparator;
    }

    public long getDataBytes() {
        return dataBytes;
    }

    public long getInvalBytes() {
        return invalBytes;
    }

    private void setLnkCgNext(long lnkCgNext) {
        this.lnkCgNext = lnkCgNext;
    }

    private void setLnkCnFirst(long lnkCnFirst) {
        this.lnkCnFirst = lnkCnFirst;
    }

    private void setLnkTxAcqName(long lnkTxAcqName) {
        this.lnkTxAcqName = lnkTxAcqName;
    }

    private void setLnkSiAcqSource(long lnkSiAcqSource) {
        this.lnkSiAcqSource = lnkSiAcqSource;
    }

    private void setLnkSrFirst(long lnkSrFirst) {
        this.lnkSrFirst = lnkSrFirst;
    }

    private void setLnkMdComment(long lnkMdComment) {
        this.lnkMdComment = lnkMdComment;
    }

    private void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    private void setCycleCount(long cycleCount) {
        this.cycleCount = cycleCount;
    }

    private void setFlags(int flags) {
        this.flags = flags;
    }

    private void setPathSeparator(int pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    private void setDataBytes(long dataBytes) {
        this.dataBytes = dataBytes;
    }

    private void setInvalBytes(long invalBytes) {
        this.invalBytes = invalBytes;
    }

    public CGBLOCK getCgNextBlock() throws IOException {
        if (this.lnkCgNext > 0) {
            return CGBLOCK.read(this.sbc, this.lnkCgNext);
        }
        return null;
    }

    public TXBLOCK getTxAcqNameBlock() throws IOException {
        if (this.lnkTxAcqName > 0) {
            return TXBLOCK.read(this.sbc, this.lnkTxAcqName);
        }
        return null;
    }

    public SIBLOCK getSiAcqSourceBlock() throws IOException {
        if (this.lnkSiAcqSource > 0) {
            return SIBLOCK.read(this.sbc, this.lnkSiAcqSource);
        }
        return null;
    }

    public CNBLOCK getCnFirstBlock() throws IOException {
        if (this.lnkCnFirst > 0) {
            return CNBLOCK.read(this.sbc, this.lnkCnFirst);
        }
        return null;
    }

    public BLOCK getMdCommentBlock() throws IOException {
        if (this.lnkMdComment > 0) {
            String blockType = getBlockType(this.sbc, this.lnkMdComment);
            // link points to a MDBLOCK
            if (blockType.equals(MDBLOCK.BLOCK_ID)) {
                return MDBLOCK.read(this.sbc, this.lnkMdComment);
            }
            // links points to TXBLOCK
            else if (blockType.equals(TXBLOCK.BLOCK_ID)) {
                return TXBLOCK.read(this.sbc, this.lnkMdComment);
            }
            // unknown
            else {
                throw new IOException("Unsupported block type for MdComment: " + blockType);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CGBLOCK [lnkCgNext=" + lnkCgNext + ", lnkCnFirst=" + lnkCnFirst + ", lnkTxAcqName=" + lnkTxAcqName
                + ", lnkSiAcqSource=" + lnkSiAcqSource + ", lnkSrFirst=" + lnkSrFirst + ", lnkMdComment="
                + lnkMdComment + ", recordId=" + recordId + ", cycleCount=" + cycleCount + ", flags=" + flags
                + ", pathSeparator=" + pathSeparator + ", dataBytes=" + dataBytes + ", invalBytes=" + invalBytes + "]";
    }

    /**
     * Reads a CGBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position within the channel.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CGBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        CGBLOCK block = new CGBLOCK(channel, pos);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(104);
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

        // LINK: Pointer to next channel group block (CGBLOCK) (can be NIL)
        block.setLnkCgNext(MDF4Util.readLink(bb));

        // LINK: Pointer to first channel block (CNBLOCK) (can be NIL)
        block.setLnkCnFirst(MDF4Util.readLink(bb));

        // LINK: Pointer to acquisition name (TXBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
        block.setLnkTxAcqName(MDF4Util.readLink(bb));

        // LINK: Pointer to acquisition source (SIBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
        block.setLnkSiAcqSource(MDF4Util.readLink(bb));

        // LINK: Pointer to first sample reduction block (SRBLOCK) (can be NIL, must be NIL for VLSD CGBLOCK)
        block.setLnkSrFirst(MDF4Util.readLink(bb));

        // LINK: Pointer to comment and additional information (TXBLOCK or MDBLOCK) (can be NIL, must be NIL for VLSD
        // CGBLOCK)
        block.setLnkMdComment(MDF4Util.readLink(bb));

        // UINT64: Record ID
        block.setRecordId(MDF4Util.readUInt64(bb));

        // UINT64: Number of cycles
        block.setCycleCount(MDF4Util.readUInt64(bb));

        // UINT16: Flags
        block.setFlags(MDF4Util.readUInt16(bb));

        // UINT16: Value of character to be used as path separator, 0 if no path separator specified.
        block.setPathSeparator(MDF4Util.readUInt16(bb));

        // BYTE 4 Reserved.
        bb.get(new byte[4]);

        // UINT32: Number of data Bytes (after record ID) used for signal values in record.
        block.setDataBytes(MDF4Util.readUInt32(bb));

        // UINT32: Number of additional Bytes for record used for invalidation bits.
        block.setInvalBytes(MDF4Util.readUInt32(bb));

        return block;
    }

}
