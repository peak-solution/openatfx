package de.rechner.openatfx.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * RandomAccessFile with buffer.
 * 
 * @author Christian Rechner
 */
public class BufferedRandomAccessFile extends RandomAccessFile {

    boolean reading = true;
    private byte buffer[];
    private int bufferSize = 0;

    private long filePos = 0;
    private long fileLength = 0;
    private long bufferStart = 0;

    public BufferedRandomAccessFile(String filename, String mode, int bufsize) throws IOException {
        this(new File(filename), mode, bufsize);
    }

    public BufferedRandomAccessFile(File file, String mode, int bufsize) throws IOException {
        super(file, mode);
        fileLength = file.length();
        buffer = new byte[bufsize];
    }

    public final int read() throws IOException {
        if (!reading)
            switchToReadBuffer();
        while (true) {
            if (filePos == fileLength)
                return -1;
            // read the data
            int readAtIdx = (int) (filePos - bufferStart);
            if (readAtIdx < 0 || readAtIdx >= bufferSize)
                updateReadBuffer();
            else {
                ++filePos;
                return ((int) buffer[readAtIdx]) & 0xff;
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!reading)
            switchToReadBuffer();

        if (filePos == fileLength)
            return -1;
        int idx = off;
        int stop = off + len;
        while (idx < stop) {
            int readAtIdx = (int) (filePos - bufferStart);
            if (readAtIdx < 0 || readAtIdx >= bufferSize) {
                updateReadBuffer();
                continue;
            }
            int toread = stop - idx;
            int available = (int) (fileLength - filePos);
            if (toread > available)
                toread = available;
            int availableInBuffer = bufferSize - readAtIdx;
            if (toread > availableInBuffer)
                toread = availableInBuffer;
            System.arraycopy(buffer, readAtIdx, b, idx, toread);
            idx += toread;
            filePos += toread;
        }
        return idx - off;
    }

    @Override
    public void write(int b) throws IOException {
        if (reading)
            switchToWriteBuffer();
        while (true) {
            if (bufferSize == 0)
                bufferStart = filePos;
            int writeAtIdx = (int) (filePos - bufferStart);
            if (writeAtIdx < 0 || writeAtIdx >= buffer.length)
                flush();
            else {
                buffer[writeAtIdx] = (byte) b;
                if (writeAtIdx == bufferSize)
                    bufferSize++;
                if (++filePos > fileLength)
                    fileLength = filePos;
                return;
            }
        }
    };

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (reading)
            switchToWriteBuffer();
        int from = off;
        int remaining = len;
        while (remaining > 0) {
            if (bufferSize == 0)
                bufferStart = filePos;
            int writeAtIdx = (int) (filePos - bufferStart);
            if (writeAtIdx < 0 || writeAtIdx >= buffer.length)
                flush();
            else {
                int todo = buffer.length - writeAtIdx;
                if (todo > remaining)
                    todo = remaining;
                System.arraycopy(b, from, buffer, writeAtIdx, todo);
                writeAtIdx += todo;
                if (writeAtIdx > bufferSize)
                    bufferSize = writeAtIdx;
                filePos += todo;
                if (filePos > fileLength)
                    fileLength = filePos;
                remaining -= todo;
                from += todo;
            }
        }
    }

    private void switchToWriteBuffer() {
        bufferSize = 0;
        bufferStart = filePos;
        reading = false;
    }

    public void switchToReadBuffer() throws IOException {
        flush();
        reading = true;
    }

    private void updateReadBuffer() throws IOException {
        super.seek(filePos);
        bufferStart = filePos;
        int n = super.read(buffer, 0, buffer.length);
        if (n < 0)
            n = 0;
        bufferSize = n;
    }

    @Override
    public long getFilePointer() throws IOException {
        return filePos;
    }

    @Override
    public long length() throws IOException {
        return fileLength;
    }

    @Override
    public void seek(long pos) throws IOException {
        filePos = pos;
        if (filePos > fileLength)
            filePos = fileLength;
        if (filePos < 0)
            filePos = 0;
    }

    public void flush() throws IOException {
        if (reading)
            return;
        super.seek(bufferStart);
        super.write(buffer, 0, bufferSize);
        bufferSize = 0;
    }

    @Override
    public void setLength(long newLength) throws IOException {
        flush();
        super.setLength(newLength);
        fileLength = newLength;
        seek(filePos);
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }
}
