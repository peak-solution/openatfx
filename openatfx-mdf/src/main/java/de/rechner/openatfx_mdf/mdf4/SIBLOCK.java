package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * THE SOURCE INFORMATION BLOCK <code>SIBLOCK<code>
 * </p>
 * The SIBLOCK describes the source of an acquisition mode or of a signal. The source information is also used to ensure
 * a unique identification of a channel.
 * 
 * @author Christian Rechner
 */
class SIBLOCK extends BLOCK {

    public static String BLOCK_ID = "##SI";

    /** Link section */

    // Pointer to TXBLOCK with name (identification) of source (can be NIL).
    // LINK
    private long lnkTxName;

    // Pointer to TXBLOCK with (tool-specific) path of source (can be NIL).
    // LINK
    private long lnkTxPath;

    // Pointer to source comment and additional information (TXBLOCK or MDBLOCK) (can be NIL)
    // LINK
    private long lnkMdComment;

    /** Data section */

    // Source type: additional classification of source:
    // 0 = OTHER source type does not fit into given categories or is unknown
    // 1 = ECU source is an ECU
    // 2 = BUS source is a bus (e.g. for bus monitoring)
    // 3 = I/O source is an I/O device (e.g. analog I/O)
    // 4 = TOOL source is a software tool (e.g. for tool generated signals/events)
    // 5 = USER source is a user interaction/input
    // (e.g. for user generated events)
    // UINT8
    private byte sourceType;

    // Bus type: additional classification of used bus (should be 0 for si_type ≥ 3):
    // 0 = NONE no bus
    // 1 = OTHER bus type does not fit into given categories or is unknown
    // 2 = CAN
    // 3 = LIN
    // 4 = MOST
    // 5 = FLEXRAY
    // 6 = K_LINE
    // 7 = ETHERNET
    // 8 = USB
    // Vender defined bus types can be added starting with value 128.
    // UINT8
    private byte busType;

    // Flags
    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: simulated source
    // Source is only a simulation (can be hardware or software simulated)
    // Cannot be set for si_type = 4 (TOOL).
    // UINT8
    private byte flags;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private SIBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    private void setLnkTxName(long lnkTxName) {
        this.lnkTxName = lnkTxName;
    }

    private void setLnkTxPath(long lnkTxPath) {
        this.lnkTxPath = lnkTxPath;
    }

    private void setLnkMdComment(long lnkMdComment) {
        this.lnkMdComment = lnkMdComment;
    }

    private void setSourceType(byte sourceType) {
        this.sourceType = sourceType;
    }

    private void setBusType(byte busType) {
        this.busType = busType;
    }

    private void setFlags(byte flags) {
        this.flags = flags;
    }

    public long getLnkTxName() {
        return lnkTxName;
    }

    public long getLnkTxPath() {
        return lnkTxPath;
    }

    public long getLnkMdComment() {
        return lnkMdComment;
    }

    public byte getSourceType() {
        return sourceType;
    }

    public byte getBusType() {
        return busType;
    }

    public byte getFlags() {
        return flags;
    }

    public TXBLOCK getTxNameBlock() throws IOException {
        if (this.lnkTxName > 0) {
            return TXBLOCK.read(this.sbc, this.lnkTxName);
        }
        return null;
    }

    public TXBLOCK getTxPath() throws IOException {
        if (this.lnkTxPath > 0) {
            return TXBLOCK.read(this.sbc, this.lnkTxPath);
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
        return "SIBLOCK [lnkTxName=" + lnkTxName + ", lnkTxPath=" + lnkTxPath + ", lnkMdComment=" + lnkMdComment
                + ", sourceType=" + sourceType + ", busType=" + busType + ", flags=" + flags + "]";
    }

    /**
     * Reads a SIBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position within the channel.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static SIBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        SIBLOCK block = new SIBLOCK(channel, pos);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(56);
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

        // LINK: Pointer to TXBLOCK with name (identification) of source (can be NIL).
        block.setLnkTxName(MDF4Util.readLink(bb));

        // LINK: Pointer to TXBLOCK with (tool-specific) path of source (can be NIL).
        block.setLnkTxPath(MDF4Util.readLink(bb));

        // LINK: Pointer to source comment and additional information (TXBLOCK or MDBLOCK) (can be NIL).
        block.setLnkMdComment(MDF4Util.readLink(bb));

        // UINT8: Source type: additional classification of source:
        block.setSourceType(MDF4Util.readUInt8(bb));

        // UINT8: Bus type: additional classification of used bus (should be 0 for si_type ≥ 3):
        block.setBusType(MDF4Util.readUInt8(bb));

        // UINT8: Flags
        block.setFlags(MDF4Util.readUInt8(bb));

        return block;
    }

}
