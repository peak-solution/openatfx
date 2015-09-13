package de.rechner.openatfx;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Implementation of the <code>de.rechner.openatfx.IFileHandler</code> interface for the local file system.
 * 
 * @author Christian Rechner
 */
public class LocalFileHandler implements IFileHandler {

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx.IFileHandler#getFileStream(java.lang.String)
     */
    @Override
    public InputStream getFileStream(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("File '" + path + "' not found!");
        }
        if (!file.canRead()) {
            throw new IOException("Unable to open file: " + path);
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx.IFileHandler#getFileRoot(java.lang.String)
     */
    @Override
    public String getFileRoot(String path) throws IOException {
        File file = new File(path);
        return file.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx.IFileHandler#getFileName(java.lang.String)
     */
    @Override
    public String getFileName(String path) throws IOException {
        File file = new File(path);
        return file.getAbsolutePath().replaceAll("\\\\", "/");
    }

}
