package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;


/**
 * <p>
 * Identification block: Identification of the file as MDF file
 * </p>
 * The IDBLOCK always begins at file position 0. It contains information to identify the file. This includes information
 * about the source of the file and general format specifications.
 * 
 * @author Christian Rechner
 */
public class IDBLOCK extends BLOCK {

    // The path to the MDF file
    private final Path mdfFilePath;

    // CHAR 8 File identifier, always contains "MDF ". ("MDF" followed by five spaces)
    private String idFile;

    // CHAR 8 Format identifier, a textual representation of the format version for display, e.g. "3.00"
    private String idVers;

    // CHAR 8 Program identifier, to identify the program which generated the MDF file
    private String idProg;

    // UINT16 1 Byte order 0 = Little endian
    private int idByteOrder;

    // 0 = Floating-point format compliant with IEEE 754 standard
    // 1 = Floating-point format compliant with G_Float (VAX architecture) (obsolete)
    // 2 = Floating-point format compliant with D_Float (VAX architecture) (obsolete)
    private int idFloatingPointFormat;

    // UINT16 1 Version number of the MDF , i.e. 300 for this version
    private int idVer;

    // UINT16 1 The code page used for all strings in the MDF file except of strings in IDBLOCK and string signals
    // (string encoded in a record).
    // Value = 0: code page is not known.
    // Value > 0: identification number of extended ASCII code page (includes all ANSI and OEM code pages)
    private int idCodePageNumber;

    // UINT16 1 Standard Flags for unfinalized MDF
    private int idUnfinFlags;

    // UINT16 1 Custom Flags for unfinalized MDF
    private int idCustomUnfinFlags;

    /**
     * Constructor.
     * 
     * @param mdfFilePath The path to the MDF file.
     * @param sbc The byte channel pointing to the MDF file.
     */
    private IDBLOCK(Path mdfFilePath, SeekableByteChannel sbc) {
        super(sbc, 0);
        this.mdfFilePath = mdfFilePath;
    }

    public Path getMdfFilePath() {
        return mdfFilePath;
    }

    public String getIdFile() {
        return idFile;
    }

    public String getIdVers() {
        return idVers;
    }

    public String getIdProg() {
        return idProg;
    }

    public int getIdByteOrder() {
        return idByteOrder;
    }

    public int getIdFloatingPointFormat() {
        return idFloatingPointFormat;
    }

    public int getIdVer() {
        return idVer;
    }

    public int getIdCodePageNumber() {
        return idCodePageNumber;
    }

    public int getIdUnfinFlags() {
        return idUnfinFlags;
    }

    public int getIdCustomUnfinFlags() {
        return idCustomUnfinFlags;
    }

    public HDBLOCK getHDBlock() throws IOException {
        return HDBLOCK.read(this.sbc);
    }

    private void setIdFile(String idFile) {
        this.idFile = idFile;
    }

    private void setIdVers(String idVers) {
        this.idVers = idVers;
    }

    private void setIdProg(String idProg) {
        this.idProg = idProg;
    }

    private void setIdByteOrder(int idByteOrder) {
        this.idByteOrder = idByteOrder;
    }

    private void setIdFloatingPointFormat(int idFloatingPointFormat) {
        this.idFloatingPointFormat = idFloatingPointFormat;
    }

    private void setIdVer(int idVer) {
        this.idVer = idVer;
    }

    private void setIdCodePageNumber(int idCodePageNumber) {
        this.idCodePageNumber = idCodePageNumber;
    }

    private void setIdUnfinFlags(int idUnfinFlags) {
        this.idUnfinFlags = idUnfinFlags;
    }

    private void setIdCustomUnfinFlags(int idCustomUnfinFlags) {
        this.idCustomUnfinFlags = idCustomUnfinFlags;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "IDBLOCK [mdfFilePath=" + mdfFilePath + ", idFile=" + idFile + ", idVers=" + idVers + ", idProg="
                + idProg + ", idByteOrder=" + idByteOrder + ", idFloatingPointFormat=" + idFloatingPointFormat
                + ", idVer=" + idVer + ", idCodePageNumber=" + idCodePageNumber + ", idUnfinFlags=" + idUnfinFlags
                + ", idCustomUnfinFlags=" + idCustomUnfinFlags + "]";
    }

    /**
     * Reads a IDBLOCK from the channel starting at current channel position.
     * 
     * @param mdfFile The path to the MDF file.
     * @param sbc The channel to read from.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static IDBLOCK read(Path mdfFilePath, SeekableByteChannel sbc) throws IOException {
        IDBLOCK idBlock = new IDBLOCK(mdfFilePath, sbc);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(0);
        sbc.read(bb);
        bb.rewind();

        // CHAR 8 File identifier, always contains "MDF ". ("MDF" followed by five spaces)
        idBlock.setIdFile(Mdf3Util.readChars(bb, 8));
        if (!idBlock.getIdFile().equals("MDF     ")) {
            throw new IOException("Invalid or corrupt MDF3 file: " + idBlock.getIdFile());
        }

        // CHAR 8 Format identifier, a textual representation of the format version for display, e.g. "3.00"
        idBlock.setIdVers(Mdf3Util.readChars(bb, 8));
        if (!idBlock.getIdVers().startsWith("3")) {
            throw new IOException("Unsupported MDF3 format: " + idBlock.getIdVers());
        }

        // CHAR 8 Program identifier, to identify the program which generated the MDF file
        idBlock.setIdProg(Mdf3Util.readChars(bb, 8));

        // UINT16 1 Byte order 0 = Little endian
        idBlock.setIdByteOrder(Mdf3Util.readUInt16(bb));
        if (idBlock.getIdByteOrder() != 0) {
            throw new IOException("Only byte order 'Little endian' is currently supported, found '"
                    + idBlock.getIdByteOrder() + "'");
        }

        // UINT16 1 Floating-point format used 0 = Floating-point format compliant with IEEE 754 standard
        idBlock.setIdFloatingPointFormat(Mdf3Util.readUInt16(bb));
        if (idBlock.getIdFloatingPointFormat() != 0) {
            throw new IOException("Only floating-point format 'IEEE 754' is currently supported, found '"
                    + idBlock.getIdFloatingPointFormat() + "'");
        }

        // UINT16 1 Version number of the MDF , i.e. 300 for this version
        idBlock.setIdVer(Mdf3Util.readUInt16(bb));

        // UINT16 1 The code page used for all strings in the MDF file except of strings in IDBLOCK and string signals
        idBlock.setIdCodePageNumber(Mdf3Util.readUInt16(bb));

        // skip 28 reserved bytes
        Mdf3Util.readChars(bb, 28);

        // UINT16 1 Standard Flags for unfinalized MDF
        idBlock.setIdUnfinFlags(Mdf3Util.readUInt16(bb));
        if (idBlock.getIdUnfinFlags() != 0) {
            throw new IOException("Only finalized MDF3 file can be read, found unfinalized standard flag '"
                    + idBlock.getIdUnfinFlags() + "'");
        }

        // UINT16 1 Custom Flags for unfinalized MDF
        idBlock.setIdCustomUnfinFlags(Mdf3Util.readUInt16(bb));
        if (idBlock.getIdCustomUnfinFlags() != 0) {
            throw new IOException("Only finalized MDF3 file can be read, found unfinalized custom flag '"
                    + idBlock.getIdCustomUnfinFlags() + "'");
        }

        return idBlock;
    }

}
