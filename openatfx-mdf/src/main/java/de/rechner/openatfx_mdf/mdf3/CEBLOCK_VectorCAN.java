package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;


/**
 * CEBLOCK extension type Vector CAN
 * 
 * @author Christian Rechner
 */
class CEBLOCK_VectorCAN extends BLOCK {

    // UINT32 1 Identifier of CAN message
    private long messageId;

    // UINT32 1 Index of CAN channel
    private long canIndex;

    // CHAR 36 Name of message (string should be terminated by 0)
    private String messageName;

    // CHAR 36 Name of sender (string should be terminated by 0)
    private String senderName;

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CEBLOCK_VectorCAN(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public long getMessageId() {
        return messageId;
    }

    private void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getCanIndex() {
        return canIndex;
    }

    private void setCanIndex(long canIndex) {
        this.canIndex = canIndex;
    }

    public String getMessageName() {
        return messageName;
    }

    private void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public String getSenderName() {
        return senderName;
    }

    private void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public String toString() {
        return "CEBLOCK_VectorCAN [messageId=" + messageId + ", canIndex=" + canIndex + ", messageName=" + messageName
                + ", senderName=" + senderName + "]";
    }

    /**
     * Reads a CEBLOCK DIM from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CEBLOCK_VectorCAN read(SeekableByteChannel sbc, long pos) throws IOException {
        CEBLOCK_VectorCAN ceBlockVector = new CEBLOCK_VectorCAN(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(80);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(pos);
        sbc.read(bb);
        bb.rewind();

        // UINT32 1 Identifier of CAN message
        ceBlockVector.setMessageId(Mdf3Util.readUInt32(bb));

        // UINT32 1 Index of CAN channel
        ceBlockVector.setCanIndex(Mdf3Util.readUInt32(bb));

        // CHAR 36 Name of message (string should be terminated by 0)
        ceBlockVector.setMessageName(Mdf3Util.readChars(bb, 36));

        // CHAR 36 Name of sender (string should be terminated by 0)
        ceBlockVector.setSenderName(Mdf3Util.readChars(bb, 36));

        return ceBlockVector;
    }

}
