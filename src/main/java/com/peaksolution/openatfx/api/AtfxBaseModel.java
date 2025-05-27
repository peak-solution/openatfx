package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.asam.ods.ErrorCode;

import com.peaksolution.openatfx.util.PatternUtil;


class AtfxBaseModel implements BaseModel {

    private final String version;
    private final List<EnumerationDefinition> enumerations;
    private final List<AtfxBaseElement> baseElements;

    /**
     * Constructor.
     * 
     * @param version The base model version.
     */
    public AtfxBaseModel(final String version) {
        this.version = version;
        this.enumerations = new ArrayList<>();
        this.baseElements = new ArrayList<>();
    }

    public void addBaseEnumerations(Collection<EnumerationDefinition> enums) {
        enumerations.addAll(enums);
    }

    public void addBaseEnumeration(EnumerationDefinition enumDef) {
        enumerations.add(enumDef);
    }

    @Override
    public Collection<EnumerationDefinition> getEnumerations() {
        return enumerations;
    }

    @Override
    public EnumerationDefinition getEnumDef(String enumName) {
        for (EnumerationDefinition currentEnum : enumerations) {
            if (currentEnum.getName().equals(enumName)) {
                return currentEnum;
            }
        }
        return null;
    }

    /**
     * Add a base element.
     * 
     * @param baseElement The base element to add.
     */
    public void addBaseElement(AtfxBaseElement baseElement) {
        this.baseElements.add(baseElement);
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    public String[] listTopLevelElements(String bePattern) {
        List<String> list = new ArrayList<String>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (baseElement.isTopLevel() && PatternUtil.nameFilterMatchCI(type, bePattern)) {
                list.add(type);
            }
        }
        return list.toArray(new String[0]);
    }

    public BaseElement[] getTopLevelElements(String bePattern) {
        List<BaseElement> list = new ArrayList<>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (baseElement.isTopLevel() && PatternUtil.nameFilterMatchCI(type, bePattern)) {
                list.add(baseElement);
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    @Override
    public BaseElement[] getElements(String bePattern) {
        List<BaseElement> list = new ArrayList<>();
        for (BaseElement baseElement : this.baseElements) {
            String type = baseElement.getType();
            if (PatternUtil.nameFilterMatchCI(type, bePattern)) {
                list.add(baseElement);
            }
        }
        return list.toArray(new BaseElement[0]);
    }

    @Override
    public AtfxBaseElement getElementByType(String beType) {
        for (AtfxBaseElement baseElement : this.baseElements) {
            if (baseElement.getType().equalsIgnoreCase(beType)) {
                return baseElement;
            }
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Base element '" + beType + "' not found");
    }

    public BaseRelation getRelation(String type1, String type2) {
        return getRelation(getElementByType(type1), getElementByType(type2));
    }

    public BaseRelation getRelation(BaseElement elem1, BaseElement elem2) {
        for (BaseRelation br : elem1.getRelations()) {
            if (br.getElem2().contains(elem2)) {
                return br;
            }
        }
        throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No base relation found between '" + elem1.getType()
                + "' and '" + elem2.getType() + "'");
    }

    public BaseRelation[] getRelations(BaseElement elem1, BaseElement elem2) {
        List<BaseRelation> list = new ArrayList<>();
        for (BaseRelation br : elem1.getRelations()) {
            if (br.getElem2().contains(elem2)) {
                list.add(br);
            }
        }
        return list.toArray(new BaseRelation[0]);
    }
}
