package de.rechner.openatfx;

import java.util.LinkedList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIteratorPOA;
import org.asam.ods.SeverityFlag;


/**
 * Implementation of <code>org.asam.ods.InstanceElementIterator</code>.
 * 
 * @author Christian Rechner
 */
class InstanceElementIteratorImpl extends InstanceElementIteratorPOA {

    private final AtfxCache atfxCache;
    private final long id;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param instanceElements The instance elements.
     */
    public InstanceElementIteratorImpl(final AtfxCache atfxCache, final long id) {
        this.atfxCache = atfxCache;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementIteratorOperations#getCount()
     */
    public int getCount() throws AoException {
        return getInstanceElements().length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementIteratorOperations#nextN(int)
     */
    public InstanceElement[] nextN(int how_many) throws AoException {
        InstanceElement[] instanceElements = getInstanceElements();
        int pointer = getPointer();

        // improve for all elements
        if (pointer == 0 && (how_many == instanceElements.length)) {
            return instanceElements;
        }

        List<InstanceElement> list = new LinkedList<InstanceElement>();
        for (int i = pointer; i < how_many; i++) {
            if (i >= instanceElements.length) {
                break;
            }
            list.add(instanceElements[i]);
            pointer++;
        }

        this.atfxCache.setIteratorPointer(id, pointer);
        return list.toArray(new InstanceElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementIteratorOperations#nextOne()
     */
    public InstanceElement nextOne() throws AoException {
        InstanceElement[] instanceElements = getInstanceElements();
        int pointer = getPointer();

        if (pointer >= instanceElements.length) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Iterator has reached the end");
        }
        InstanceElement ie = instanceElements[pointer];
        pointer++;
        this.atfxCache.setIteratorPointer(id, pointer);
        return ie;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementIteratorOperations#reset()
     */
    public void reset() throws AoException {
        this.atfxCache.setIteratorPointer(id, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.InstanceElementIteratorOperations#destroy()
     */
    public void destroy() throws AoException {
        this.atfxCache.removeInstanceIterator(this.id);
    }

    private InstanceElement[] getInstanceElements() throws AoException {
        InstanceElement[] instanceElements = this.atfxCache.getIteratorInstances(this.id);
        if (instanceElements == null) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Invalid InstanceElementIterator reference");
        }
        return instanceElements;
    }

    private int getPointer() throws AoException {
        Integer pointer = this.atfxCache.getIteratorPointer(this.id);
//        if (pointer == null) {
//            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
//                                  "Invalid InstanceElementIterator reference");
//        }
        return pointer;
    }

}
