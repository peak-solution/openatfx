package de.rechner.openatfx;

import org.asam.ods.InstanceElementIteratorPOATie;
import org.asam.ods.MeasurementPOATie;
import org.asam.ods.SubMatrixPOATie;
import org.omg.CORBA.LocalObject;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;


/**
 * A servant manager for the instance elements implementing the flighweight pattern. The application element and
 * instance ids are coded into the CORBA object id. On an CORBA request this information is parsed out and used. For
 * every request a new servant is created.
 * 
 * @author Christian Rechner
 */
class InstanceServantLocator extends LocalObject implements ServantLocator {

    private static final long serialVersionUID = 7856903379174182704L;

    private final POA modelPOA;
    private transient final AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param atfxCache The atfx cache.
     */
    public InstanceServantLocator(POA modelPOA, AtfxCache atfxCache) {
        this.modelPOA = modelPOA;
        this.atfxCache = atfxCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.omg.PortableServer.ServantLocatorOperations#preinvoke(byte[], org.omg.PortableServer.POA,
     *      java.lang.String, org.omg.PortableServer.ServantLocatorPackage.CookieHolder)
     */
    public Servant preinvoke(byte[] oid, POA adapter, String operation, CookieHolder the_cookie) throws ForwardRequest {
        long[] ls = AtfxCache.toLongA(oid);
        long type = ls[0];
        long aid = ls[1];
        long iid = ls[2];

        // type=0, object is a <code>org.asam.ods.InstanceElement</code>
        if (type == 0) {
            return new InstanceElementImpl(modelPOA, adapter, atfxCache, aid, iid);
        }
        // type=1, object is a <code>org.asam.ods.Measurement</code>
        else if (type == 1) {
            return new MeasurementPOATie(new MeasurementImpl(modelPOA, adapter, atfxCache, aid, iid));
        }
        // type=2, object is a <code>org.asam.ods.SubMatrix</code>
        else if (type == 2) {
            return new SubMatrixPOATie(new SubMatrixImpl(modelPOA, adapter, atfxCache, aid, iid));
        }
        // type=3, object is a <code>org.asam.ods.InstanceElementIterator</code>
        else if (type == 3) {
            return new InstanceElementIteratorPOATie(new InstanceElementIteratorImpl(atfxCache, aid));
        }
        throw new ForwardRequest();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.omg.PortableServer.ServantLocatorOperations#postinvoke(byte[], org.omg.PortableServer.POA,
     *      java.lang.String, java.lang.Object, org.omg.PortableServer.Servant)
     */
    public void postinvoke(byte[] oid, POA adapter, String operation, java.lang.Object the_cookie, Servant the_servant) {}

}
