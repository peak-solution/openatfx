package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameIteratorPOA;
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
class NameIteratorImpl extends NameIteratorPOA {

    private static final Log LOG = LogFactory.getLog(NameIteratorImpl.class);

    private final POA poa;
    private final String[] names;
    private int pointer;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param names The names.
     */
    public NameIteratorImpl(POA poa, String[] names) {
        this.poa = poa;
        this.names = names;
        this.pointer = 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameIteratorOperations#getCount()
     */
    public int getCount() throws AoException {
        return names.length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameIteratorOperations#nextN(int)
     */
    public String[] nextN(int how_many) throws AoException {
        List<String> list = new ArrayList<String>();
        for (int i = pointer; i < how_many; i++) {
            if (i >= this.names.length) {
                break;
            }
            list.add(this.names[i]);
            this.pointer++;
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameIteratorOperations#nextOne()
     */
    public String nextOne() throws AoException {
        if (pointer >= this.names.length) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "Iterator is at the end");
        }
        String str = this.names[this.pointer];
        pointer++;
        return str;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameIteratorOperations#reset()
     */
    public void reset() throws AoException {
        this.pointer = 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameIteratorOperations#destroy()
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
