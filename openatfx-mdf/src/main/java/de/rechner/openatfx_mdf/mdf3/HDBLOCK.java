package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * Header block: General description of the measurement file
 * </p>
 * The HDBLOCK always begins at file position 64. It contains general information about the contents of the measured
 * data file.
 * 
 * @author Christian Rechner
 */
class HDBLOCK extends BLOCK {

    public static String BLOCK_ID = "HD";

    // LINK 1 Pointer to the first file group block (DGBLOCK)
    private long lnkFirstFileGroup;

    // LINK 1 Pointer to the measurement file comment text (TXBLOCK) (NIL allowed)
    private long lnkFileCommentTxt;

    // LINK 1 Pointer to program block (PRBLOCK) (NIL allowed)
    private long lnkProgramBlock;

    // UINT16 1 Number of data groups
    private int numberOfDataGroups;

    // CHAR 10 Date at which the recording was started in “DD:MM:YYYY” format
    private String dateStarted;

    // CHAR 8 Time at which the recording was started in “HH:MM:SS” format
    private String timeStarted;

    // CHAR 32 Author’s name
    private String author;

    // CHAR 32 Name of organization or department
    private String department;

    // CHAR 32 Project name
    private String projectName;

    // CHAR 32 Measurement object e. g. the vehicle identification
    private String meaObject;

    // UINT64 1 Time stamp at which recording was started in nanoseconds. Elapsed time since 00:00:00 01.01.1970 (local
    // time) (local time = UTC time + UTC time offset) Note: the local time does not contain a daylight saving time
    // (DST) offset! Valid since version 3.20. Default value: 0 See remark below
    private long timestamp;

    // INT16 1 UTC time offset in hours (= GMT time zone) For example 1 means GMT+1 time zone = Central European Time
    // (CET). The value must be in range [-12, 12], i.e. it can be negative! Valid since version 3.20. Default value: 0
    // (= GMT time)
    private int utcTimeOffsetHours;

    // UINT16 1 Time quality class
    // 0 = local PC reference time (Default)
    // 10 = external time source
    // 16 = external absolute synchronized time
    private int timeQualityClass;

    // CHAR 32 Timer identification (time source), e.g. "Local PC Reference Time" or "GPS Reference Time". Valid since
    // version 3.20. Default value: empty string
    private String timerIdent;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private HDBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkFirstFileGroup() {
        return lnkFirstFileGroup;
    }

    private void setLnkFirstFileGroup(long lnkFirstFileGroup) {
        this.lnkFirstFileGroup = lnkFirstFileGroup;
    }

    public long getLnkFileCommentTxt() {
        return lnkFileCommentTxt;
    }

    private void setLnkFileCommentTxt(long lnkFileCommentTxt) {
        this.lnkFileCommentTxt = lnkFileCommentTxt;
    }

    public long getLnkProgramBlock() {
        return lnkProgramBlock;
    }

    private void setLnkProgramBlock(long lnkProgramBlock) {
        this.lnkProgramBlock = lnkProgramBlock;
    }

    public int getNumberOfDataGroups() {
        return numberOfDataGroups;
    }

    private void setNumberOfDataGroups(int numberOfDataGroups) {
        this.numberOfDataGroups = numberOfDataGroups;
    }

    public String getDateStarted() {
        return dateStarted;
    }

    private void setDateStarted(String dateStarted) {
        this.dateStarted = dateStarted;
    }

    public String getTimeStarted() {
        return timeStarted;
    }

    private void setTimeStarted(String timeStarted) {
        this.timeStarted = timeStarted;
    }

    public String getAuthor() {
        return author;
    }

    private void setAuthor(String author) {
        this.author = author;
    }

    public String getDepartment() {
        return department;
    }

    private void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectName() {
        return projectName;
    }

    private void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getMeaObject() {
        return meaObject;
    }

    private void setMeaObject(String meaObject) {
        this.meaObject = meaObject;
    }

    public long getTimestamp() {
        return timestamp;
    }

    private void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUtcTimeOffsetHours() {
        return utcTimeOffsetHours;
    }

    private void setUtcTimeOffsetHours(int utcTimeOffsetHours) {
        this.utcTimeOffsetHours = utcTimeOffsetHours;
    }

    public int getTimeQualityClass() {
        return timeQualityClass;
    }

    private void setTimeQualityClass(int timeQualityClass) {
        this.timeQualityClass = timeQualityClass;
    }

    public String getTimerIdent() {
        return timerIdent;
    }

    private void setTimerIdent(String timerIdent) {
        this.timerIdent = timerIdent;
    }

    public DGBLOCK getFirstFileGroup() throws IOException {
        if (this.lnkFirstFileGroup > 0) {
            return DGBLOCK.read(this.sbc, this.lnkFirstFileGroup);
        }
        return null;
    }

    public TXBLOCK getFileCommentTxt() throws IOException {
        if (this.lnkFileCommentTxt > 0) {
            return TXBLOCK.read(this.sbc, this.lnkFileCommentTxt);
        }
        return null;
    }

    public PRBLOCK getProgramBlock() throws IOException {
        if (this.lnkProgramBlock > 0) {
            return PRBLOCK.read(this.sbc, this.lnkProgramBlock);
        }
        return null;
    }

    @Override
    public String toString() {
        return "HDBLOCK [lnkFirstFileGroup=" + lnkFirstFileGroup + ", lnkFileCommentTxt=" + lnkFileCommentTxt
                + ", lnkProgramBlock=" + lnkProgramBlock + ", numberOfDataGroups=" + numberOfDataGroups
                + ", dateStarted=" + dateStarted + ", timeStarted=" + timeStarted + ", author=" + author
                + ", department=" + department + ", projectName=" + projectName + ", meaObject=" + meaObject
                + ", timestamp=" + timestamp + ", utcTimeOffsetHours=" + utcTimeOffsetHours + ", timeQualityClass="
                + timeQualityClass + ", timerIdent=" + timerIdent + "]";
    }

    /**
     * Reads a HDBLOCK from the channel starting at current channel position.
     * 
     * @param sbc The channel to read from.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static HDBLOCK read(SeekableByteChannel sbc) throws IOException {
        HDBLOCK block = new HDBLOCK(sbc, 64);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(64);
        sbc.read(bb);
        bb.rewind();

        // CHAR 2 Block type identifier
        block.setId(Mdf3Util.readChars(bb, 2));
        if (!block.getId().equals(BLOCK_ID)) {
            throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
        }

        // UINT16 1 Block size of this block in bytes
        block.setLength(Mdf3Util.readUInt16(bb));

        // read block header
        bb = ByteBuffer.allocate(block.getLength() - 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(68);
        sbc.read(bb);
        bb.rewind();

        // LINK 1 Pointer to the first file group block (DGBLOCK)
        block.setLnkFirstFileGroup(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the measurement file comment text (TXBLOCK) (NIL allowed)
        block.setLnkFileCommentTxt(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to program block (PRBLOCK) (NIL allowed)
        block.setLnkProgramBlock(Mdf3Util.readLink(bb));

        // UINT16 1 Number of data groups
        block.setNumberOfDataGroups(Mdf3Util.readUInt16(bb));

        // CHAR 10 Date at which the recording was started in “DD:MM:YYYY” format
        block.setDateStarted(Mdf3Util.readChars(bb, 10));

        // CHAR 8 Time at which the recording was started in “HH:MM:SS” format
        block.setTimeStarted(Mdf3Util.readChars(bb, 8));

        // CHAR 32 Author’s name
        block.setAuthor(Mdf3Util.readChars(bb, 32));

        // CHAR 32 Name of organization or department
        block.setDepartment(Mdf3Util.readChars(bb, 32));

        // CHAR 32 Project name
        block.setProjectName(Mdf3Util.readChars(bb, 32));

        // CHAR 32 Measurement object e. g. the vehicle identification
        block.setMeaObject(Mdf3Util.readChars(bb, 32));

        // new fields from MDF version >3.20
        if (block.getLength() > 164) {

            // UINT64 1 Time stamp at which recording was started in nanoseconds
            BigInteger nanoseconds = Mdf3Util.readUInt64(bb);
            block.setTimestamp(nanoseconds.longValue());

            // INT16 1 UTC time offset in hours (= GMT time zone)
            block.setUtcTimeOffsetHours(Mdf3Util.readInt16(bb));

            // UINT16 1 Time quality class
            block.setTimeQualityClass(Mdf3Util.readUInt16(bb));

            // CHAR 32 Timer identification (time source)
            block.setTimerIdent(Mdf3Util.readChars(bb, 32));

        }

        return block;
    }

}
