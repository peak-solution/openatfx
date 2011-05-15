package de.rechner.openatfx;

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
        String oidStr = new String(oid);

        // parse aid and iid from object id
        String[] splitted = oidStr.split(":");
        long aid = Long.valueOf(splitted[0]);
        long iid = Long.valueOf(splitted[1]);

        return new InstanceElementImpl(modelPOA, adapter, atfxCache, aid, iid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.omg.PortableServer.ServantLocatorOperations#postinvoke(byte[], org.omg.PortableServer.POA,
     *      java.lang.String, java.lang.Object, org.omg.PortableServer.Servant)
     */
    public void postinvoke(byte[] oid, POA adapter, String operation, java.lang.Object the_cookie, Servant the_servant) {}

}
