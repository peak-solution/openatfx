package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.Blob;
import org.asam.ods.BlobPOA;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


/**
 * Implementation of <code>org.asam.ods.NameIterator</code>.
 * 
 * @author Christian Rechner
 */
class BlobImpl extends BlobPOA {

    private static final Log LOG = LogFactory.getLog(NameIteratorImpl.class);

    private final POA poa;
    private final List<Byte> content;
    private String header;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     */
    public BlobImpl(POA poa) {
        this.poa = poa;
        this.header = "";
        this.content = new ArrayList<Byte>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#getHeader()
     */
    public String getHeader() throws AoException {
        return this.header;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#setHeader(java.lang.String)
     */
    public void setHeader(String header) throws AoException {
        this.header = header;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#getLength()
     */
    public int getLength() throws AoException {
        return this.content.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#set(byte[])
     */
    public void set(byte[] value) throws AoException {
        this.content.clear();
        append(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#append(byte[])
     */
    public void append(byte[] value) throws AoException {
        for (byte b : value) {
            this.content.add(b);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#get(int, int)
     */
    public byte[] get(int offset, int length) throws AoException {
        if ((offset + length) > getLength()) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "The index must be in the range from 0 to max.");
        }
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = this.content.get(offset + i);
        }
        return b;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#compare(org.asam.ods.Blob)
     */
    public boolean compare(Blob aBlob) throws AoException {
        return Arrays.equals(get(0, getLength()), aBlob.get(0, aBlob.getLength()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BlobOperations#destroy()
     */
    public void destroy() throws AoException {
        try {
            byte[] id = poa.servant_to_id(this);
            poa.deactivate_object(id);
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ObjectNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

}
