package de.rechner.openatfx_mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

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
    private String idFile;

    // Format identifier, a textual representation of the format version for display, e.g. "4.11" (including zero
    // termination) or "4.11    " (followed by spaces, no zero termination required if 4 spaces).
    // CHAR 8
    private String idVers;

    // Program identifier, to identify the program which generated the MDF file (no zero termination required).
    // This program identifier serves only for compatibility with previous MDF format versions. Detailed information
    // about the generating application must be written to the first FHBLOCK referenced by the HDBLOCK.
    // As a recommendation, the program identifier inserted into the 8 characters should be the base name (first 8
    // characters) of the EXE/DLL of the writing application. Alternatively, also version information of the application
    // can be appended (e.g. "MyApp45" for version 4.5 of MyApp.exe).
    // CHAR 8
    private String idProg;

    // Version number of the MDF format, i.e. 411
    // UINT16 1
    private int version;

    // UINT16 1 Standard Flags for unfinalized MDF
    private int unfinalizedStandardFlags;

    // UINT16 1 Custom Flags for unfinalized MDF
    private int unfinalizedCustomFlags;

    /**
     * Constructor.
     */
    public IDBLOCK() {}

    public String getIdFile() {
        return idFile;
    }

    public void setIdFile(String idFile) {
        this.idFile = idFile;
    }

    public String getIdVers() {
        return idVers;
    }

    public void setIdVers(String idVers) {
        this.idVers = idVers;
    }

    public String getIdProg() {
        return idProg;
    }

    public void setIdProg(String idProg) {
        this.idProg = idProg;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    /**
     * Reads a IDBLOCK from the channel starting at current channel position.
     * 
     * @param channel The channel to read from.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static IDBLOCK read(SeekableByteChannel channel) throws IOException {
        IDBLOCK idBlock = new IDBLOCK();

        // read block
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(0);
        channel.read(bb);
        bb.rewind();

        // CHAR 8: File identifier
        idBlock.setIdFile(MDFUtil.readChars(bb, 8));
        if (!idBlock.getIdFile().equals("MDF     ")) {
            throw new IOException("Invalid or corrupt MDF4 file: " + idBlock.getIdFile());
        }

        // CHAR 8: Format identifier
        idBlock.setIdVers(MDFUtil.readChars(bb, 8));
        if (!idBlock.getIdVers().startsWith("4")) {
            throw new IOException("Unsupported MDF4 format: " + idBlock.getIdVers());
        }

        // CHAR 8: Program identifier
        idBlock.setIdProg(MDFUtil.readChars(bb, 8));

        // // UINT16 1 Byte order 0 = Little endian
        // idBlock.setByteOrder(MDFUtil.readUInt16(bb));
        // if (idBlock.getByteOrder() != 0) {
        // throw new IOException("Only byte order 'Little endian' is currently supported, found '"
        // + idBlock.getByteOrder() + "'");
        // }
        //
        // // UINT16 1 Floating-point format used 0 = Floating-point format compliant with IEEE 754 standard
        // idBlock.setFloatingPointFormat(MDFUtil.readUInt16(bb));
        // if (idBlock.getFloatingPointFormat() != 0) {
        // throw new IOException("Only floating-point format 'IEEE 754' is currently supported, found '"
        // + idBlock.getFloatingPointFormat() + "'");
        // }
        //
        // // UINT16 1 Version number of the MDF , i.e. 300 for this version
        // idBlock.setVersion(MDFUtil.readUInt16(bb));
        //
        // // UINT16 1 The code page used for all strings in the MDF file except of strings in IDBLOCK and string
        // signals
        // idBlock.setCodePageNumber(MDFUtil.readUInt16(bb));
        //
        // // skip 28 reserved bytes
        // MDFUtil.readChars(bb, 28);
        //
        // // UINT16 1 Standard Flags for unfinalized MDF
        // idBlock.setUnfinalizedStandardFlags(MDFUtil.readUInt16(bb));
        // if (idBlock.getUnfinalizedStandardFlags() != 0) {
        // throw new IOException("Only finalized MDF3 file can be read, found unfinalized standard flag '"
        // + idBlock.getUnfinalizedStandardFlags() + "'");
        // }
        //
        // // UINT16 1 Custom Flags for unfinalized MDF
        // idBlock.setUnfinalizedCustomFlags(MDFUtil.readUInt16(bb));
        // if (idBlock.getUnfinalizedCustomFlags() != 0) {
        // throw new IOException("Only finalized MDF3 file can be read, found unfinalized custom flag '"
        // + idBlock.getUnfinalizedCustomFlags() + "'");
        // }

        return idBlock;
    }
}
