package com.peaksolution.openatfx.api.corba;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValueUnit;
import org.asam.ods.ODSFileOperations;
import org.asam.ods.ODSReadTransfer;
import org.asam.ods.ODSWriteTransfer;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.api.AtfxInstance;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ODSFile</code>.
 * 
 * @author Markus Renner
 */
class ODSFileImpl extends InstanceElementImpl implements ODSFileOperations {
    private static AtomicInteger instanceNameCounter = new AtomicInteger(1);
    
    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param delegate The delegate instance.
     */
    public ODSFileImpl(POA modelPOA, POA instancePOA, CorbaAtfxCache atfxCache, AtfxInstance delegate) {
        super(modelPOA, instancePOA, atfxCache, delegate);
    }

    private Path getFilePath() throws AoException {
        NameValueUnit nvu = getValueByBaseName("ao_location");
        String stringVal = nvu.value.u.stringVal();
        if (0 == nvu.value.flag || stringVal == null || stringVal.isEmpty()) {
            return null;
        }
        
        IFileHandler fileHandler = corbaCache.getFileHandler();
        if (fileHandler instanceof LocalFileHandler) {
            try {
                return Paths.get(fileHandler.getFileRoot(Paths.get(stringVal)), stringVal);
            } catch (IOException e) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "Error getting file path for file string " + stringVal);
            }
        }

        return Paths.get(stringVal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#create()
     */
    public ODSWriteTransfer create() throws AoException {
        String filename = null;
        NameValueUnit nvu = null;
        try {
            nvu = getValueByBaseName("ao_original_filename");
        } catch (AoException aoe) {
            // tolerate not existing attribute
        }
        if (null != nvu && null != nvu.value && nvu.value.flag != 0 && null != nvu.value.u) {
            filename = nvu.value.u.stringVal();
        }
        if (filename == null || filename.isEmpty())
        {
            nvu = getValueByBaseName("ao_location");
            if (null != nvu && null != nvu.value && nvu.value.flag != 0 && null != nvu.value.u) {
                String location = nvu.value.u.stringVal();
                filename = Paths.get(location).getFileName().toString();
            }
        }
        if (filename == null || filename.isEmpty())
        {
            filename = "mea" + instanceNameCounter.getAndIncrement() + ".btf";
        }

        ApplicationAttribute locationAttr = getApplicationElement().getAttributeByBaseName("ao_location");
        setValue(ODSHelper.createStringNVU(locationAttr.getName(), filename));
        try {
            ApplicationAttribute sizeAttr = getApplicationElement().getAttributeByBaseName("ao_size");
            setValue(ODSHelper.createLongLongNVU(sizeAttr.getName(), 0L));
        } catch (AoException ex) {
            if (ErrorCode.AO_NOT_FOUND == ex.errCode) {
                // ignore missing optional size attribute
            } else
                throw ex;
        }
        
        return corbaCache.newWriteTransfer(modelPOA, instancePOA, aid, iid, filename, this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#exists()
     */
    public boolean exists() throws AoException {
        Path path = getFilePath();
        return (null != path && Files.exists(path));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#canRead()
     */
    public boolean canRead() throws AoException {
        if (exists()) {
            Path path = getFilePath();
            return path.toFile().canRead();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#canWrite()
     */
    public boolean canWrite() throws AoException {
        if (exists()) {
            Path path = getFilePath();
            return path.toFile().canWrite();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#getDate()
     */
    public String getDate() throws AoException {
        if (exists()) {
            Path path = getFilePath();
            try {
                FileTime creationTime = (FileTime) Files.getAttribute(path, "creationTime");
                return ODSHelper.asODSDate(new Date(creationTime.toMillis()));
            } catch (IOException ex) {
                throw new AoException(ErrorCode.AO_BAD_OPERATION, SeverityFlag.ERROR, 0, "Could not get creation time from AoFile (file=" + getFilePath().toString() + ")");
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#getSize()
     */
    public T_LONGLONG getSize() throws AoException {
        if (exists()) {
            Path path = getFilePath();
            long size = path.toFile().length();
            return ODSHelper.asODSLongLong(size);
        }
        return new T_LONGLONG(0, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#remove()
     */
    public void remove() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#read()
     */
    public ODSReadTransfer read() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#takeUnderControl(java.lang.String)
     */
    public void takeUnderControl(String sourceUrl) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#removeFromControl(java.lang.String)
     */
    public void removeFromControl(String targetUrl) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ODSFileOperations#append()
     */
    public ODSWriteTransfer append() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Not implemented");
    }

}
