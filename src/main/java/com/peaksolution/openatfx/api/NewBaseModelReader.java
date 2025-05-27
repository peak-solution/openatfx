package com.peaksolution.openatfx.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.util.ODSHelper;

import generated.ODSBaseModel;
import generated.ODSBaseModel.Model.Element;
import generated.ODSBaseModel.Model.Element.Attribute;
import generated.ODSBaseModel.Model.Element.Relation;
import generated.ODSBaseModel.Model.Element.Uniqueness;
import generated.ODSBaseModel.Model.Enumeration;
import generated.ODSBaseModel.Model.Enumeration.Item;
import generated.RelationshipEnum;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;


public class NewBaseModelReader implements BaseModelReader {

    private static final Logger LOG = LoggerFactory.getLogger(NewBaseModelReader.class);

    private Collection<String> topLevelElementNames = new HashSet<>(Arrays.asList("aoenvironment", "aotest",
                                                                                        "aophysicaldimension",
                                                                                        "aoquantity",
                                                                                        "aoquantitygroup",
                                                                                        "aotestequipment",
                                                                                        "aotestsequence",
                                                                                        "aounitundertest",
                                                                                        "aounit",
                                                                                        "aounitgroup",
                                                                                        "aouser",
                                                                                        "aousergroup",
                                                                                        "aoany",
                                                                                        "aolog",
                                                                                        "aoparameterset",
                                                                                        "aonamemap",
                                                                                        "aomimetypemap"));
    
    public AtfxBaseModel getBaseModel(String baseModelVersion) {
        ODSBaseModel model = readODSBaseModel(baseModelVersion);
        return parseBaseModel(model);
    }
    
    private ODSBaseModel readODSBaseModel(String baseModelVersion) {
        try {
            InputStream in = NewBaseModelReader.class.getResourceAsStream("ODSBaseModel_" + baseModelVersion + ".xml");
            if (in == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No base model file found for version " + baseModelVersion);
            }
            
            JAXBContext context = JAXBContext.newInstance(ODSBaseModel.class);
            return (ODSBaseModel) context.createUnmarshaller().unmarshal(in);
        } catch (JAXBException e) {
            throw new OpenAtfxException(ErrorCode.AO_UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * @param model
     * @return
     */
    private AtfxBaseModel parseBaseModel(ODSBaseModel model) {
        Collection<EnumerationDefinition> enums = parseEnumerations(model.getModel().getEnumeration());
        Map<String, AtfxBaseElement> baseElements = parseElements(enums, model.getModel().getElement());
        parseBaseRelations(model.getModel().getElement(), baseElements);

        AtfxBaseModel baseModel = new AtfxBaseModel(model.getVersion());
        baseModel.addBaseEnumerations(enums);
        for (AtfxBaseElement baseElement : baseElements.values()) {
            baseModel.addBaseElement(baseElement);
        }

        return baseModel;
    }

    /**
     * @param enumerations
     * @return
     */
    protected Collection<EnumerationDefinition> parseEnumerations(List<Enumeration> enumerations) {
        Collection<EnumerationDefinition> enums = new ArrayList<>();
        int artificialEnumIndex = 0;
        for (Enumeration currentEnumeration : enumerations) {
            AtfxEnumeration baseEnumeration = new AtfxEnumeration(artificialEnumIndex++, currentEnumeration.getName());
            // parse items
            for (Item item : currentEnumeration.getItem()) {
                baseEnumeration.addItem((int) item.getValue(), item.getName());
            }
            enums.add(baseEnumeration);
        }
        return enums;
    }

    /**
     * @param enumDefs
     * @param elements
     * @return
     */
    protected Map<String, AtfxBaseElement> parseElements(Collection<EnumerationDefinition> enumDefs,
            List<Element> elements) {
        Map<String, AtfxBaseElement> baseElements = new HashMap<>();

        List<String> enumNames = enumDefs.stream().map(EnumerationDefinition::getName).toList();
        
        for (Element element : elements) {
            boolean isTopLevelElement = topLevelElementNames.contains(element.getName());
            AtfxBaseElement baseElement = new AtfxBaseElement(element.getName(), isTopLevelElement);
            for (BaseAttribute baseAttr : parseBaseAttributes(enumNames, baseElement, element.getAttribute(),
                                                              element.getUniqueness())) {
                baseElement.addBaseAttribute(baseAttr);
            }

            baseElements.put(element.getName(), baseElement);
        }

        return baseElements;
    }

    /**
     * @param enumNames
     * @param baseElement
     * @param attributes
     * @param uniquenesses
     * @return
     */
    private BaseAttribute[] parseBaseAttributes(Collection<String> enumNames, BaseElement baseElement,
            List<Attribute> attributes, List<Uniqueness> uniquenesses) {
        List<BaseAttribute> list = new ArrayList<>();

        for (Attribute attribute : attributes) {
            // get enum name
            String enumName = attribute.getEnumerationType();
            
            // get DataType
            DataType dataType = DataType.fromString(attribute.getDatatype().name());
            // get uniqueness
            boolean unique = false;
            for (Uniqueness uni : uniquenesses) {
                if (uni.getItem().size() == 1 && uni.getItem().get(0).equalsIgnoreCase(attribute.getName())) {
                    unique = true;
                    break;
                }
            }
            
            if (enumName != null && !enumName.isBlank() && !enumNames.contains(enumName)) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, attribute + " at " + baseElement
                        + " references unknown enumeration '" + enumName + "'!");
            }
            list.add(new AtfxBaseAttribute(attribute.getName(), dataType, attribute.isMandatory(), attribute.isObligatory(),
                                           unique, attribute.isAutogenerated(), baseElement, enumName));
        }

        return list.toArray(new BaseAttribute[0]);
    }
    
    /**
     * @param elements
     * @param baseElements
     */
    private void parseBaseRelations(List<Element> elements, Map<String, AtfxBaseElement> baseElements) {
        // first collect info for normal as well as inverse relation directions
        Map<BaseElement, Map<String, RelationRange>> relationRanges = new HashMap<>();
        Map<BaseElement, Map<String, Relationship>> relationShips = new HashMap<>();
        Map<BaseElement, Map<String, RelationType>> relationTypes = new HashMap<>();
        for (Element element : elements) {
            for (Relation relation : element.getRelation()) {
                BaseElement elementImpl = baseElements.get(element.getName());

                RelationRange relationRange = new RelationRange();
                relationRange.min = ODSHelper.string2relRange(relation.getMinOccurs());
                relationRange.max = ODSHelper.string2relRange(relation.getMaxOccurs());
                relationRanges.computeIfAbsent(elementImpl, v -> new HashMap<>()).put(relation.getName(),
                                                                                      relationRange);

                RelationshipEnum relEnum = relation.getRelationship();
                Relationship relationship = Relationship.valueOf(relEnum.value());
                relationShips.computeIfAbsent(elementImpl, v -> new HashMap<>()).put(relation.getName(), relationship);

                RelationType relType = RelationType.INFO;
                if (relEnum == RelationshipEnum.CHILD || relEnum == RelationshipEnum.FATHER) {
                    relType = RelationType.FATHER_CHILD;
                }
                relationTypes.computeIfAbsent(elementImpl, v -> new HashMap<>()).put(relation.getName(), relType);
            }
        }
        
        // then create BaseRelations with both direction information
        for (Element element : elements) {
            for (Relation relation : element.getRelation()) {
                Collection<BaseElement> elements2 = new ArrayList<>();
                for (String refTo : relation.getRefTo()) {
                    BaseElement element2 = baseElements.get(refTo);
                    elements2.add(element2);
                }
                BaseElement element1 = baseElements.get(element.getName());
                BaseElement elem2ForMaps = elements2.iterator().next();
                String inverseRelName = getInverseRelationName(element, relation, elem2ForMaps.getType());
                RelationRange relationRange = relationRanges.get(element1).get(relation.getName());
                RelationRange inverseRelationRange = relationRanges.get(elem2ForMaps).get(inverseRelName);
                Relationship relationship = relationShips.get(element1).get(relation.getName());
                Relationship inverseRelationship = relationShips.get(elem2ForMaps).get(inverseRelName);
                RelationType relationType = relationTypes.get(element1).get(relation.getName());

                BaseRelation baseRelationImpl = new AtfxBaseRelation(element1, elements2, relation.getName(),
                                                                     relation.getInverseName(), relation.isMandatory(),
                                                                     relationRange, inverseRelationRange, relationship,
                                                                     inverseRelationship, relationType);
                element1.addBaseRelation(baseRelationImpl);
            }
        }
    }
    
    /**
     * The official base model has an empty inverse_name on AoTest.children and AoSubTest.children,
     * because it can be either parent or parent_test
     * 
     * @param baseElement
     * @param relation
     * @param refTo
     * @return the inverse relation name, fixed if necessary
     */
    private String getInverseRelationName(Element baseElement, Relation relation, String refTo) {
        String inverseRelNameFromRelation = relation.getInverseName();
        if (inverseRelNameFromRelation == null || inverseRelNameFromRelation.isBlank()) {
            for (Relation r : baseElement.getRelation()) {
                if (r.getName().equalsIgnoreCase(relation.getName())) {
                    if (baseElement.getName().equalsIgnoreCase("AoTest") || baseElement.getName().equalsIgnoreCase("AoSubTest")) {
                        if (refTo.equalsIgnoreCase("AoMeasurement")) {
                            return "test";
                        } else  {
                            return "parent_test";
                        }
                    }
                }
            }
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "NewBaseModelReader.getFixedEmptyInverseName() called with unexpected base element "
                                                + baseElement.getName());
        }
        return inverseRelNameFromRelation;
    }
}
