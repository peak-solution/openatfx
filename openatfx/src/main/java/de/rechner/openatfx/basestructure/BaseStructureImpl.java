package de.rechner.openatfx.basestructure;

import java.util.ArrayList;
import java.util.List;

import org.asam.ods.AoException;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructurePOA;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;

import de.rechner.openatfx.util.PatternUtil;


/**
 * Implementation of <code>org.asam.ods.BaseStructure</code>.
 * 
 * @author Christian Rechner
 */
class BaseStructureImpl extends BaseStructurePOA {

    private final String version;
    private final List<BaseElement> baseElements;

    /**
     * Constructor.
     * 
     * @param version The base model version.
     */
    public BaseStructureImpl(final String version) {
        this.version = version;
        this.baseElements = new ArrayList<BaseElement>();
    }

    public void addBaseElement(BaseElement baseElement) {
        this.baseElements.add(baseElement);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#getVersion()
     */
    public String getVersion() throws AoException {
        return this.version;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#listTopLevelElements(java.lang.String)
     */
    public String[] listTopLevelElements(String bePattern) throws AoException {
        List<String> list = new ArrayList<String>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (baseElement.isTopLevel() && PatternUtil.nameFilterMatchCaseInsensitive(type, bePattern)) {
                list.add(type);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#getTopLevelElements(java.lang.String)
     */
    public BaseElement[] getTopLevelElements(String bePattern) throws AoException {
        List<BaseElement> list = new ArrayList<BaseElement>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (baseElement.isTopLevel() && PatternUtil.nameFilterMatchCaseInsensitive(type, bePattern)) {
                list.add(baseElement);
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#listElements(java.lang.String)
     */
    public String[] listElements(String bePattern) throws AoException {
        List<String> list = new ArrayList<String>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (PatternUtil.nameFilterMatchCaseInsensitive(type, bePattern)) {
                list.add(type);
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#getElements(java.lang.String)
     */
    public BaseElement[] getElements(String bePattern) throws AoException {
        List<BaseElement> list = new ArrayList<BaseElement>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (PatternUtil.nameFilterMatchCaseInsensitive(type, bePattern)) {
                list.add(baseElement);
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#getElementByType(java.lang.String)
     */
    public BaseElement getElementByType(String beType) throws AoException {
        for (BaseElement baseElement : this.baseElements) {
            if (baseElement.getType().equalsIgnoreCase(beType)) {
                return baseElement;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Base element '" + beType + "' not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseStructureOperations#getRelation(org.asam.ods.BaseElement, org.asam.ods.BaseElement)
     */
    public BaseRelation getRelation(BaseElement elem1, BaseElement elem2) throws AoException {
        for (BaseRelation br : elem1.getAllRelations()) {
            if (br.getElem2().getType().equals(elem2.getType())) {
                return br;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "No base relation found between '"
                + elem1.getType() + "' and '" + elem2.getType() + "'");
    }

}
