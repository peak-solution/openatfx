package de.rechner.openatfx;

import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.BaseAttribute;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Assists with copying instance elements.
 * 
 * @author Steinbfl
 */
class InstanceElementCopyHelper {

    private final AtfxCache atfxCache;

    public InstanceElementCopyHelper(AtfxCache atfxCache) {
        this.atfxCache = atfxCache;
    }

    /**
     * Check if the parameters given are sufficient to copy the instance element. The conditions are: Either 'newName'
     * or 'newVersion' must not be empty. Also: If the given instance element has no version attribute, 'newName' must
     * not be empty.
     * 
     * @param ie
     * @param newName
     * @param newVersion
     * @throws AoException
     */
    private void checkPreconditions(InstanceElement ie, String newName, String newVersion) throws AoException {
        // check, if there is name/version, only name or only version given.
        // check
        // if the instance element has a version(if not, giving only a version
        // is invalid and results in an exception)
        if ((newName == null || newName.length() < 1)) {
            if (!this.hasVersionAttribute(ie)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "The new name given was null or empty. Must provide a new name, because the instance element is not versionable!");
            } else if (newVersion == null || newVersion.length() < 1) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "The new version and the new name were both was null or empty. Must provide at least one of the two!");
            }
        }
    }

    private void copyAttributeValues(InstanceElement ie, InstanceElement copy, String newVersion) throws AoException {
        // get the attribute value names
        ApplicationAttribute[] allApplicationAttributes = ie.getApplicationElement().getAttributes("*");
        List<String> allApplicationAttributeNames = new ArrayList<String>();
        for (ApplicationAttribute applicationAttribute : allApplicationAttributes) {
            // do not change id and name
            BaseAttribute ba = applicationAttribute.getBaseAttribute();
            if (ba != null && ba.getName() != null) {
                if (ba.getName().equals("id") || ba.getName().equals("name")) {
                    continue;
                }
            }
            allApplicationAttributeNames.add(applicationAttribute.getName());
        }

        NameValueUnit[] valueSeq = ie.getValueSeq(allApplicationAttributeNames.toArray(new String[0]));

        // set the attribute values
        for (NameValueUnit oldNVU : valueSeq) {
            copy.setValue(ODSHelper.cloneNVU(oldNVU));
        }

        // set version
        if (this.hasVersionAttribute(ie)) {
            copy.setValue(ODSHelper.createStringNVU(ie.getApplicationElement().getAttributeByBaseName("version")
                                                      .getName(), newVersion));
        }
    }

    private InstanceElement copyInstanceElement(InstanceElement ie, String newName) throws AoException {
        InstanceElement copy = ie.getApplicationElement().createInstance(newName);
        return copy;
    }

    private void copyInfoRelations(InstanceElement ie, InstanceElement copy) throws AoException {
        // get the info relations
        List<ApplicationRelation> infoRelationsList = new ArrayList<ApplicationRelation>();

        for (ApplicationRelation ar : ie.getApplicationElement().getAllRelations()) {
            if (ar.getRelationType().equals(RelationType.INFO)) {
                // to prevent overwriting of relations: get only those info
                // relations where the foreign key is in the
                // copied element.
                if (ar.getInverseRelationRange().max == -1) {
                    infoRelationsList.add(ar);
                }
            }
        }

        // set the info relations
        for (ApplicationRelation ar : infoRelationsList) {
            InstanceElementIterator iter = ie.getRelatedInstances(ar, "*");
            for (InstanceElement relatedIE : iter.nextN(iter.getCount())) {
                copy.createRelation(ar, relatedIE);
                relatedIE.destroy();
            }
            iter.destroy();
        }
    }

    private void setParentRelation(InstanceElement ie, InstanceElement copy) throws AoException {
        // get the parent relations. (it IS possible for an application
        // element to have multiple parent application elements)
        List<ApplicationRelation> parentRelationsList = new ArrayList<ApplicationRelation>();

        for (ApplicationRelation ar : ie.getApplicationElement().getAllRelations()) {
            if (ar.getRelationship().equals(Relationship.FATHER)) {
                parentRelationsList.add(ar);
            }
        }

        // set the parent (it is NOT allowed for an instance element to have
        // multiple parent instance elements)
        boolean parentFound = false;
        for (ApplicationRelation parentAR : parentRelationsList) {
            InstanceElementIterator parentIEIter = ie.getRelatedInstances(parentAR, "*");
            for (InstanceElement parentIE : parentIEIter.nextN(parentIEIter.getCount())) {
                if (parentFound) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "Multiple parent instances found for instance [id="
                                                  + ODSHelper.asJLong(ie.getId()) + "]");
                }
                parentFound = true;
                copy.createRelation(parentAR, parentIE);
                parentIE.destroy();
            }
            parentIEIter.destroy();
        }
    }

    private void copyChildren(InstanceElement ie, InstanceElement copy) throws AoException {
        List<ApplicationRelation> childRelationsList = new ArrayList<ApplicationRelation>();

        // get all child relations
        for (ApplicationRelation ar : ie.getApplicationElement().getAllRelations()) {
            if (ar.getRelationship().equals(Relationship.CHILD)) {
                childRelationsList.add(ar);
            }
        }

        // set the child relations
        for (ApplicationRelation ar : childRelationsList) {
            InstanceElementIterator iter = ie.getRelatedInstances(ar, "*");
            for (InstanceElement relatedChildIE : iter.nextN(iter.getCount())) {
                String relatedIeChildVersion = null;
                if (this.hasVersionAttribute(relatedChildIE)) {
                    String versionAttributeName = relatedChildIE.getApplicationElement()
                                                                .getAttributeByBaseName("version").getName();
                    relatedIeChildVersion = ODSHelper.getStringVal(relatedChildIE.getValue(versionAttributeName));
                }

                this.checkPreconditions(relatedChildIE, relatedChildIE.getName(), relatedIeChildVersion);
                InstanceElement copiedRelatedChildIE = this.copyInstanceElement(relatedChildIE,
                                                                                relatedChildIE.getName());
                this.copyAttributeValues(relatedChildIE, copiedRelatedChildIE, relatedIeChildVersion);
                this.copyInfoRelations(relatedChildIE, copiedRelatedChildIE);
                this.copyChildren(relatedChildIE, copiedRelatedChildIE);
                copy.createRelation(ar, copiedRelatedChildIE);
                relatedChildIE.destroy();
                copiedRelatedChildIE.destroy();
            }
            iter.destroy();
        }
    }

    public boolean hasVersionAttribute(InstanceElement ie) throws AoException {
        long aid = ODSHelper.asJLong(ie.getApplicationElement().getId());
        return atfxCache.getAttrNoByBaName(aid, "version") != null;
    }

    public InstanceElement shallowCopy(InstanceElement ie, String newName, String newVersion) throws AoException {
        this.checkPreconditions(ie, newName, newVersion);
        if (newName == null || newName.length() < 1) {
            newName = ie.getName();
        }
        InstanceElement copy = this.copyInstanceElement(ie, newName);
        this.copyAttributeValues(ie, copy, newVersion);
        this.copyInfoRelations(ie, copy);
        this.setParentRelation(ie, copy);
        return copy;
    }

    public InstanceElement deepCopy(InstanceElement ie, String newName, String newVersion) throws AoException {
        this.checkPreconditions(ie, newName, newVersion);
        if (newName == null || newName.length() < 1) {
            newName = ie.getName();
        }
        InstanceElement copy = this.copyInstanceElement(ie, newName);
        this.copyAttributeValues(ie, copy, newVersion);
        this.copyInfoRelations(ie, copy);
        this.copyChildren(ie, copy);
        this.setParentRelation(ie, copy);
        return copy;
    }

}
