package de.rechner.openatfx_mdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.rechner.openatfx.IFileHandler;


/**
 * Implementation of the <code>de.rechner.openatfx.IFileHandler</code> interface for the local file system.
 * 
 * @author Christian Rechner
 */
class TmpFileHandler implements IFileHandler {

    private static final String ATFX_TEMPLATE = "model.atfx";

    
    
    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx.IFileHandler#getFileStream(java.lang.String)
     */
    @Override
    public InputStream getFileStream(String path) throws IOException {
        InputStream in = getClass().getResourceAsStream(ATFX_TEMPLATE);
        if (in == null) {
            throw new IOException("Unable to get template ATFX: " + ATFX_TEMPLATE);
        }
        return in;
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
        return file.getName();
    }

}
