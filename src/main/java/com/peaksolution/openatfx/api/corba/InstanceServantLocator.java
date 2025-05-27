package com.peaksolution.openatfx.api.corba;

import org.asam.ods.InstanceElementIteratorPOATie;
import org.asam.ods.MeasurementPOATie;
import org.asam.ods.ODSFilePOATie;
import org.asam.ods.SubMatrixPOATie;
import org.omg.CORBA.LocalObject;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

import com.peaksolution.openatfx.api.AtfxInstance;
import com.peaksolution.openatfx.api.OpenAtfxAPIImplementation;


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
    private final transient CorbaAtfxCache atfxCache;
    private final transient OpenAtfxAPIImplementation api;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param atfxCache The atfx cache.
     * @param api The OpenAtfxAPI.
     */
    public InstanceServantLocator(POA modelPOA, CorbaAtfxCache atfxCache, OpenAtfxAPIImplementation api) {
        this.modelPOA = modelPOA;
        this.atfxCache = atfxCache;
        this.api = api;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.omg.PortableServer.ServantLocatorOperations#preinvoke(byte[], org.omg.PortableServer.POA,
     *      java.lang.String, org.omg.PortableServer.ServantLocatorPackage.CookieHolder)
     */
    public Servant preinvoke(byte[] oid, POA adapter, String operation, CookieHolder theCookie) throws ForwardRequest {
        long[] ls = CorbaAtfxCache.toLongA(oid);
        long type = ls[0];
        long aid = ls[1];
        long iid = ls[2];

        // get instance from API only if not an instance iterator
        AtfxInstance instance = null;
        if (type != 3) {
            instance = api.getInstanceById(aid, iid);
        }
        
        // type=0, object is a <code>org.asam.ods.InstanceElement</code>
        if (type == 0) {
            return new InstanceElementImpl(modelPOA, adapter, atfxCache, instance);
        }
        // type=1, object is a <code>org.asam.ods.Measurement</code>
        else if (type == 1) {
            return new MeasurementPOATie(new MeasurementImpl(modelPOA, adapter, atfxCache, instance));
        }
        // type=2, object is a <code>org.asam.ods.SubMatrix</code>
        else if (type == 2) {
            return new SubMatrixPOATie(new SubMatrixImpl(modelPOA, adapter, atfxCache, instance, api.getContext().get("VALUEMATRIX_MODE")));
        }
        // type=3, object is a <code>org.asam.ods.InstanceElementIterator</code>
        else if (type == 3) {
            return new InstanceElementIteratorPOATie(new InstanceElementIteratorImpl(atfxCache, aid));
        }
        // type=4, object is a <code>org.asam.ods.ODSFile</code>
        else if (type == 4) {
            return new ODSFilePOATie(new ODSFileImpl(modelPOA, adapter, atfxCache, instance));
        }
        throw new ForwardRequest();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.omg.PortableServer.ServantLocatorOperations#postinvoke(byte[], org.omg.PortableServer.POA,
     *      java.lang.String, java.lang.Object, org.omg.PortableServer.Servant)
     */
    public void postinvoke(byte[] oid, POA adapter, String operation, java.lang.Object theCookie, Servant theServant) {}

}
