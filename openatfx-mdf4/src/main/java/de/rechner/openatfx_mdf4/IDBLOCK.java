package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import de.rechner.openatfx_mdf4.util.MDFUtil;


/**
 * <p>
 * THE FILE IDENTIFICATION BLOCK <code>IDBLOCK<code>
 * </p>
 * The IDBLOCK always begins at file position 0 and has a constant length of 64 Bytes. It contains information to
 * identify the file. This includes information about the source of the file and general format specifications. To be
 * compliant with older MDF formats in this section each CHAR must be a 1-Byte ASCII character. The IDBLOCK is the only
 * block without a Header section and without a Link section.
 * 
 * @author Christian Rechner
 */
class IDBLOCK {

    // File identifier, always contains "MDF     " ("MDF" followed by five spaces, no zero termination), except for
    // "unfinalized" MDF files. The file identifier for unfinalized MDF files contains "UnFinMF " ("UnFinMF" followed by
    // one space, no zero termination).
    // CHAR 8
    private String id_file;

    // Format identifier, a textual representation of the format version for display, e.g. "4.11" (including zero
    // termination) or "4.11    " (followed by spaces, no zero termination required if 4 spaces).
    // CHAR 8
    private String id_vers;


    
    
    //
    //
    //

    // CHAR 8 File identifier, always contains "MDF ". ("MDF" followed by five spaces)
    private String fileIdent;

    // CHAR 8 Format identifier, a textual representation of the format version for display, e.g. "3.00"
    private String formatIdent;

    // CHAR 8 Program identifier, to identify the program which generated the MDF file
    private String programIdent;

    // UINT16 1 Byte order 0 = Little endian
    private int byteOrder;

    // 0 = Floating-point format compliant with IEEE 754 standard
    // 1 = Floating-point format compliant with G_Float (VAX architecture) (obsolete)
    // 2 = Floating-point format compliant with D_Float (VAX architecture) (obsolete)
    private int floatingPointFormat;

    // UINT16 1 Version number of the MDF , i.e. 300 for this version
    private int version;

    // UINT16 1 The code page used for all strings in the MDF file except of strings in IDBLOCK and string signals
    // (string encoded in a record).
    // Value = 0: code page is not known.
    // Value > 0: identification number of extended ASCII code page (includes all ANSI and OEM code pages)
    private int codePageNumber;

    // UINT16 1 Standard Flags for unfinalized MDF
    private int unfinalizedStandardFlags;

    // UINT16 1 Custom Flags for unfinalized MDF
    private int unfinalizedCustomFlags;

    /**
     * Constructor.
     */
    public IDBLOCK() {}

    public String getFileIdent() {
        return fileIdent;
    }

    public void setFileIdent(String fileIdent) {
        this.fileIdent = fileIdent;
    }

    public String getFormatIdent() {
        return formatIdent;
    }

    public void setFormatIdent(String formatIdent) {
        this.formatIdent = formatIdent;
    }

    public String getProgramIdent() {
        return programIdent;
    }

    public void setProgramIdent(String programIdent) {
        this.programIdent = programIdent;
    }

    public int getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(int byteOrder) {
        this.byteOrder = byteOrder;
    }

    public int getFloatingPointFormat() {
        return floatingPointFormat;
    }

    public void setFloatingPointFormat(int floatingPointFormat) {
        this.floatingPointFormat = floatingPointFormat;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getCodePageNumber() {
        return codePageNumber;
    }

    public void setCodePageNumber(int codePageNumber) {
        this.codePageNumber = codePageNumber;
    }

    public int getUnfinalizedStandardFlags() {
        return unfinalizedStandardFlags;
    }

    public void setUnfinalizedStandardFlags(int unfinalizedStandardFlags) {
        this.unfinalizedStandardFlags = unfinalizedStandardFlags;
    }

    public int getUnfinalizedCustomFlags() {
        return unfinalizedCustomFlags;
    }

    public void setUnfinalizedCustomFlags(int unfinalizedCustomFlags) {
        this.unfinalizedCustomFlags = unfinalizedCustomFlags;
    }

    @Override
    public String toString() {
        return "IDBLOCK [fileIdent=" + fileIdent + ", formatIdent=" + formatIdent + ", programIdent=" + programIdent
                + ", byteOrder=" + byteOrder + ", floatingPointFormat=" + floatingPointFormat + ", version=" + version
                + ", codePageNumber=" + codePageNumber + ", unfinalizedStandardFlags=" + unfinalizedStandardFlags
                + ", unfinalizedCustomFlags=" + unfinalizedCustomFlags + "]";
    }

    /**
     * Reads a IDBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static IDBLOCK read(FileChannel channel) throws IOException {
        IDBLOCK idBlock = new IDBLOCK();

        // read block
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(0);
        channel.read(bb);
        bb.rewind();

        // CHAR 8 File identifier, always contains "MDF ". ("MDF" followed by five spaces)
        idBlock.setFileIdent(MDFUtil.readChars(bb, 8));
        if (!idBlock.getFileIdent().equals("MDF     ")) {
            throw new IOException("Invalid or corrupt MDF4 file: " + idBlock.getFileIdent());
        }

        // CHAR 8 Format identifier, a textual representation of the format version for display, e.g. "3.00"
        idBlock.setFormatIdent(MDFUtil.readChars(bb, 8));
        if (!idBlock.getFormatIdent().startsWith("4")) {
            throw new IOException("Unsupported MDF4 format: " + idBlock.getFormatIdent());
        }

        // CHAR 8 Program identifier, to identify the program which generated the MDF file
        idBlock.setProgramIdent(MDFUtil.readChars(bb, 8));

        // UINT16 1 Byte order 0 = Little endian
        idBlock.setByteOrder(MDFUtil.readUInt16(bb));
        if (idBlock.getByteOrder() != 0) {
            throw new IOException("Only byte order 'Little endian' is currently supported, found '"
                    + idBlock.getByteOrder() + "'");
        }

        // UINT16 1 Floating-point format used 0 = Floating-point format compliant with IEEE 754 standard
        idBlock.setFloatingPointFormat(MDFUtil.readUInt16(bb));
        if (idBlock.getFloatingPointFormat() != 0) {
            throw new IOException("Only floating-point format 'IEEE 754' is currently supported, found '"
                    + idBlock.getFloatingPointFormat() + "'");
        }

        // UINT16 1 Version number of the MDF , i.e. 300 for this version
        idBlock.setVersion(MDFUtil.readUInt16(bb));

        // UINT16 1 The code page used for all strings in the MDF file except of strings in IDBLOCK and string signals
        idBlock.setCodePageNumber(MDFUtil.readUInt16(bb));

        // skip 28 reserved bytes
        MDFUtil.readChars(bb, 28);

        // UINT16 1 Standard Flags for unfinalized MDF
        idBlock.setUnfinalizedStandardFlags(MDFUtil.readUInt16(bb));
        if (idBlock.getUnfinalizedStandardFlags() != 0) {
            throw new IOException("Only finalized MDF3 file can be read, found unfinalized standard flag '"
                    + idBlock.getUnfinalizedStandardFlags() + "'");
        }

        // UINT16 1 Custom Flags for unfinalized MDF
        idBlock.setUnfinalizedCustomFlags(MDFUtil.readUInt16(bb));
        if (idBlock.getUnfinalizedCustomFlags() != 0) {
            throw new IOException("Only finalized MDF3 file can be read, found unfinalized custom flag '"
                    + idBlock.getUnfinalizedCustomFlags() + "'");
        }

        return idBlock;
    }

}
