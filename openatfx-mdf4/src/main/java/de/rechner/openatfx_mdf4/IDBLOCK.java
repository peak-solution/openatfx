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
    private int idVer;

    // Standard flags for unfinalized MDF.
    // Bit combination of flags that indicate the steps required to finalize the MDF file.
    // For a finalized MDF file, the value must be 0 (no flag set).
    // UINT16 1
    private int idUnfinFlags;

    // Custom flags for unfinalized MDF.
    // Bit combination of flags that indicate custom steps required to finalize the MDF file.
    // For a finalized MDF file, the value must be 0 (no flag set).
    // UINT16 1
    private int idCustomUnfinFlags;

    /**
     * Constructor.
     */
    private IDBLOCK() {}

    public String getIdFile() {
        return idFile;
    }

    private void setIdFile(String idFile) {
        this.idFile = idFile;
    }

    public String getIdVers() {
        return idVers;
    }

    private void setIdVers(String idVers) {
        this.idVers = idVers;
    }

    public String getIdProg() {
        return idProg;
    }

    private void setIdProg(String idProg) {
        this.idProg = idProg;
    }

    public int getIdVer() {
        return idVer;
    }

    private void setIdVer(int idVer) {
        this.idVer = idVer;
    }

    public int getIdUnfinFlags() {
        return idUnfinFlags;
    }

    private void setIdUnfinFlags(int idUnfinFlags) {
        this.idUnfinFlags = idUnfinFlags;
    }

    public int getIdCustomUnfinFlags() {
        return idCustomUnfinFlags;
    }

    private void setIdCustomUnfinFlags(int idCustomUnfinFlags) {
        this.idCustomUnfinFlags = idCustomUnfinFlags;
    }

    @Override
    public String toString() {
        return "IDBLOCK [idFile=" + idFile + ", idVers=" + idVers + ", idProg=" + idProg + ", idVer=" + idVer
                + ", idUnfinFlags=" + idUnfinFlags + ", idCustomUnfinFlags=" + idCustomUnfinFlags + "]";
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
        idBlock.setIdFile(MDFUtil.readCharsISO8859(bb, 8));
        if (!idBlock.getIdFile().equals("MDF     ")) {
            throw new IOException("Invalid or corrupt MDF4 file: " + idBlock.getIdFile());
        }

        // CHAR 8: Format identifier
        idBlock.setIdVers(MDFUtil.readCharsISO8859(bb, 8));
        if (!idBlock.getIdVers().startsWith("4")) {
            throw new IOException("Unsupported MDF4 format: " + idBlock.getIdVers());
        }

        // CHAR 8: Program identifier
        idBlock.setIdProg(MDFUtil.readCharsISO8859(bb, 8));

        // BYTE 4: id_reserved
        bb.get(new byte[4]);

        // UINT16: Version number
        idBlock.setIdVer(MDFUtil.readUInt16(bb));
        if (idBlock.getIdVer() < 400) {
            throw new IOException("Unsupported MDF version, must be >400: " + idBlock.getIdVer());
        }

        // UINT16: Standard flags for unfinalized MDF.
        idBlock.setIdUnfinFlags(MDFUtil.readUInt16(bb));
        if (idBlock.getIdCustomUnfinFlags() != 0) {
            throw new IOException("Only finalized MDF file can be read, found unfinalized standard flag '"
                    + idBlock.getIdUnfinFlags() + "'");
        }

        // UINT16: Custom Flags for unfinalized MDF
        idBlock.setIdCustomUnfinFlags(MDFUtil.readUInt16(bb));
        if (idBlock.getIdCustomUnfinFlags() != 0) {
            throw new IOException("Only finalized MDF file can be read, found unfinalized custom flag '"
                    + idBlock.getIdCustomUnfinFlags() + "'");
        }

        return idBlock;
    }

}
