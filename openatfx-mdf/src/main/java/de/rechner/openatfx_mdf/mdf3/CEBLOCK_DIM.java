package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * CEBLOCK extension type DIM
 * 
 * @author Christian Rechner
 */
class CEBLOCK_DIM extends BLOCK {

    // UINT16 1 Number of module
    private int numberOfModule;

    // UINT32 1 Address
    private long address;

    // CHAR 80 Description
    private String description;

    // CHAR 32 Identification of ECU
    private String ecuIdent;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CEBLOCK_DIM(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public int getNumberOfModule() {
        return numberOfModule;
    }

    private void setNumberOfModule(int numberOfModule) {
        this.numberOfModule = numberOfModule;
    }

    public long getAddress() {
        return address;
    }

    private void setAddress(long address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public String getEcuIdent() {
        return ecuIdent;
    }

    private void setEcuIdent(String ecuIdent) {
        this.ecuIdent = ecuIdent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "CEBLOCK_DIM [numberOfModule=" + numberOfModule + ", address=" + address + ", description="
                + description + ", ecuIdent=" + ecuIdent + "]";
    }

    /**
     * Reads a CEBLOCK DIM from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CEBLOCK_DIM read(SeekableByteChannel sbc, long pos) throws IOException {
        CEBLOCK_DIM ceBlockDim = new CEBLOCK_DIM(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(118);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(pos);
        sbc.read(bb);
        bb.rewind();

        // UINT16 1 Number of module
        ceBlockDim.setNumberOfModule(Mdf3Util.readInt16(bb));

        // UINT32 1 Address
        ceBlockDim.setAddress(Mdf3Util.readUInt32(bb));

        // CHAR 80 Description
        ceBlockDim.setDescription(Mdf3Util.readChars(bb, 80));

        // CHAR 32 Identification of ECU
        ceBlockDim.setEcuIdent(Mdf3Util.readChars(bb, 32));

        return ceBlockDim;
    }

}
