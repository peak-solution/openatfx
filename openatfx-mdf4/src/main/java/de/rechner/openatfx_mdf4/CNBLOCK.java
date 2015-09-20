package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE CHANNEL BLOCK <code>CNBLOCK</code>
 * </p>
 * The CNBLOCK describes a channel, i.e. it contains information about the recorded signal and how its signal values are
 * stored in the MDF file.
 * 
 * @author Christian Rechner
 */
class CNBLOCK extends BLOCK {

    public static String BLOCK_ID = "##CN";

    /** Link section */

    // Pointer to next channel block (CNBLOCK) (can be NIL)
    // LINK
    private long lnkCnNext;

    // Composition of channels: Pointer to channel array block (CABLOCK) or channel block (CNBLOCK) (can be NIL).
    // LINK
    private long lnkComposition;

    // Pointer to TXBLOCK with name (identification) of channel.
    // LINK
    private long lnkTxName;

    // Pointer to channel source (SIBLOCK) (can be NIL)
    // Must be NIL for component channels (members of a structure or array elements) because they all must have the same
    // source and thus simply use the SIBLOCK of their parent CNBLOCK (direct child of CGBLOCK).
    // LINK
    private long lnkSiSource;

    // Pointer to the conversion formula (CCBLOCK) (can be NIL, must be NIL for complex channel data types, i.e. for
    // cn_data_type ≥ 10).
    // LINK
    private long lnkCcConversion;

    // Pointer to channel type specific signal data
    // LINK
    private long lnkData;

    // Pointer to TXBLOCK/MDBLOCK with designation for physical unit of signal data (after conversion) or (only for
    // channel data types "MIME sample" and "MIME stream") to MIME context-type text. (can be NIL).
    // LINK
    private long lnkMdUnit;

    // Pointer to TXBLOCK/MDBLOCK with comment and additional information about the channel. (can be NIL)
    // LINK
    private long lnkMdComment;

    // List of attachments for this channel (references to ATBLOCKs in global linked list of ATBLOCKs).
    // The length of the list is given by cn_attachment_count. It can be empty (cn_attachment_count = 0), i.e. there are
    // no attachments for this channel.
    // LINK
    private long[] lnkAtReference;

    // Only present if "default X" flag (bit 12) is set.
    // Reference to channel to be preferably used as X axis.
    // The reference is a link triple with pointer to parent DGBLOCK, parent CGBLOCK and CNBLOCK for the channel (none
    // of them must be NIL).
    // The referenced channel does not need to have the same raster nor monotonously increasing values. It can be a
    // master channel, e.g. in case several master channels are present. In case of different rasters, visualization may
    // depend on the interpolation method used by the tool.
    // In case no default X channel is specified, the tool is free to choose the X axis; usually a master channels would
    // be used.
    // LINK
    private long[] lnkDefaultX;

    /** Data section */

    // Channel type
    // 0 = fixed length data channel channel value is contained in record.
    // 1 = variable length data channel also denoted as "variable length signal data" (VLSD) channel
    // 2 = master channel for all signals of this group
    // 3 = virtual master channel
    // 4 = synchronization channel
    // 5 = maximum length data channel
    // 6 = virtual data channel
    // UINT8
    private byte channelType;

    // cn_sync_type UINT8 1 Sync type:
    // 0 = None (to be used for normal data channels)
    // 1 = Time (physical values must be seconds)
    // 2 = Angle (physical values must be radians)
    // 3 = Distance (physical values must be meters)
    // 4 = Index (physical values must be zero-based index values)
    // UINT8
    private byte syncType;

    // Channel data type of raw signal value
    // 0 = unsigned integer (LE Byte order)
    // 1 = unsigned integer (BE Byte order)
    // 2 = signed integer (two’s complement) (LE Byte order)
    // 3 = signed integer (two’s complement) (BE Byte order)
    // 4 = IEEE 754 floating-point format (LE Byte order)
    // 5 = IEEE 754 floating-point format (BE Byte order)
    // 6 = String (SBC, standard ASCII encoded (ISO-8859-1 Latin), NULL terminated)
    // 7 = String (UTF-8 encoded, NULL terminated)
    // 8 = String (UTF-16 encoded LE Byte order, NULL terminated)
    // 9 = String (UTF-16 encoded BE Byte order, NULL terminated)
    // 10 = Byte Array with unknown content (e.g. structure)
    // 11 = MIME sample (sample is Byte Array with MIME content-type specified in cn_md_unit)
    // 12 = MIME stream (all samples of channel represent a stream with MIME content-type specified in cn_md_unit)
    // 13 = CANopen date (Based on 7 Byte CANopen Date data structure, see Table 36)
    // 14 = CANopen time (Based on 6 Byte CANopen Time data structure, see Table 37)
    // UINT8
    private byte dataType;

    // Bit offset (0-7): first bit (=LSB) of signal value after Byte offset has been applied.
    // If zero, the signal value is 1-Byte aligned. A value different to zero is only allowed for Integer data types
    // (cn_data_type ≤ 3) and if the Integer signal value fits into 8 contiguous Bytes (cn_bit_count + cn_bit_offset ≤
    // 64). For all other cases, cn_bit_offset must be zero.
    // UINT8
    private byte bitOffset;

    // Offset to first Byte in the data record that contains bits of the signal value. The offset is applied to the
    // plain record data, i.e. skipping the record ID.
    // UINT32
    private long byteOffset;

    // Number of bits for signal value in record.
    // UINT32
    private long bitCount;

    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: All values invalid flag
    // Bit 1: Invalidation bit valid flag
    // Bit 2: Precision valid flag
    // Bit 3: Value range valid flag
    // Bit 4: Limit range valid flag
    // Bit 5: Extended limit range valid flag
    // Bit 6: Discrete value flag
    // Bit 7: Calibration flag
    // Bit 8: Calculated flag
    // Bit 10: Bus event flag
    // Bit 11: Monotonous flag
    // Bit 12: Default X axis flag
    // UINT32
    private long flags;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     */
    public CNBLOCK(SeekableByteChannel sbc) {
        super(sbc);
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

    /**
     * Reads a CGBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position within the channel.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CNBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        CNBLOCK block = new CNBLOCK(channel);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(104);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos);
        channel.read(bb);
        bb.rewind();

        // CHAR 4: Block type identifier, always "##HD"
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

        return block;
    }

}
