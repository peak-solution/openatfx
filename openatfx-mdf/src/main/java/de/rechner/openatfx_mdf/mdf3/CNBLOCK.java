package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * <p>
 * Channel block: Description of a channel
 * </p>
 * This block describes a measurement channel.
 * 
 * @author Christian Rechner
 */
class CNBLOCK extends BLOCK {

    public static String BLOCK_ID = "CN";

    // LINK 1 Pointer to next channel block (CNBLOCK) of this channel group (NIL allowed)
    private long lnkNextCnBlock;

    // LINK 1 Pointer to the conversion formula (CCBLOCK) of this signal (NIL allowed).
    private long lnkCcBlock;

    // LINK 1 Pointer to the source-depending extensions (CEBLOCK) of this signal (NIL allowed)
    private long lnkCeBlock;

    // LINK 1 Pointer to the dependency block (CDBLOCK) of this signal (NIL allowed)
    private long lnkCdBlock;

    // LINK 1 Pointer to the channel comment (TXBLOCK) of this signal (NIL allowed)
    private long lnkChannelComment;

    // UINT16 1 Channel type
    // 0 = data channel
    // 1 = time channel for all signals of this group (in each channel group, exactly one
    // channel must be defined as time channel)
    private int channelType;

    // CHAR 32 Signal name, i.e. the first 32 characters of the ASAM-MCD unique name
    private String signalName;

    // CHAR 128 Signal description
    private String signalDescription;

    // UINT16 1 Number of the first bits [0..n] (bit position within a byte: bit 0 is the least significant
    // bit, bit 7 is the most significant bit)
    private int numberOfFirstBits;

    // UINT16 1 Number of bits
    private int numberOfBits;

    // UINT16 1 Signal data type
    // 0 = unsigned integer
    // 1 = signed integer (two’s complement)
    // 2,3 = IEEE 754 floating-point format
    // 7 = String (NULL terminated)
    // 8 = Byte Array
    private int signalDataType;

    // BOOL 1 Value range – known implementation value
    private boolean knownImplValue;

    // REAL 1 Value range – minimum implementation value
    private double minImplValue;

    // REAL 1 Value range – maximum implementation value
    private double maxImplValue;

    // REAL 1 Rate in which the variable was sampled. Unit [s]
    private double sampleRate;

    // LINK 1 Pointer to the ASAM-MCD unique name (TXBLOCK) (NIL allowed)
    private long lnkMcdUniqueName;

    // LINK 1 Pointer to TXBLOCK that contains the signal's display identifier (default: NIL; NIL allowed)
    private long lnkSignalDisplayIdentifier;

    // UINT16 1 Byte offset of the signal in the data record in addition to bit offset (default value: 0)
    // note: this fields shall only be used if the CGBLOCK record size and the actual offset
    // is larger than 8192 Bytes to ensure compatibility; it enables to write data blocks
    // larger than 8kBytes
    private int byteOffset;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CNBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkNextCnBlock() {
        return lnkNextCnBlock;
    }

    private void setLnkNextCnBlock(long lnkNextCnBlock) {
        this.lnkNextCnBlock = lnkNextCnBlock;
    }

    public long getLnkCcBlock() {
        return lnkCcBlock;
    }

    private void setLnkCcBlock(long lnkCcBlock) {
        this.lnkCcBlock = lnkCcBlock;
    }

    public long getLnkCeBlock() {
        return lnkCeBlock;
    }

    private void setLnkCeBlock(long lnkCeBlock) {
        this.lnkCeBlock = lnkCeBlock;
    }

    public long getLnkCdBlock() {
        return lnkCdBlock;
    }

    private void setLnkCdBlock(long lnkCdBlock) {
        this.lnkCdBlock = lnkCdBlock;
    }

    public long getLnkChannelComment() {
        return lnkChannelComment;
    }

    private void setLnkChannelComment(long lnkChannelComment) {
        this.lnkChannelComment = lnkChannelComment;
    }

    public int getChannelType() {
        return channelType;
    }

    private void setChannelType(int channelType) {
        this.channelType = channelType;
    }

    public String getSignalName() {
        return signalName;
    }

    private void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public String getSignalDescription() {
        return signalDescription;
    }

    private void setSignalDescription(String signalDescription) {
        this.signalDescription = signalDescription;
    }

    public int getNumberOfFirstBits() {
        return numberOfFirstBits;
    }

    private void setNumberOfFirstBits(int numberOfFirstBits) {
        this.numberOfFirstBits = numberOfFirstBits;
    }

    public int getNumberOfBits() {
        return numberOfBits;
    }

    private void setNumberOfBits(int numberOfBits) {
        this.numberOfBits = numberOfBits;
    }

    public int getSignalDataType() {
        return signalDataType;
    }

    private void setSignalDataType(int signalDataType) {
        this.signalDataType = signalDataType;
    }

    public boolean isKnownImplValue() {
        return knownImplValue;
    }

    private void setKnownImplValue(boolean knownImplValue) {
        this.knownImplValue = knownImplValue;
    }

    public double getMinImplValue() {
        return minImplValue;
    }

    private void setMinImplValue(double minImplValue) {
        this.minImplValue = minImplValue;
    }

    public double getMaxImplValue() {
        return maxImplValue;
    }

    private void setMaxImplValue(double maxImplValue) {
        this.maxImplValue = maxImplValue;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    private void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public long getLnkMcdUniqueName() {
        return lnkMcdUniqueName;
    }

    private void setLnkMcdUniqueName(long lnkMcdUniqueName) {
        this.lnkMcdUniqueName = lnkMcdUniqueName;
    }

    public long getLnkSignalDisplayIdentifier() {
        return lnkSignalDisplayIdentifier;
    }

    private void setLnkSignalDisplayIdentifier(long lnkSignalDisplayIdentifier) {
        this.lnkSignalDisplayIdentifier = lnkSignalDisplayIdentifier;
    }

    public int getByteOffset() {
        return byteOffset;
    }

    private void setByteOffset(int byteOffset) {
        this.byteOffset = byteOffset;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "CNBLOCK [lnkNextCnBlock=" + lnkNextCnBlock + ", lnkCcBlock=" + lnkCcBlock + ", lnkCeBlock="
                + lnkCeBlock + ", lnkCdBlock=" + lnkCdBlock + ", lnkChannelComment=" + lnkChannelComment
                + ", channelType=" + channelType + ", signalName=" + signalName + ", signalDescription="
                + signalDescription + ", numberOfFirstBits=" + numberOfFirstBits + ", numberOfBits=" + numberOfBits
                + ", signalDataType=" + signalDataType + ", knownImplValue=" + knownImplValue + ", minImplValue="
                + minImplValue + ", maxImplValue=" + maxImplValue + ", sampleRate=" + sampleRate
                + ", lnkMcdUniqueName=" + lnkMcdUniqueName + ", lnkSignalDisplayIdentifier="
                + lnkSignalDisplayIdentifier + ", byteOffset=" + byteOffset + "]";
    }

    public CNBLOCK getNextCnBlock() throws IOException {
        if (this.lnkNextCnBlock > 0) {
            return CNBLOCK.read(this.sbc, this.lnkNextCnBlock);
        }
        return null;
    }

    public CCBLOCK getCcBlock() throws IOException {
        if (this.lnkCcBlock > 0) {
            return CCBLOCK.read(this.sbc, this.lnkCcBlock);
        }
        return null;
    }

    public CEBLOCK getCeblock() throws IOException {
        if (this.lnkCeBlock > 0) {
            return CEBLOCK.read(this.sbc, this.lnkCeBlock);
        }
        return null;
    }

    public TXBLOCK getChannelComment() throws IOException {
        if (this.lnkChannelComment > 0) {
            return TXBLOCK.read(this.sbc, this.lnkChannelComment);
        }
        return null;
    }

    public TXBLOCK getMcdUniqueName() throws IOException {
        if (this.lnkMcdUniqueName > 0) {
            return TXBLOCK.read(this.sbc, this.lnkMcdUniqueName);
        }
        return null;
    }

    public TXBLOCK getSignalDisplayIdentifier() throws IOException {
        if (this.lnkSignalDisplayIdentifier > 0) {
            return TXBLOCK.read(this.sbc, this.lnkSignalDisplayIdentifier);
        }
        return null;
    }

    /**
     * Reads a CNBLOCK from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CNBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
        CNBLOCK block = new CNBLOCK(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(228);
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

        // LINK 1 Pointer to next channel block (CNBLOCK) of this channel group (NIL allowed)
        block.setLnkNextCnBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the conversion formula (CCBLOCK) of this signal (NIL allowed).
        block.setLnkCcBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the source-depending extensions (CEBLOCK) of this signal (NIL allowed)
        block.setLnkCeBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the dependency block (CDBLOCK) of this signal (NIL allowed)
        block.setLnkCdBlock(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to the channel comment (TXBLOCK) of this signal (NIL allowed)
        block.setLnkChannelComment(Mdf3Util.readLink(bb));

        // UINT16 1 Channel type
        // 0 = data channel
        // 1 = time channel for all signals of this group (in each channel group, exactly one
        // channel must be defined as time channel)
        block.setChannelType(Mdf3Util.readUInt16(bb));

        // CHAR 32 Signal name, i.e. the first 32 characters of the ASAM-MCD unique name
        block.setSignalName(Mdf3Util.readChars(bb, 32));

        // CHAR 128 Signal description
        block.setSignalDescription(Mdf3Util.readChars(bb, 128));

        // UINT16 1 Number of the first bits [0..n] (bit position within a byte: bit 0 is the least significant
        // bit, bit 7 is the most significant bit)
        block.setNumberOfFirstBits(Mdf3Util.readUInt16(bb));

        // UINT16 1 Number of bits
        block.setNumberOfBits(Mdf3Util.readUInt16(bb));

        // UINT16 1 Signal data type
        // 0 = unsigned integer
        // 1 = signed integer (two’s complement)
        // 2,3 = IEEE 754 floating-point format
        // 7 = String (NULL terminated)
        // 8 = Byte Array
        block.setSignalDataType(Mdf3Util.readUInt16(bb));

        // BOOL 1 Value range – known implementation value
        block.setKnownImplValue(Mdf3Util.readBool(bb));

        // REAL 1 Value range – minimum implementation value
        block.setMinImplValue(Mdf3Util.readReal(bb));

        // REAL 1 Value range – maximum implementation value
        block.setMaxImplValue(Mdf3Util.readReal(bb));

        // REAL 1 Rate in which the variable was sampled. Unit [s]
        block.setSampleRate(Mdf3Util.readReal(bb));

        // LINK 1 Pointer to the ASAM-MCD unique name (TXBLOCK) (NIL allowed)
        block.setLnkMcdUniqueName(Mdf3Util.readLink(bb));

        // LINK 1 Pointer to TXBLOCK that contains the signal's display identifier (default: NIL; NIL allowed)
        block.setLnkSignalDisplayIdentifier(Mdf3Util.readLink(bb));

        // UINT16 1 Byte offset of the signal in the data record in addition to bit offset (default value: 0)
        // note: this fields shall only be used if the CGBLOCK record size and the actual offset
        // is larger than 8192 Bytes to ensure compatibility; it enables to write data blocks
        // larger than 8kBytes
        block.setByteOffset(Mdf3Util.readUInt16(bb));

        return block;
    }

}
