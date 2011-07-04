package de.rechner.openatfx.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;


/**
 * Wrapper of an instance of AoExternalComponent. Used for reading measurement data from external files.
 * 
 * @author Christian Rechner
 */
@SuppressWarnings("unused")
public class ExternalComponentImpl {

    private static final Log LOG = LogFactory.getLog(ExternalComponentImpl.class);

    private final InstanceElement ieExtComp;

    private int valueType;
    private long length;
    private long offset;

    private int blocksize;
    private int valuesperblock;
    private int valueOffset;

    private FileChannel channel;
    private MappedByteBuffer mappedByteBuffer;

    /**
     * Constructor.
     * 
     * @param ieExtComp The AoExternalComponent instance element.
     */
    public ExternalComponentImpl(InstanceElement ieExtComp) {
        this.ieExtComp = ieExtComp;
    }

    /**
     * Returns the component file containing the measurement data.
     * 
     * @return The file.
     * @throws AoException Error retrieving file name.
     */
    private File getFile() throws AoException {
        String filename = ieExtComp.getValueByBaseName("filename_url").value.u.stringVal();
        // TODO: build full pathname
        return new File(filename);
    }

    private File getBaseDir() throws AoException {
        return null;
    }
    
    /**
     * Returns the data values type enumeration value.
     * 
     * @return The enumeration value.
     * @throws AoException Error getting value type.
     */
    private int getValueType() throws AoException {
        return this.ieExtComp.getValueByBaseName("value_type").value.u.enumVal();
    }

    private long getStartOffset() throws AoException {
        return this.ieExtComp.getValueByBaseName("start_offset").value.u.enumVal();
    }

    private int getBlockSize() throws AoException {
        return this.ieExtComp.getValueByBaseName("block_size").value.u.enumVal();
    }

    private int getValuesPerBlock() throws AoException {
        return this.ieExtComp.getValueByBaseName("valuesperblock").value.u.enumVal();
    }

    private int getValueOffset() throws AoException {
        return this.ieExtComp.getValueByBaseName("value_offset").value.u.enumVal();
    }

    private File getFlagsFile() throws AoException {
        // return this.ieExtComp.getValueByBaseName("flags_filename_url").value.u.enumVal();
        return null;
    }

    private long getFlagsStartOffset() throws AoException {
        return this.ieExtComp.getValueByBaseName("flags_start_offset").value.u.enumVal();
    }

    private MappedByteBuffer getMappedByteBuffer() throws AoException {
        if (this.mappedByteBuffer == null) {
            try {
                File file = getFile();
                this.channel = new RandomAccessFile(file, "rw").getChannel();
                this.mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());
            } catch (FileNotFoundException e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, e.getMessage());
            }
        }
        return this.mappedByteBuffer;
    }

    public TS_Value getValue() throws AoException {
        // dt_boolean (0),
        // dt_byte (1),
        // dt_short (2),
        // dt_long (3),
        // dt_longlong (4),
        // ieeefloat8 (6),
        // dt_short_beo (7),
        // dt_long_beo (8),
        // dt_longlong_beo (9),
        // ieeefloat4_beo (10),
        // ieeefloat8_beo (11),
        // dt_string (12),
        // dt_bytestr (13),
        // dt_blob (14),
        // dt_boolean_flags_beo (15),
        // dt_byte_flags_beo (16),
        // dt_string_flags_beo (17),
        // dt_bytestr_beo (18),
        // dt_sbyte (19),
        // dt_sbyte_flags_beo (20),
        // dt_ushort (21),
        // dt_ushort_beo (22),
        // dt_ulong (23),
        // dt_ulong_beo (24)

        return null;
    }

    public TS_ValueSeq getValueSeq(int start, int length) throws AoException {
        return null;
    }

    /**
     * Closes the external component channel.
     * 
     * @throws AoException Error closing channel.
     */
    public void close() throws AoException {
        if (this.channel != null) {
            try {
                this.channel.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                this.channel = null;
                this.mappedByteBuffer = null;
            }
        }
    }

}
