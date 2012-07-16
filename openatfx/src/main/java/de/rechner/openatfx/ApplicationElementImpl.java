package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.Collection;
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

    private final POA modelPOA;
    private final POA instancePOA;
    private final AtfxCache atfxCache;
    private final ApplicationStructure applicationStructure;
    private final BaseElement baseElement;
    private final long aid;

    /**
     * Constructor.
     * 
     * @param modelPOA The model POA.
     * @param instancePOA The instance POA.
     * @param atfxCache The ATFX cache.
     * @param applicationStructure The application structure.
     * @param baseElement The base element.
     * @param aid The application element id;
     */
    public ApplicationElementImpl(POA modelPOA, POA instancePOA, AtfxCache atfxCache,
            ApplicationStructure applicationStructure, BaseElement baseElement, long aid) {
        this.modelPOA = modelPOA;
        this.instancePOA = instancePOA;
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
        try {
            int attrNo = this.atfxCache.nextAttrNo(this.aid);
            ApplicationAttributeImpl aaImpl = new ApplicationAttributeImpl(this.atfxCache, this.aid, attrNo);
            ApplicationAttribute aa = ApplicationAttributeHelper.narrow(modelPOA.servant_to_reference(aaImpl));
            this.atfxCache.addApplicationAttribute(this.aid, attrNo, aa);
            return aa;
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
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
        Integer attrNo = this.atfxCache.getAttrNoByName(this.aid, aaName);
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "ApplicationAttribute '" + aaName
                    + "' not found");
        }
        return this.atfxCache.getApplicationAttribute(this.aid, attrNo);
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
        // get application attribute
        Integer attrNo = this.atfxCache.getAttrNoByBaName(this.aid, baName);
        if (attrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "ApplicationAttribute by base attribute name '" + baName
                                          + "' not found for ApplicationElement '" + getName() + "'");
        }
        return this.atfxCache.getApplicationAttribute(aid, attrNo);
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
            byte[] id = modelPOA.reference_to_id(applAttr);
            modelPOA.deactivate_object(id);
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
            if ((baseRel != null) && baseRel.getRelationName().equalsIgnoreCase(baseRelName)) {
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
        Integer idAttrNo = this.atfxCache.getAttrNoByBaName(this.aid, "id");
        if (idAttrNo == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "Not application attribute of base attribute 'id' found for aid=" + aid);
        }
        long iid = this.atfxCache.nextIid(this.aid);
        this.atfxCache.addInstance(this.aid, iid);

        // set id value
        this.atfxCache.setInstanceValue(this.aid, iid, idAttrNo, ODSHelper.asODSLongLong(iid));

        // set instance name and return reference
        InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, aid, iid);
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
            for (InstanceElement ie : this.atfxCache.getInstances(this.instancePOA, aid)) {
                String name = ie.getName();
                if (PatternUtil.nameFilterMatch(name, iePattern)) {
                    list.add(name);
                }
            }
            NameIteratorImpl nIteratorImpl = new NameIteratorImpl(this.modelPOA, list.toArray(new String[0]));
            return NameIteratorHelper.narrow(this.modelPOA.servant_to_reference(nIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
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
            InstanceElement[] ieAr = null;
            // check filter 'all' for performance tuning
            if (iePattern.equals("*")) {
                ieAr = this.atfxCache.getInstances(this.instancePOA, aid);
            } else {
                Collection<InstanceElement> list = new ArrayList<InstanceElement>();
                for (InstanceElement ie : this.atfxCache.getInstances(this.instancePOA, aid)) {
                    if (PatternUtil.nameFilterMatch(ie.getName(), iePattern)) {
                        list.add(ie);
                    }
                }
                ieAr = list.toArray(new InstanceElement[0]);
            }

            InstanceElementIteratorImpl ieIteratorImpl = new InstanceElementIteratorImpl(this.modelPOA, ieAr);
            return InstanceElementIteratorHelper.narrow(this.modelPOA.servant_to_reference(ieIteratorImpl));
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
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
        InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, aid, ODSHelper.asJLong(ieId));
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
        for (InstanceElement ie : this.atfxCache.getInstances(this.instancePOA, this.aid)) {
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
        if (this.atfxCache.getInstanceById(this.instancePOA, aid, iid) == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "InstanceElement aid=" + aid + ",iid="
                    + ODSHelper.asJLong(ieId) + " not found");
        }
        // remove recursively
        InstanceElement ie = this.atfxCache.getInstanceById(this.instancePOA, aid, iid);
        if (recursive) {
            InstanceElementIterator iter = ie.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
            InstanceElement[] instances = iter.nextN(iter.getCount());
            iter.destroy();
            for (int i = 0; i < instances.length; i++) {
                ApplicationElement ae = instances[i].getApplicationElement();
                ae.removeInstance(instances[i].getId(), true);
            }
        }
        // remove instance
        this.atfxCache.removeInstance(this.aid, iid);
        LOG.debug("Removed instance aid=" + this.aid + ",iid=" + iid);
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
