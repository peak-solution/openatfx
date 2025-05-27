package com.peaksolution.openatfx.api.corba;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueUnit;
import org.asam.ods.ODSWriteTransferPOA;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.util.ODSHelper;


/**
 * The class which allows a write transfer (from a client to the openatfx library) of the content of a file.
 */
public class OdsWriteTransfer extends ODSWriteTransferPOA {

    private static final String AO_HASH_ALGORITHM = "ao_hash_algorithm";
    private static final String AO_HASH_VALUE = "ao_hash_value";
    private static final String AO_SIZE = "ao_size";
    private static final int MEMORY_MAPPING_THRESHOLD = 64000;
    private static final int FILE_WRITE_BUFFER_SIZE = 1024 * 1024;
    private static final String FILE_HASH_ALGORITHM = "SHA-256";

    protected static enum State {
        OPEN, CLOSED, CLOSE_FAILED
    }

    private final Path filePath;
    private final InstanceElementImpl instElement;

    private State state = State.OPEN;
    private boolean fileModified = false;

    public OdsWriteTransfer(String filePath, InstanceElementImpl instElement) {
        this.filePath = Paths.get(filePath);
        this.instElement = instElement;
    }

    /**
     * Close this OdsWriteTransfer instance. This method closes the file on the file system; thereby any data to be
     * written to the file and still residing in cache will be flushed to the file. The method finally destroys this
     * OdsWriteTransfer object.
     * 
     * @throws AoException
     */
    public void close() throws AoException {
        if (state == State.OPEN) {
            try {
                if (fileModified) {
                    List<NameValueUnit> attributeValues = new ArrayList<>();

                    String sizeAttrName = getAttrNameFromInstance(instElement, AO_SIZE);
                    if (sizeAttrName != null) {
                        attributeValues.add(ODSHelper.createLongLongNVU(sizeAttrName, Long.valueOf(getDataSize())));
                    }

                    String hashValAttrName = getAttrNameFromInstance(instElement, AO_HASH_VALUE);
                    String hashAlgAttrName = getAttrNameFromInstance(instElement, AO_HASH_ALGORITHM);
                    if (hashValAttrName != null && hashAlgAttrName != null) {
                        String hash = getChecksum();
                        attributeValues.add(ODSHelper.createStringNVU(hashAlgAttrName, FILE_HASH_ALGORITHM));
                        attributeValues.add(ODSHelper.createStringNVU(hashValAttrName, hash));
                    }

                    if (!attributeValues.isEmpty()) {
                        instElement.setValueSeq(attributeValues.stream().filter(Objects::nonNull)
                                                               .toArray(NameValueUnit[]::new));
                    }
                }

                state = State.CLOSED;
            } catch (AoException exc) {
                state = State.CLOSE_FAILED;
                throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
            } finally {
                closeByteChannel(filePath);
            }
        }
    }

    private void closeByteChannel(final Path path) throws AoException {
        try {
            SeekableByteChannel byteChannel = getByteChannel();
            synchronized (byteChannel) {
                if (byteChannel.isOpen()) {
                    byteChannel.close();
                }
            }
        } catch (IOException exc) {

        }
    }

    private String getChecksum() throws AoException {
        SeekableByteChannel byteChannel = getByteChannel();
        long oldPos = 0;
        try {
            oldPos = byteChannel.position();
            byteChannel.position(0);
            MessageDigest md = MessageDigest.getInstance(FILE_HASH_ALGORITHM);

            ByteBuffer buf = ByteBuffer.allocate(1024);
            int bytesRead = 0;
            while ((bytesRead = byteChannel.read(buf)) != -1) {
                md.update(buf.array(), 0, bytesRead);
                buf.rewind();
            }

            return byteToString(md.digest());
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        } catch (NoSuchAlgorithmException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Unknown algorithm: " + exc.getMessage());
        } finally {
            try {
                byteChannel.position(oldPos);
            } catch (IOException exc) {
                throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0,
                                      "Error resetting file channel position: " + exc.getMessage());
            }
        }
    }

    private String byteToString(byte[] mdbytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xFF) + 0x100, 16).substring(1));
        }
        return sb.toString().toLowerCase();
    }

    private String getAttrNameFromInstance(InstanceElementImpl inst, String baseAttrName) throws AoException {
        try {
            ApplicationAttribute attr = inst.getApplicationElement().getAttributeByBaseName(baseAttrName);
            return attr.getName();
        } catch (AoException ex) {
            if (ErrorCode.AO_NOT_FOUND == ex.errCode) {
                // ignore missing optional attribute
            } else
                throw ex;
        }
        return null;
    }

    /**
     * Write a sequence of bytes to the file. This method writes a sequence of bytes to the file represented by this
     * OdsWriteTransfer instance. The parameter 'buffer' contains the byte sequence that is to be written to the file.
     * The contents of 'buffer' will be appended at the end of the file (which may also be the very start of the file in
     * case the file was newly created), and subsequent method invocations will add their bytes at the then actual end
     * of the file. Note that only sequential writing is supported by this interface. If previous parts of the file need
     * to be changed, the file must be removed and a new one must be created. In case the current location of the file
     * runs out of memory or any other operating system problem occurs the code will return the exception
     * AO_SYSTEM_PROBLEM. In this case the results of the actual putOctetSeq() invocation will be undone (no byte of
     * 'buffer' is added to the file) and the file may be closed without containing 'buffer' at its end.
     *
     * @param buffer The buffer containing the sequence of bytes that shall be written to the file.
     * @throws AoException
     */
    public void putOctectSeq(byte[] buffer) throws AoException {
        putOctectSeq(buffer, -1L);
    }

    public void putOctectSeq(byte[] buffer, long position) throws AoException {
        final byte[] finalBuffer = buffer;
        checkClosed();
        if (position < 0L) {
            append(finalBuffer);
        } else {
            put(finalBuffer, position);
        }

        fileModified = true;
    }

    /**
     * Puts data to this {@link OdsWriteTransfer}'s {@link SeekableByteChannel}.
     * 
     * @param data Array of bytes to put
     * @param position The position to write the data at
     * @return The number of bytes written
     * @throws AoException
     */
    public int put(byte[] data, long position) throws AoException {
        int dataLength = data.length;
        SeekableByteChannel byteChannel = getByteChannel();
        MappedByteBuffer mappedByteBuffer = null;

        try {
            if (!(byteChannel instanceof FileChannel) || MEMORY_MAPPING_THRESHOLD < 0
                    || dataLength < MEMORY_MAPPING_THRESHOLD) {
                byteChannel.position(position);
                int bytesWrittenTotal = 0;
                ByteBuffer buffer = ByteBuffer.wrap(data);
                while (dataLength > (bytesWrittenTotal += byteChannel.write(buffer)))
                    ;
                return bytesWrittenTotal;
            } else {
                mappedByteBuffer = ((FileChannel) byteChannel).map(MapMode.READ_WRITE, position, dataLength);
                mappedByteBuffer.put(data);
                return data.length;
            }
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        } finally {
            if (null != mappedByteBuffer) {
                closeDirectBuffer(mappedByteBuffer);
                mappedByteBuffer = null;
            }
        }
    }

    protected static void closeDirectBuffer(ByteBuffer buffer) {
        // Taken from
        // http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java

        if (null != buffer && buffer.isDirect()) {
            // we could use this type cast and call functions without reflection code,
            // but static import from sun.* package is risky for non-SUN virtual machine.
            // try { ((sun.nio.ch.DirectBuffer) pBuffer).cleaner().clean(); } catch (Exception exc) { }
            try {
                Method cleaner = buffer.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleaner.invoke(buffer));
            } catch (Exception exc) {
            }

            buffer = null;
        }
    }

    public int append(byte[] data) throws AoException {
        try {
            return put(data, getByteChannel().size());
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
    }

    private SeekableByteChannel getByteChannel() throws AoException {
        StandardOpenOption[] options = new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.READ,
                StandardOpenOption.WRITE };
        try {
            return Files.newByteChannel(filePath, options);
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
    }

    public void writeStream(InputStream in) throws AoException {
        writeStream(in, -1L);
    }

    public long writeStream(InputStream in, long position) throws AoException {
        checkClosed();

        try {
            long bytesWritten = 0L;
            if (position < 0L) {
                bytesWritten = put(in, getByteChannel().size());
            } else {
                bytesWritten = put(in, position);
            }

            fileModified = true;

            return bytesWritten;
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
    }

    private long put(InputStream inputStream, long position) throws AoException {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];
        int bytesRead = -1;
        long positionStart = position;
        try {
            while ((bytesRead = inputStream.read(buffer)) > -1) {
                if (bytesRead > 0) {
                    position += put(bytesRead < FILE_WRITE_BUFFER_SIZE ? ArrayUtils.subarray(buffer, 0, bytesRead)
                            : buffer, position);
                }
            }
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }

        return (position - positionStart);
    }

    /**
     * Retrieve the current position of the write pointer in the file. This method returns the current byte position of
     * the write pointer. The start of the file corresponds to a position of 0.
     *
     * @return The current position of the write pointer in the file.
     * @throws AoException
     */
    public T_LONGLONG getPosition() throws AoException {
        checkClosed();

        try {
            return ODSHelper.asODSLongLong(getByteChannel().position());
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
    }

    public void truncate(long size) throws AoException {
        checkClosed();
        try {
            getByteChannel().truncate(size);
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
        fileModified = true;
    }

    public long getDataSize() throws AoException {
        checkClosed();

        try {
            return getByteChannel().size();
        } catch (IOException exc) {
            throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0, exc.getMessage());
        }
    }

    private void checkClosed() throws AoException {
        switch (state) {
            case CLOSED:
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0,
                                      "OdsWriteTransfer instance has already been closed!");
            case CLOSE_FAILED:
                throw new AoException(ErrorCode.AO_SYSTEM_PROBLEM, SeverityFlag.ERROR, 0,
                                      "Previous close operation on OdsWriteTransfer instance failed, instance can thus no longer be used!");
            default:
            break;
        }
    }
}
