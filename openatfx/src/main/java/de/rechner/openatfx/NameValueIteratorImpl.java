package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.NameValueIteratorPOA;
import org.asam.ods.SeverityFlag;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;


/**
 * Implementation of <code>org.asam.ods.NameValueIterator</code>.
 * 
 * @author Christian Rechner
 */
class NameValueIteratorImpl extends NameValueIteratorPOA {

    private static final Log LOG = LogFactory.getLog(NameIteratorImpl.class);

    private final POA poa;
    private final NameValue[] nameValues;
    private int pointer;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param nameValues The values.
     */
    public NameValueIteratorImpl(POA poa, NameValue[] nameValues) {
        this.poa = poa;
        this.nameValues = nameValues;
        this.pointer = 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameValueIteratorOperations#getCount()
     */
    public int getCount() throws AoException {
        return this.nameValues.length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameValueIteratorOperations#nextN(int)
     */
    public NameValue[] nextN(int how_many) throws AoException {
        List<NameValue> list = new ArrayList<NameValue>();
        for (int i = pointer; i < how_many; i++) {
            if (i >= this.nameValues.length) {
                break;
            }
            list.add(this.nameValues[i]);
            this.pointer++;
        }
        return list.toArray(new NameValue[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameValueIteratorOperations#nextOne()
     */
    public NameValue nextOne() throws AoException {
        if (pointer >= this.nameValues.length) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0, "Iterator is at the end");
        }
        NameValue nv = this.nameValues[this.pointer];
        pointer++;
        return nv;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameValueIteratorOperations#reset()
     */
    public void reset() throws AoException {
        this.pointer = 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.NameValueIteratorOperations#destroy()
     */
    public void destroy() throws AoException {
        try {
            byte[] id = poa.reference_to_id(_this_object());
            poa.deactivate_object(id);
        } catch (WrongAdapter e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ObjectNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

}
