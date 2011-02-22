package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationAttributeHelper;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationElementPOA;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationRelationInstanceElementSeq;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.InitialRight;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.InstanceElementIteratorHelper;
import org.asam.ods.NameIterator;
import org.asam.ods.NameIteratorHelper;
import org.asam.ods.NameValueSeqUnit;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.util.ODSHelper;
import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.ApplicationElement</code>.
 * 
 * @author Christian Rechner
 */
class ApplicationElementImpl extends ApplicationElementPOA {

    private static final Log LOG = LogFactory.getLog(ApplicationElementImpl.class);

    private final POA poa;
    private final AtfxCache atfxCache;
    private final ApplicationStructure applicationStructure;
    private final BaseElement baseElement;
    private final long aid;

    /**
     * Constructor.
     * 
     * @param poa The POA.
     * @param atfxCache The ATFX cache.
     * @param applicationStructure The application structure.
     * @param baseElement The base element.
     * @param aid The application element id;
     */
    public ApplicationElementImpl(POA poa, AtfxCache atfxCache, ApplicationStructure applicationStructure,
            BaseElement baseElement, long aid) {
        this.poa = poa;
        this.atfxCache = atfxCache;
        this.applicationStructure = applicationStructure;
        this.baseElement = baseElement;
        this.aid = aid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getApplicationStructure()
     */
    public ApplicationStructure getApplicationStructure() throws AoException {
        return this.applicationStructure;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getBaseElement()
     */
    public BaseElement getBaseElement() throws AoException {
        return this.baseElement;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setBaseElement(org.asam.ods.BaseElement)
     */
    public void setBaseElement(BaseElement baseElem) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setBaseElement' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getId()
     */
    public T_LONGLONG getId() throws AoException {
        return ODSHelper.asODSLongLong(this.aid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getName()
     */
    public String getName() throws AoException {
        return this.atfxCache.getApplicationElementNameById(this.aid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setName(java.lang.String)
     */
    public void setName(String aeName) throws AoException {
        // check name length
        if (aeName == null || aeName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "aeName must not be empty");
        }
        if (aeName.length() > 30) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "Application element name must not be greater than 30 characters");
        }
        // check for name equality
        if (getName().equals(aeName)) {
            return;
        }
        // check for existing application element
        ApplicationElement existingAe = this.atfxCache.getApplicationElementByName(aeName);
        if (existingAe != null) {
            throw new AoException(ErrorCode.AO_DUPLICATE_NAME, SeverityFlag.ERROR, 0, "ApplicationElement with name '"
                    + aeName + "' already exists");
        }
        this.atfxCache.renameApplicationElement(this.aid, aeName);
    }

    /***************************************************************************************
     * methods for application attributes
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#createAttribute()
     */
    public ApplicationAttribute createAttribute() throws AoException {
        // check if attribute already exists
        if (this.atfxCache.getApplicationAttributeByName(aid, "") != null) {
            throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                  "Creating a new application attribute with already having other attributes with empty name is not allowed!");
        }
        // create application attribute
        try {
            ApplicationAttributeImpl aaImpl = new ApplicationAttributeImpl(this.poa, this.atfxCache, this.aid);
            this.poa.activate_object(aaImpl);
            ApplicationAttribute aa = ApplicationAttributeHelper.narrow(poa.servant_to_reference(aaImpl));
            this.atfxCache.addApplicationAttribute(this.aid, aa);
            return aa;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#listAttributes(java.lang.String)
     */
    public String[] listAttributes(String aaPattern) throws AoException {
        List<String> list = new ArrayList<String>();
        for (String aaName : this.atfxCache.listApplicationAttributes(aid)) {
            if (PatternUtil.nameFilterMatch(aaName, aaPattern)) {
                list.add(aaName);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getAttributes(java.lang.String)
     */
    public ApplicationAttribute[] getAttributes(String aaPattern) throws AoException {
        List<ApplicationAttribute> list = new ArrayList<ApplicationAttribute>();
        for (ApplicationAttribute aa : this.atfxCache.getApplicationAttributes(aid)) {
            if (PatternUtil.nameFilterMatch(aa.getName(), aaPattern)) {
                list.add(aa);
            }
        }
        return list.toArray(new ApplicationAttribute[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getAttributeByName(java.lang.String)
     */
    public ApplicationAttribute getAttributeByName(String aaName) throws AoException {
        // check aaName length
        if (aaName == null || aaName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "aaName must not be empty");
        }
        // lookup
        ApplicationAttribute aa = this.atfxCache.getApplicationAttributeByName(aid, aaName);
        if (aa == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '" + aaName
                    + "' not found");
        }
        return aa;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getAttributeByBaseName(java.lang.String)
     */
    public ApplicationAttribute getAttributeByBaseName(String baName) throws AoException {
        // check baName length
        if (baName == null || baName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "baName must not be empty");
        }
        // lookup
        ApplicationAttribute aa = this.atfxCache.getApplicationAttributeByBaName(aid, baName);
        if (aa == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "ApplicationAttribute by base attribute name '" + baName
                                          + "' not found for ApplicationElement '" + getName() + "'");
        }
        return aa;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#removeAttribute(org.asam.ods.ApplicationAttribute)
     */
    public void removeAttribute(ApplicationAttribute applAttr) throws AoException {
        // check applAttr
        if (applAttr == null) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "applAttr must not be empty");
        }
        // check for non obligatory base attribute
        if (applAttr.getBaseAttribute() != null && applAttr.getBaseAttribute().isObligatory()) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "Removing application attribute derived from non obligatory base attribute not allowed");
        }
        // perform remove from cache
        this.atfxCache.removeApplicationAttribute(this.aid, applAttr.getName());
        // deactivate CORBA object
        try {
            byte[] id = poa.reference_to_id(applAttr);
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

    /***************************************************************************************
     * methods for application relations
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getAllRelations()
     */
    public ApplicationRelation[] getAllRelations() throws AoException {
        return this.atfxCache.getApplicationRelations(this.aid).toArray(new ApplicationRelation[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#listAllRelatedElements()
     */
    public String[] listAllRelatedElements() throws AoException {
        List<String> list = new ArrayList<String>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(this.aid)) {
            list.add(rel.getElem2().getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getAllRelatedElements()
     */
    public ApplicationElement[] getAllRelatedElements() throws AoException {
        List<ApplicationElement> list = new ArrayList<ApplicationElement>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(this.aid)) {
            list.add(rel.getElem2());
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getRelationsByBaseName(java.lang.String)
     */
    public ApplicationRelation[] getRelationsByBaseName(String baseRelName) throws AoException {
        List<ApplicationRelation> list = new ArrayList<ApplicationRelation>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(this.aid)) {
            BaseRelation baseRel = rel.getBaseRelation();
            if ((baseRel != null) && baseRel.getRelationName().equals(baseRelName)) {
                list.add(rel);
            }
        }
        return list.toArray(new ApplicationRelation[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getRelationsByType(org.asam.ods.RelationType)
     */
    public ApplicationRelation[] getRelationsByType(RelationType aeRelationType) throws AoException {
        List<ApplicationRelation> list = new ArrayList<ApplicationRelation>();
        for (ApplicationRelation rel : this.atfxCache.getApplicationRelations(this.aid)) {
            if (rel.getRelationType() == aeRelationType) {
                list.add(rel);
            }
        }
        return list.toArray(new ApplicationRelation[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#listRelatedElementsByRelationship(org.asam.ods.Relationship)
     */
    public String[] listRelatedElementsByRelationship(Relationship aeRelationship) throws AoException {
        List<String> list = new ArrayList<String>();
        for (ApplicationElement ae : getRelatedElementsByRelationship(aeRelationship)) {
            list.add(ae.getName());
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getRelatedElementsByRelationship(org.asam.ods.Relationship)
     */
    public ApplicationElement[] getRelatedElementsByRelationship(Relationship aeRelationship) throws AoException {
        List<ApplicationElement> list = new ArrayList<ApplicationElement>();
        for (ApplicationRelation applRel : this.atfxCache.getApplicationRelations(this.aid)) {
            if (aeRelationship == Relationship.ALL_REL) {
                list.add(applRel.getElem2());
                // } else if (aeRelationship == Relationship.INFO_REL) {
                // if (applRel.getRelationship() == Relationship.INFO_FROM
                // || applRel.getRelationship() == Relationship.INFO_TO) {
                // list.add(applRel.getElem2());
                // }
            } else if (applRel.getRelationship() == aeRelationship) {
                list.add(applRel.getElem2());
            }
        }
        return list.toArray(new ApplicationElement[0]);
    }

    /***************************************************************************************
     * methods for instance elements
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#createInstance(java.lang.String)
     */
    public InstanceElement createInstance(String ieName) throws AoException {
        String idValName = this.atfxCache.getApplicationAttributeByBaName(this.aid, "id").getName();
        long iid = this.atfxCache.nextIid(this.aid);
        this.atfxCache.addInstance(this.aid, iid);
        this.atfxCache.setInstanceValue(this.aid, iid, idValName, ODSHelper.createLongLongNV(idValName, iid).value);
        InstanceElement ie = this.atfxCache.getInstanceById(this.poa, aid, iid);
        ie.setName(ieName);
        return ie;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#listInstances(java.lang.String)
     */
    public NameIterator listInstances(String iePattern) throws AoException {
        try {
            List<String> list = new ArrayList<String>();
            for (InstanceElement ie : this.atfxCache.getInstances(poa, aid)) {
                String name = ie.getName();
                if (PatternUtil.nameFilterMatch(name, iePattern)) {
                    list.add(name);
                }
            }
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.poa, list.toArray(new String[0]));
            this.poa.activate_object(nIteratorImpl);
            return NameIteratorHelper.narrow(this.poa.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getInstances(java.lang.String)
     */
    public InstanceElementIterator getInstances(String iePattern) throws AoException {
        try {
            List<InstanceElement> list = new ArrayList<InstanceElement>();
            for (InstanceElement ie : this.atfxCache.getInstances(poa, aid)) {
                String name = ie.getName();
                if (PatternUtil.nameFilterMatch(name, iePattern)) {
                    list.add(ie);
                }
            }
            InstanceElement[] ieAr = list.toArray(new InstanceElement[0]);
            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.poa, ieAr);
            this.poa.activate_object(ieIteratorImpl);
            return InstanceElementIteratorHelper.narrow(this.poa.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getInstanceById(org.asam.ods.T_LONGLONG)
     */
    public InstanceElement getInstanceById(T_LONGLONG ieId) throws AoException {
        InstanceElement ie = this.atfxCache.getInstanceById(this.poa, aid, ODSHelper.asJLong(ieId));
        if (ie == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceElement aid=" + aid + ",iid="
                    + ODSHelper.asJLong(ieId) + " not found");
        }
        return ie;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getInstanceByName(java.lang.String)
     */
    public InstanceElement getInstanceByName(String ieName) throws AoException {
        InstanceElement found = null;
        for (InstanceElement ie : this.atfxCache.getInstances(this.poa, this.aid)) {
            if (ie.getName().equals(ieName)) {
                // check if duplicate
                if (found != null) {
                    throw new AoException(ErrorCode.AO_DUPLICATE_VALUE, SeverityFlag.ERROR, 0,
                                          "Multiple instances found for '" + ieName + "'");
                }
                found = ie;
            }
        }
        return found;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#createInstances(org.asam.ods.NameValueSeqUnit[],
     *      org.asam.ods.ApplicationRelationInstanceElementSeq[])
     */
    public InstanceElement[] createInstances(NameValueSeqUnit[] attributes,
            ApplicationRelationInstanceElementSeq[] relatedInstances) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'createInstances' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#removeInstance(org.asam.ods.T_LONGLONG, boolean)
     */
    public void removeInstance(T_LONGLONG ieId, boolean recursive) throws AoException {
        // check if instance exists
        long iid = ODSHelper.asJLong(ieId);
        if (this.atfxCache.getInstanceById(this.poa, aid, iid) == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceElement aid=" + aid + ",iid="
                    + ODSHelper.asJLong(ieId) + " not found");
        }
        // remove recursively
        InstanceElement ie = this.atfxCache.getInstanceById(this.poa, aid, iid);
        if (recursive) {
            InstanceElementIterator iter = ie.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
            for (int i = 0; i < iter.getCount(); i++) {
                InstanceElement childIe = iter.nextOne();
                ApplicationElement ae = childIe.getApplicationElement();
                ae.removeInstance(childIe.getId(), true);
            }
            iter.destroy();
        }
        // remove instance
        this.atfxCache.removeInstance(this.aid, iid);
        // deactivate CORBA object
        try {
            byte[] id = poa.reference_to_id(ie);
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
        LOG.debug("Removed instance aid=" + aid + ",iid=" + iid);
    }

    /***************************************************************************************
     * methods for security
     ***************************************************************************************/

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setRights(org.asam.ods.InstanceElement, int,
     *      org.asam.ods.RightsSet)
     */
    public void setRights(InstanceElement usergroup, int rights, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'setRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getRights()
     */
    public ACL[] getRights() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'getRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getInitialRights()
     */
    public InitialRight[] getInitialRights() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setInitialRights(org.asam.ods.InstanceElement, int,
     *      org.asam.ods.T_LONGLONG, org.asam.ods.RightsSet)
     */
    public void setInitialRights(InstanceElement usergroup, int rights, T_LONGLONG refAid, RightsSet set)
            throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInitialRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setInitialRightRelation(org.asam.ods.ApplicationRelation, boolean)
     */
    public void setInitialRightRelation(ApplicationRelation applRel, boolean set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setInitialRightRelation' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getInitialRightRelations()
     */
    public ApplicationRelation[] getInitialRightRelations() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getInitialRightRelations' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#getSecurityLevel()
     */
    public int getSecurityLevel() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'getSecurityLevel' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationElementOperations#setSecurityLevel(int, org.asam.ods.RightsSet)
     */
    public void setSecurityLevel(int secLevel, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0,
                              "Method 'setSecurityLevel' not implemented");
    }

}
