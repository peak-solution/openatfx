package de.rechner.openatfx;

import java.io.IOException;
import java.io.InputStream;


/**
 * Interface defining an abstraction layer to the underlying file system.<br/>
 * This enables to implement custom file access methods.
 * 
 * @author Christian Rechner
 */
public interface IFileHandler {

    public String getFileRoot(String path) throws IOException;

    public String getFileName(String path) throws IOException;

    public InputStream getFileStream(String path) throws IOException;

}
