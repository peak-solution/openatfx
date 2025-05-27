package com.peaksolution.openatfx;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Implementation of the <code>com.peaksolution.openatfx.IFileHandler</code> interface for the local file system.
 * 
 * @author Christian Rechner
 */
public class LocalFileHandler implements IFileHandler {

    /**
     * {@inheritDoc}
     * 
     * @see com.peaksolution.openatfx.IFileHandler#getFileStream(java.nio.file.Path)
     */
    @Override
    public InputStream getFileStream(Path path) throws IOException {
        File file = path.toFile();
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
     * @see com.peaksolution.openatfx.IFileHandler#getFileRoot(java.nio.file.Path)
     */
    @Override
    public String getFileRoot(Path path) throws IOException {
        return path.toAbsolutePath().getParent().toString().replace("\\\\", "/");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.peaksolution.openatfx.IFileHandler#getFileName(java.nio.file.Path)
     */
    @Override
    public String getFileName(Path path) throws IOException {
        return path.getFileName().toString();
    }

}
