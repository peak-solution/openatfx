package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE HEADER BLOCK <code>HDBLOCK<code>
 * </p>
 * The HDBLOCK always begins at file position 64. It contains general information about the contents of the measured
 * data file and is the root for the block hierarchy.
 * 
 * @author Christian Rechner
 */
class HDBLOCK extends BLOCK {

    public static String BLOCK_ID = "##HD";

    /** Link section */

    // Pointer to the first data group block (DGBLOCK) (can be NIL)
    // LINK
    private long lnkDgFirst;

    // Pointer to first file history block (FHBLOCK)
    // There must be at least one FHBLOCK with information about the application which created the MDF file.
    // LINK
    private long lnkFhFirst;

    // Pointer to first channel hierarchy block (CHBLOCK) (can be NIL).
    // LINK
    private long lnkChFirst;

    // Pointer to first attachment block (ATBLOCK) (can be NIL)
    // LINK
    private long lnkAtFirst;

    // Pointer to first event block (EVBLOCK) (can be NIL)
    // LINK
    private long lnkEvFirst;

    // Pointer to the measurement file comment (TXBLOCK or MDBLOCK) (can be NIL)
    // LINK
    private long lnkMdComment;

    /** Data section */

    // Time stamp at start of measurement in nanoseconds elapsed since 00:00:00 01.01.1970 (UTC time or local time,
    // depending on "local time" flag, see [UTC]). All time stamps for time synchronized master channels or events are
    // always relative to this start time stamp.
    // UINT64
    private long startTimeNs;

    // Time zone offset in minutes.
    // The value is not necessarily a multiple of 60 and can be negative! For the current time zone definitions, it is
    // expected to be in the range [-840,840] min.
    // For example a value of 60 (min) means UTC+1 time zone = Central European Time
    // Only valid if "time offsets valid" flag is set in time flags.
    // INT16
    private short tzOffsetMin;

    // Daylight saving time (DST) offset in minutes for start time stamp. During the summer months, most regions observe
    // a DST offset of 60 min (1 hour).
    // Only valid if "time offsets valid" flag is set in time flags.
    // INT16
    private short dstOffsetMin;

    // Time flags
    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: Local time flag
    // If set, the start time stamp in nanoseconds represents the local time instead of the UTC time, In this case, time
    // zone and DST offset must not be considered (time offsets flag must not be set). Should only be used if UTC time
    // is unknown.
    // If the bit is not set (default), the start time stamp represents the UTC time.
    // Bit 1: Time offsets valid flag
    // If set, the time zone and DST offsets are valid. Must not be set together with "local time" flag (mutually
    // exclusive).
    // If the offsets are valid, the locally displayed time at start of recording can be determined
    // (after conversion of offsets to ns) by
    // Local time = UTC time + time zone offset + DST offset.
    // UINT8
    private byte timeFlags;

    // Time quality class
    // 0 = local PC reference time (Default)
    // 10 = external time source
    // 16 = external absolute synchronized time
    // UINT8
    private byte timeClass;

    // Flags
    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: Start angle valid flag. If set, the start angle value below is valid.
    // Bit 1: Start distance valid flag. If set, the start distance value below is valid.
    // UINT8
    private byte flags;

    // Start angle in radians at start of measurement (only for angle synchronous measurements)
    // Only valid if "start angle valid" flag is set.
    // REAL
    private double startAngleRad;

    // Start distance in meters at start of measurement
    // (only for distance synchronous measurements)
    // Only valid if "start distance valid" flag is set.
    // All distance values for distance synchronized master channels
    // REAL
    private double startDistanceM;

    private final Path mdfFilePath;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param mdfFilePath THe path to the MDF file.
     */
    public HDBLOCK(SeekableByteChannel sbc, Path mdfFilePath) {
        super(sbc);
        this.mdfFilePath = mdfFilePath;
    }

    public Path getMdfFilePath() {
        return mdfFilePath;
    }

    public long getLnkDgFirst() {
        return lnkDgFirst;
    }

    private void setLnkDgFirst(long lnkDgFirst) {
        this.lnkDgFirst = lnkDgFirst;
    }

    public long getLnkFhFirst() {
        return lnkFhFirst;
    }

    private void setLnkFhFirst(long lnkFhFirst) {
        this.lnkFhFirst = lnkFhFirst;
    }

    public long getLnkChFirst() {
        return lnkChFirst;
    }

    private void setLnkChFirst(long lnkChFirst) {
        this.lnkChFirst = lnkChFirst;
    }

    public long getLnkAtFirst() {
        return lnkAtFirst;
    }

    private void setLnkAtFirst(long lnkAtFirst) {
        this.lnkAtFirst = lnkAtFirst;
    }

    public long getLnkEvFirst() {
        return lnkEvFirst;
    }

    private void setLnkEvFirst(long lnkEvFirst) {
        this.lnkEvFirst = lnkEvFirst;
    }

    public long getLnkMdComment() {
        return lnkMdComment;
    }

    private void setLnkMdComment(long lnkMdComment) {
        this.lnkMdComment = lnkMdComment;
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    private void setStartTimeNs(long startTimeNs) {
        this.startTimeNs = startTimeNs;
    }

    public short getTzOffsetMin() {
        return tzOffsetMin;
    }

    private void setTzOffsetMin(short tzOffsetMin) {
        this.tzOffsetMin = tzOffsetMin;
    }

    public short getDstOffsetMin() {
        return dstOffsetMin;
    }

    private void setDstOffsetMin(short dstOffsetMin) {
        this.dstOffsetMin = dstOffsetMin;
    }

    public byte getTimeFlags() {
        return timeFlags;
    }

    private void setTimeFlags(byte timeFlags) {
        this.timeFlags = timeFlags;
    }

    public byte getTimeClass() {
        return timeClass;
    }

    private void setTimeClass(byte timeClass) {
        this.timeClass = timeClass;
    }

    public byte getFlags() {
        return flags;
    }

    private void setFlags(byte flags) {
        this.flags = flags;
    }

    public double getStartAngleRad() {
        return startAngleRad;
    }

    private void setStartAngleRad(double startAngleRad) {
        this.startAngleRad = startAngleRad;
    }

    public double getStartDistanceM() {
        return startDistanceM;
    }

    private void setStartDistanceM(double startDistanceM) {
        this.startDistanceM = startDistanceM;
    }

    public boolean isLocalTime() {
        return BigInteger.valueOf(this.timeFlags).testBit(0);
    }

    public boolean isTimeFlagsValid() {
        return BigInteger.valueOf(this.timeFlags).testBit(1);
    }

    public boolean isStartAngleValid() {
        return BigInteger.valueOf(this.flags).testBit(0);
    }

    public boolean isStartDistanceValid() {
        return BigInteger.valueOf(this.flags).testBit(1);
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

    public FHBLOCK getFhFirstBlock() throws IOException {
        if (this.lnkMdComment > 0) {
            return FHBLOCK.read(this.sbc, this.lnkFhFirst);
        }
        return null;
    }

    public DGBLOCK getDgFirstBlock() throws IOException {
        if (this.lnkDgFirst > 0) {
            return DGBLOCK.read(this.sbc, this.lnkDgFirst);
        }
        return null;
    }

    @Override
    public String toString() {
        return "HDBLOCK [lnkDgFirst=" + lnkDgFirst + ", lnkFhFirst=" + lnkFhFirst + ", lnkChFirst=" + lnkChFirst
                + ", lnkAtFirst=" + lnkAtFirst + ", lnkEvFirst=" + lnkEvFirst + ", lnkMdComment=" + lnkMdComment
                + ", startTimeNs=" + startTimeNs + ", tzOffsetMin=" + tzOffsetMin + ", dstOffsetMin=" + dstOffsetMin
                + ", timeFlags=" + timeFlags + ", timeClass=" + timeClass + ", flags=" + flags + ", startAngleRad="
                + startAngleRad + ", startDistanceM=" + startDistanceM + "]";
    }

    /**
     * Reads a HDBLOCK from the channel starting at current channel position.
     * 
     * @param mdfFile The path to the MDF file.
     * @param channel The channel to read from.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static HDBLOCK read(Path mdfFile, SeekableByteChannel channel) throws IOException {
        HDBLOCK block = new HDBLOCK(channel, mdfFile);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(112);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(64);
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

        // LINK: Pointer to the first data group block (DGBLOCK) (can be NIL)
        block.setLnkDgFirst(MDFUtil.readLink(bb));

        // LINK: Pointer to first file history block (FHBLOCK)
        block.setLnkFhFirst(MDFUtil.readLink(bb));

        // LINK: Pointer to first channel hierarchy block (CHBLOCK) (can be NIL).
        block.setLnkChFirst(MDFUtil.readLink(bb));

        // LINK: Pointer to first attachment block (ATBLOCK) (can be NIL)
        block.setLnkAtFirst(MDFUtil.readLink(bb));

        // LINK: Pointer to first event block (EVBLOCK) (can be NIL)
        block.setLnkEvFirst(MDFUtil.readLink(bb));

        // LINK: Pointer to the measurement file comment (TXBLOCK or MDBLOCK) (can be NIL)
        block.setLnkMdComment(MDFUtil.readLink(bb));

        // UINT64: Time stamp at start of measurement in nanoseconds elapsed since 00:00:00 01.01.1970
        block.setStartTimeNs(MDFUtil.readUInt64(bb));

        // INT16: Time zone offset in minutes.
        block.setTzOffsetMin(MDFUtil.readInt16(bb));

        // INT16: Daylight saving time (DST) offset in minutes for start time stamp.
        block.setDstOffsetMin(MDFUtil.readInt16(bb));

        // UINT8: Time flags
        block.setTimeFlags(MDFUtil.readUInt8(bb));

        // UINT8: Time quality class
        block.setTimeClass(MDFUtil.readUInt8(bb));

        // UINT8: Flags
        block.setFlags(MDFUtil.readUInt8(bb));
        if (block.getFlags() != 0) {
            throw new IOException("HDBLOCK hd_flags!=0 not yet supported");
        }

        // BYTE: Reserved
        bb.get();

        // REAL: Start angle in radians at start of measurement (only for angle synchronous measurements)
        block.setStartAngleRad(MDFUtil.readReal(bb));

        // REAL: Start distance in meters at start of measurement
        block.setStartDistanceM(MDFUtil.readReal(bb));

        return block;
    }

}
