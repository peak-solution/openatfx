package de.rechner.openatfx.basestructure;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseAttributeHelper;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseElementHelper;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseRelationHelper;
import org.asam.ods.BaseStructure;
import org.asam.ods.BaseStructureHelper;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.EnumerationDefinitionHelper;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.SeverityFlag;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rechner.openatfx.util.ODSHelper;
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


public class NewBaseModelReader extends BasicModelReader {

    private static final Logger LOG = LoggerFactory.getLogger(NewBaseModelReader.class);

    private Collection<String> topLevelElementNames = new HashSet<String>(Arrays.asList("aoenvironment", "aotest",
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
    
    @Override
    BaseStructure readBaseStructure(String baseModelVersion, POA poa) throws AoException {
        try {
            InputStream in = NewBaseModelReader.class.getResourceAsStream("ODSBaseModel_" + baseModelVersion + ".xml");
            JAXBContext context = JAXBContext.newInstance(ODSBaseModel.class);
            ODSBaseModel model = (ODSBaseModel) context.createUnmarshaller().unmarshal(in);
            return parseBaseModel(poa, model);
        } catch (JAXBException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * @param poa
     * @param model
     * @return
     * @throws AoException
     */
    private BaseStructure parseBaseModel(POA poa, ODSBaseModel model) throws AoException {
        try {
            Map<String, EnumerationDefinition> enumDefMap = parseEnumerations(poa, model.getModel().getEnumeration());
            Map<String, BaseElementImpl> baseElements = parseElements(poa, enumDefMap, model.getModel().getElement());
            parseBaseRelations(poa, model.getModel().getElement(), baseElements);

            BaseStructureImpl baseStructureImpl = new BaseStructureImpl(model.getVersion());
            for (BaseElementImpl baseElementImpl : baseElements.values()) {
                BaseElement baseElement = BaseElementHelper.narrow(poa.servant_to_reference(baseElementImpl));
                baseStructureImpl.addBaseElement(baseElement);
            }

            BaseStructure baseStructure = BaseStructureHelper.narrow(poa.servant_to_reference(baseStructureImpl));
            return baseStructure;
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * @param poa
     * @param enumerations
     * @return
     * @throws ServantAlreadyActive
     * @throws WrongPolicy
     * @throws ServantNotActive
     */
    private Map<String, EnumerationDefinition> parseEnumerations(POA poa, List<Enumeration> enumerations)
            throws ServantAlreadyActive, WrongPolicy, ServantNotActive {
        Map<String, EnumerationDefinition> map = new HashMap<String, EnumerationDefinition>();

        int artificialEnumIndex = 0;
        for (Enumeration enumeration : enumerations) {
            BaseEnumerationDefinitionImpl enumerationImpl = new BaseEnumerationDefinitionImpl(artificialEnumIndex++,
                                                                                              enumeration.getName());
            // parse items
            for (Item item : enumeration.getItem()) {
                enumerationImpl.addItem((int) item.getValue(), item.getName());
            }

            EnumerationDefinition enumDef = EnumerationDefinitionHelper.narrow(poa.servant_to_reference(enumerationImpl));
            map.put(enumeration.getName(), enumDef);
        }

        return map;
    }

    /**
     * @param poa
     * @param enumDefs
     * @param elements
     * @return
     * @throws AoException
     * @throws ServantAlreadyActive
     * @throws WrongPolicy
     * @throws ServantNotActive
     */
    private Map<String, BaseElementImpl> parseElements(POA poa, Map<String, EnumerationDefinition> enumDefs,
            List<Element> elements) throws AoException, ServantAlreadyActive, WrongPolicy, ServantNotActive {
        Map<String, BaseElementImpl> baseElements = new HashMap<>();

        for (Element element : elements) {
            boolean isTopLevelElement = topLevelElementNames.contains(element.getName());
            BaseElementImpl baseElementImpl = new BaseElementImpl(element.getName(), isTopLevelElement);
            BaseElement baseElement = BaseElementHelper.narrow(poa.servant_to_reference(baseElementImpl));
            for (BaseAttribute baseAttr : parseBaseAttributes(poa, enumDefs, baseElement, element.getAttribute(),
                                                              element.getUniqueness())) {
                baseElementImpl.addBaseAttribute(baseAttr);
            }

            baseElements.put(element.getName(), baseElementImpl);
        }

        return baseElements;
    }

    /**
     * @param poa
     * @param enumDefs
     * @param baseElement
     * @param attributes
     * @param uniquenesses
     * @return
     * @throws AoException
     * @throws ServantNotActive
     * @throws WrongPolicy
     */
    private BaseAttribute[] parseBaseAttributes(POA poa, Map<String, EnumerationDefinition> enumDefs,
            BaseElement baseElement, List<Attribute> attributes, List<Uniqueness> uniquenesses)
            throws AoException, ServantNotActive, WrongPolicy {
        List<BaseAttribute> list = new ArrayList<BaseAttribute>();

        for (Attribute attribute : attributes) {
            // get enum name
            String enumName = attribute.getEnumerationType();
            
            // get DataType
            DataType dataType = ODSHelper.string2dataType(attribute.getDatatype().name());
            // get uniqueness
            boolean unique = false;
            for (Uniqueness uni : uniquenesses) {
                if (uni.getItem().size() == 1 && uni.getItem().get(0).equalsIgnoreCase(attribute.getName())) {
                    unique = true;
                    break;
                }
            }
            BaseAttributeImpl baseAttrImpl = new BaseAttributeImpl(attribute.getName(), dataType,
                                                                   attribute.isObligatory(), unique, baseElement,
                                                                   enumDefs.get(enumName));
            BaseAttribute baseAttribute = BaseAttributeHelper.narrow(poa.servant_to_reference(baseAttrImpl));
            list.add(baseAttribute);

            autogeneratedFlags.computeIfAbsent(baseElement.getType(), v -> new HashMap<>())
                              .put(baseAttribute.getName(), attribute.isAutogenerated());
        }

        return list.toArray(new BaseAttribute[0]);
    }
    
    /**
     * @param poa
     * @param elements
     * @param baseElements
     * @throws AoException
     * @throws ServantAlreadyActive
     * @throws WrongPolicy
     * @throws ServantNotActive
     */
    private void parseBaseRelations(POA poa, List<Element> elements, Map<String, BaseElementImpl> baseElements)
            throws AoException, ServantAlreadyActive, WrongPolicy, ServantNotActive {
        // first collect info for normal as well as inverse relation directions
        Map<BaseElementImpl, Map<String, RelationRange>> relationRanges = new HashMap<>();
        Map<BaseElementImpl, Map<String, Relationship>> relationShips = new HashMap<>();
        Map<BaseElementImpl, Map<String, RelationType>> relationTypes = new HashMap<>();
        for (Element element : elements) {
            for (Relation relation : element.getRelation()) {
                BaseElementImpl elementImpl = baseElements.get(element.getName());

                RelationRange relationRange = new RelationRange();
                relationRange.min = ODSHelper.string2relRange(relation.getMinOccurs());
                relationRange.max = ODSHelper.string2relRange(relation.getMaxOccurs());
                relationRanges.computeIfAbsent(elementImpl, v -> new HashMap<>()).put(relation.getName(),
                                                                                      relationRange);

                RelationshipEnum relEnum = relation.getRelationship();
                Relationship relationship = ODSHelper.string2relationship(relEnum.value());
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
                for (String refTo : relation.getRefTo()) {
                    BaseElementImpl elem1Impl = baseElements.get(element.getName());
                    BaseElementImpl elem2Impl = baseElements.get(refTo);
                    BaseElement elem1 = BaseElementHelper.narrow(poa.servant_to_reference(elem1Impl));
                    BaseElement elem2 = BaseElementHelper.narrow(poa.servant_to_reference(elem2Impl));
                    

                    String inverseRelName = getInverseRelationName(element, relation, refTo);
                    RelationRange relationRange = relationRanges.get(elem1Impl).get(relation.getName());
                    RelationRange inverseRelationRange = relationRanges.get(elem2Impl).get(inverseRelName);
                    Relationship relationship = relationShips.get(elem1Impl).get(relation.getName());
                    Relationship inverseRelationship = relationShips.get(elem2Impl).get(inverseRelName);
                    RelationType relationType = relationTypes.get(elem1Impl).get(relation.getName());

                    BaseRelationImpl baseRelationImpl = new BaseRelationImpl(elem1, elem2, relation.getName(),
                                                                             inverseRelName, relationRange,
                                                                             inverseRelationRange, relationship,
                                                                             inverseRelationship, relationType);
                    BaseRelation baseRelation = BaseRelationHelper.narrow(poa.servant_to_reference(baseRelationImpl));
                    elem1Impl.addBaseRelation(baseRelation);
                }
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
     * @throws AoException
     */
    private String getInverseRelationName(Element baseElement, Relation relation, String refTo) throws AoException {
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
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                  "NewBaseModelReader.getFixedEmptyInverseName() called with unexpected base element "
                                          + baseElement.getName());
        }
        return inverseRelNameFromRelation;
    }
}
