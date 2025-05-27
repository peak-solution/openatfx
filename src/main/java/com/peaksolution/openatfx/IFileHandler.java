package com.peaksolution.openatfx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Interface defining an abstraction layer to the underlying file system.<br>
 * This enables to implement custom file access methods.
 * 
 * @author Christian Rechner, Markus Renner
 */
public interface IFileHandler {

    public String getFileRoot(Path path) throws IOException;

    /**
     * Returns the file name without path information
     * 
     * @param path The full path to the file.
     * @return The extracted file name.
     * @throws IOException Error getting file name.
     */
    public String getFileName(Path path) throws IOException;

    /**
     * Returns a stream for given full path.
     * 
     * @param path The full path to the file to read.
     * @return The stream.
     * @throws IOException Error opening file stream.
     */
    public InputStream getFileStream(Path path) throws IOException;

}
