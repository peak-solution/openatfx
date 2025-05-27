package com.peaksolution.openatfx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.AoFactoryPOA;
import org.asam.ods.AoSession;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.api.corba.CorbaAtfxReader;


/**
 * Implementation of <code>org.asam.ods.AoFactory</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
class AoFactoryImpl extends AoFactoryPOA {

    private final ORB orb;
    private final IFileHandler fileHandler;

    /**
     * Creates a new AoFactory object.
     * 
     * @param orb The ORB.
     * @param fileHandler The file handler.
     */
    public AoFactoryImpl(ORB orb, IFileHandler fileHandler) {
        this.orb = orb;
        this.fileHandler = fileHandler;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getInterfaceVersion()
     */
    public String getInterfaceVersion() throws AoException {
        return OpenAtfxConstants.DEF_ODS_API_VERSION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getType()
     */
    public String getType() throws AoException {
        return OpenAtfxConstants.DEF_TYPE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getName()
     */
    public String getName() throws AoException {
        return OpenAtfxConstants.DEF_TYPE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#getDescription()
     */
    public String getDescription() throws AoException {
        return "ATFX file driver for ASAM OO-API";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#newSession(java.lang.String)
     */
    public AoSession newSession(String auth) throws AoException {
        List<NameValue> list = new ArrayList<>();
        for (String str : auth.split(",")) {
            String[] parts = str.split("=");
            if (parts.length == 2) {
                NameValue nv = new NameValue();
                nv.valName = parts[0];
                nv.value = new TS_Value();
                nv.value.flag = (short) 15;
                nv.value.u = new TS_Union();
                nv.value.u.stringVal(parts[1]);
                list.add(nv);
            }
        }
        return newSessionNameValue(list.toArray(new NameValue[0]));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.AoFactoryOperations#newSessionNameValue(org.asam.ods.NameValue[])
     */
    public AoSession newSessionNameValue(NameValue[] auth) throws AoException {
        File atfxFile = null;
        for (NameValue nv : auth) {
            if (OpenAtfxConstants.CONTEXT_FILENAME.equalsIgnoreCase(nv.valName)) {
                atfxFile = new File(nv.value.u.stringVal());
            }
        }
        if (atfxFile == null) {
            throw new AoException(ErrorCode.AO_MISSING_VALUE, SeverityFlag.ERROR, 0,
                                  "Parameter '" + OpenAtfxConstants.CONTEXT_FILENAME + "' not found");
        }
        CorbaAtfxReader reader = new CorbaAtfxReader();
        reader.init(orb, fileHandler, atfxFile.toPath());
        return reader.getSession();
    }
}
