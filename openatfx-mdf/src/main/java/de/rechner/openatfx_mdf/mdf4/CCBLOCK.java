package de.rechner.openatfx_mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;


/**
 * <p>
 * THE CHANNEL CONVERSION BLOCK <code>CCBLOCK<code>
 * </p>
 * The data records can be used to store raw values (often also denoted as implementation values or internal values).
 * The CCBLOCK serves to specify a conversion formula to convert the raw values to physical values with a physical unit.
 * The result of a conversion always is either a floating-point value (REAL) or a character string (UTF-8).
 * 
 * @author Christian Rechner
 */
class CCBLOCK extends BLOCK {

    public static String BLOCK_ID = "##CC";

    /** Link section */

    // Link to TXBLOCK with name (identifier) of conversion (can be NIL).
    // LINK
    private long lnkTxName;

    // Link to TXBLOCK/MDBLOCK with physical unit of signal data (after conversion). (can be NIL)
    // LINK
    private long lnkMdUnit;

    // Link to TXBLOCK/MDBLOCK with comment of conversion and additional information. (can be NIL)
    // LINK
    private long lnkMdComment;

    // Link to CCBLOCK for inverse formula (can be NIL, must be NIL for CCBLOCK of the inverse formula (no cyclic
    // reference allowed).
    // LINK
    private long lnkCcInverse;

    // List of additional links to TXBLOCKs with strings or to CCBLOCKs with partial conversion rules. Length of list is
    // given by cc_ref_count. The list can be empty. Details are explained in formula-specific block supplement.
    // LINK
    private long[] lnkCcRef;

    /** Data section */

    // Conversion type (formula identifier)
    // 0 = 1:1 conversion (in this case, the CCBLOCK can be omitted)
    // 1 = linear conversion
    // 2 = rational conversion
    // 3 = algebraic conversion (MCD-2 MC text formula)
    // 4 = value to value tabular look-up with interpolation
    // 5 = value to value tabular look-up without interpolation
    // 6 = value range to value tabular look-up
    // 7 = value to text/scale conversion tabular look-up
    // 8 = value range to text/scale conversion tabular look-up
    // 9 = text to value tabular look-up
    // 10 = text to text tabular look-up (translation)
    // UINT8
    private byte type;

    // Precision for display of floating point values.
    // 0xFF means unrestricted precision (infinite)
    // Any other value specifies the number of decimal places to use for display of floating point values.
    // Note: only valid if "precision valid" flag (bit 0) is set and if cn_precision of the parent CNBLOCK is invalid,
    // otherwise cn_precision must be used.
    // UINT8
    private byte precision;

    // Flags
    // The value contains the following bit flags (Bit 0 = LSB):
    // Bit 0: Precision valid flag
    // Bit 1: Physical value range valid flag
    // Bit 2: Status string flag
    // UINT16
    private int flags;

    // Length M of cc_ref list with additional links.
    // UINT16
    private int refCount;

    // Length N of cc_val list with additional parameters.
    // UINT16
    private int valCount;

    // Minimum physical signal value that occurred for this signal.
    // REAL
    private double phyRangeMin;

    // Maximum physical signal value that occurred for this signal.
    // REAL
    private double phyRangeMax;

    // List of additional conversion parameters.
    // Length of list is given by cc_val_count. The list can be empty.
    // REAL N
    private double[] val;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CCBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getLnkTxName() {
        return lnkTxName;
    }

    public long getLnkMdUnit() {
        return lnkMdUnit;
    }

    public long getLnkMdComment() {
        return lnkMdComment;
    }

    public long getLnkCcInverse() {
        return lnkCcInverse;
    }

    public long[] getLnkCcRef() {
        return lnkCcRef;
    }

    public byte getType() {
        return type;
    }

    public byte getPrecision() {
        return precision;
    }

    public int getFlags() {
        return flags;
    }

    public int getRefCount() {
        return refCount;
    }

    public int getValCount() {
        return valCount;
    }

    public double getPhyRangeMin() {
        return phyRangeMin;
    }

    public double getPhyRangeMax() {
        return phyRangeMax;
    }

    public double[] getVal() {
        return val;
    }

    private void setLnkTxName(long lnkTxName) {
        this.lnkTxName = lnkTxName;
    }

    private void setLnkMdUnit(long lnkMdUnit) {
        this.lnkMdUnit = lnkMdUnit;
    }

    private void setLnkMdComment(long lnkMdComment) {
        this.lnkMdComment = lnkMdComment;
    }

    private void setLnkCcInverse(long lnkCcInverse) {
        this.lnkCcInverse = lnkCcInverse;
    }

    private void setLnkCcRef(long[] lnkCcRef) {
        this.lnkCcRef = lnkCcRef;
    }

    private void setType(byte type) {
        this.type = type;
    }

    private void setPrecision(byte precision) {
        this.precision = precision;
    }

    private void setFlags(int flags) {
        this.flags = flags;
    }

    private void setRefCount(int refCount) {
        this.refCount = refCount;
    }

    private void setValCount(int valCount) {
        this.valCount = valCount;
    }

    private void setPhyRangeMin(double phyRangeMin) {
        this.phyRangeMin = phyRangeMin;
    }

    private void setPhyRangeMax(double phyRangeMax) {
        this.phyRangeMax = phyRangeMax;
    }

    private void setVal(double[] val) {
        this.val = val;
    }

    public TXBLOCK getCnTxNameBlock() throws IOException {
        if (this.lnkTxName > 0) {
            return TXBLOCK.read(this.sbc, this.lnkTxName);
        }
        return null;
    }

    public BLOCK getMdUnitBlock() throws IOException {
        if (this.lnkMdUnit > 0) {
            String blockType = getBlockType(this.sbc, this.lnkMdUnit);
            // link points to a MDBLOCK
            if (blockType.equals(MDBLOCK.BLOCK_ID)) {
                return MDBLOCK.read(this.sbc, this.lnkMdUnit);
            }
            // links points to TXBLOCK
            else if (blockType.equals(TXBLOCK.BLOCK_ID)) {
                return TXBLOCK.read(this.sbc, this.lnkMdUnit);
            }
            // unknown
            else {
                throw new IOException("Unsupported block type for MdUnit: " + blockType);
            }
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

    public CCBLOCK getCcInverseBlock() throws IOException {
        if (this.lnkCcInverse > 0) {
            return CCBLOCK.read(this.sbc, this.lnkCcInverse);
        }
        return null;
    }

    public TXBLOCK[] getCcRefBlocks() throws IOException {
        if (this.lnkCcRef.length > 0) {
            TXBLOCK[] ccRef = new TXBLOCK[this.lnkCcRef.length];
            for (int i = 0; i < ccRef.length; i++) {
                if (lnkCcRef[i] > 0) {
                    ccRef[i] = TXBLOCK.read(this.sbc, this.lnkCcRef[i]);
                }
            }
            return ccRef;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf4.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "CCBLOCK [lnkTxName=" + lnkTxName + ", lnkMdUnit=" + lnkMdUnit + ", lnkMdComment=" + lnkMdComment
                + ", lnkCcInverse=" + lnkCcInverse + ", lnkCcRef=" + Arrays.toString(lnkCcRef) + ", type=" + type
                + ", precision=" + precision + ", flags=" + flags + ", refCount=" + refCount + ", valCount=" + valCount
                + ", phyRangeMin=" + phyRangeMin + ", phyRangeMax=" + phyRangeMax + ", val=" + Arrays.toString(val)
                + "]";
    }

    /**
     * Reads a CCBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @param pos The position
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CCBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
        CCBLOCK block = new CCBLOCK(channel, pos);

        // read block header
        ByteBuffer bb = ByteBuffer.allocate(24);
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

        // read block content
        bb = ByteBuffer.allocate((int) block.getLength() + 24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(pos + 24);
        channel.read(bb);
        bb.rewind();

        // read links
        long[] lnks = new long[(int) block.getLinkCount()];
        for (int i = 0; i < lnks.length; i++) {
            lnks[i] = MDF4Util.readLink(bb);
        }

        // read data

        // UINT8: Conversion type (formula identifier)
        block.setType(MDF4Util.readUInt8(bb));

        // UINT8: Precision for display of floating point values.
        block.setPrecision(MDF4Util.readUInt8(bb));

        // UINT16: Flags.
        block.setFlags(MDF4Util.readUInt16(bb));

        // UINT16: Length M of cc_ref list with additional links.
        block.setRefCount(MDF4Util.readUInt16(bb));

        // UINT16: Length N of cc_val list with additional.
        block.setValCount(MDF4Util.readUInt16(bb));

        // REAL: Minimum physical signal value that occurred for this signal.
        block.setPhyRangeMin(MDF4Util.readReal(bb));

        // REAL: Maximum physical signal value that occurred for this signal.
        block.setPhyRangeMax(MDF4Util.readReal(bb));

        // REAL N: List of additional conversion parameters.
        double[] val = new double[block.getValCount()];
        for (int i = 0; i < val.length; i++) {
            val[i] = MDF4Util.readReal(bb);
        }
        block.setVal(val);

        // extract links after reading data (then we know how many attachments)
        block.setLnkTxName(lnks[0]);
        block.setLnkMdUnit(lnks[1]);
        block.setLnkMdComment(lnks[2]);
        block.setLnkCcInverse(lnks[3]);
        long[] lnkCcRef = new long[block.getRefCount()];
        for (int i = 0; i < lnkCcRef.length; i++) {
            lnkCcRef[i] = lnks[i + 4];
        }
        block.setLnkCcRef(lnkCcRef);

        return block;
    }

}
