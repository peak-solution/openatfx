package de.rechner.openatfx;

import java.io.IOException;
import java.io.InputStream;


/**
 * Interface defining an abstraction layer to the underlying file system.<br>
 * This enables to implement custom file access methods.
 * 
 * @author Christian Rechner
 */
public interface IFileHandler {

    public String getFileRoot(String path) throws IOException;

    /**
     * Returns the file name without path information
     * 
     * @param path The full path to the file.
     * @return The extracted file name.
     * @throws IOException Error getting file name.
     */
    public String getFileName(String path) throws IOException;

    /**
     * Returns a stream for given full path.
     * 
     * @param path The full path to the file to read.
     * @return The stream.
     * @throws IOException Error opening file stream.
     */
    public InputStream getFileStream(String path) throws IOException;

}
