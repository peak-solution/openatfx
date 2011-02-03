package de.rechner.openatfx.basestructure;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.SeverityFlag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Singleton used to write a <code>org.asam.ods.BaseStructure</code> to a XML file.
 * 
 * @author Christian Rechner
 */
public class BaseStructureWriter {

    private static final Log LOG = LogFactory.getLog(BaseStructureWriter.class);

    /** The singleton instance */
    private static BaseStructureWriter instance;

    /**
     * Non visible constructor.
     */
    private BaseStructureWriter() {}

    /**
     * Export given base structure to an XML file.
     * 
     * @param file The target file.
     * @param baseStructure The base structure to export.
     * @throws AoException Error exporting base structure.
     */
    public void exportBaseStructure(File file, BaseStructure baseStructure) throws AoException {
        try {
            DOMSource domSource = new DOMSource(createBaseStructureXML(baseStructure));
            StreamResult streamResult = new StreamResult(file);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Creates the DOM object containing the base structure.
     * 
     * @param baseStructure The base structure object.
     * @return The DOM object.
     * @throws AoException Error reading base structure.
     */
    private Document createBaseStructureXML(BaseStructure baseStructure) throws AoException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element rootElem = doc.createElement("BaseStructure");
            rootElem.setAttribute("version", baseStructure.getVersion());
            doc.appendChild(rootElem);

            // enumeration definitions
            for (Element element : createEnumerationDefNodes(doc, baseStructure)) {
                rootElem.appendChild(element);
            }

            // base elements
            for (Element element : createBaseElementNodes(doc, baseStructure)) {
                rootElem.appendChild(element);
            }

            // base relations
            for (Element element : createBaseRelationNodes(doc, baseStructure)) {
                rootElem.appendChild(element);
            }

            return doc;
        } catch (ParserConfigurationException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Creates the XML nodes containing the enumeration definitions.
     * 
     * @param doc The XML document.
     * @param baseStructure The base structure.
     * @return The XML elements.
     * @throws AoException Error reading base structure.
     */
    private Element[] createEnumerationDefNodes(Document doc, BaseStructure baseStructure) throws AoException {
        List<Element> elements = new ArrayList<Element>();
        Set<String> exported = new HashSet<String>();
        for (BaseElement baseElement : baseStructure.getElements("*")) {
            for (BaseAttribute baseAttribute : baseElement.getAttributes("*")) {
                DataType dt = baseAttribute.getDataType();
                if (dt == DataType.DT_ENUM || dt == DataType.DS_ENUM) {
                    EnumerationDefinition enumDef = baseAttribute.getEnumerationDefinition();
                    String name = enumDef.getName();
                    if (!exported.contains(enumDef.getName())) {
                        elements.add(createEnumerationDefNode(doc, enumDef));
                        exported.add(name);
                    }
                }
            }
        }
        return elements.toArray(new Element[0]);
    }

    /**
     * Creates the XML node containing a enumeration definition.
     * 
     * @param doc The XML document.
     * @param enumDef The enumeration definition.
     * @return The XML element.
     * @throws AoException Error reading enumeration definition.
     */
    private Element createEnumerationDefNode(Document doc, EnumerationDefinition enumDef) throws AoException {
        Element enumDefElem = doc.createElement("EnumerationDefinition");
        enumDefElem.setAttribute("name", enumDef.getName());
        enumDefElem.setAttribute("index", String.valueOf(enumDef.getIndex()));
        for (String itemName : enumDef.listItemNames()) {
            Element itemElem = doc.createElement("EnumerationItem");
            itemElem.setAttribute("name", itemName);
            itemElem.setAttribute("item", String.valueOf(enumDef.getItem(itemName)));
            enumDefElem.appendChild(itemElem);
        }
        return enumDefElem;
    }

    /**
     * Creates the XML nodes containing the base elements.
     * 
     * @param doc The XML document.
     * @param baseStructure The base structure.
     * @return The XML element.
     * @throws AoException Error reading base structure.
     */
    private Element[] createBaseElementNodes(Document doc, BaseStructure baseStructure) throws AoException {
        List<Element> elements = new ArrayList<Element>();
        for (BaseElement baseElement : baseStructure.getElements("*")) {
            Element baseElementElem = doc.createElement("BaseElement");
            baseElementElem.setAttribute("type", baseElement.getType());
            baseElementElem.setAttribute("topLevel", String.valueOf(baseElement.isTopLevel()));
            for (Element baseAttrElem : createBaseAttributeNodes(doc, baseElement)) {
                baseElementElem.appendChild(baseAttrElem);
            }
            elements.add(baseElementElem);
        }
        return elements.toArray(new Element[0]);
    }

    /**
     * Creates the XML nodes containing the base attributes of a base element.
     * 
     * @param doc The XML document.
     * @param baseElement The base element.
     * @return The XML element.
     * @throws AoException Error reading base structure.
     */
    private Element[] createBaseAttributeNodes(Document doc, BaseElement baseElement) throws AoException {
        List<Element> elements = new ArrayList<Element>();
        for (BaseAttribute baseAttribute : baseElement.getAttributes("*")) {
            Element baseAttributeElem = doc.createElement("BaseAttribute");
            baseAttributeElem.setAttribute("name", baseAttribute.getName());
            DataType dataType = baseAttribute.getDataType();
            baseAttributeElem.setAttribute("dataType", ODSHelper.dataType2String(dataType));
            if (dataType == DataType.DT_ENUM || dataType == DataType.DS_ENUM) {
                baseAttributeElem.setAttribute("enumerationDefinition", baseAttribute.getEnumerationDefinition()
                                                                                     .getName());
            }
            baseAttributeElem.setAttribute("obligatory", String.valueOf(baseAttribute.isObligatory()));
            baseAttributeElem.setAttribute("unique", String.valueOf(baseAttribute.isUnique()));
            elements.add(baseAttributeElem);
        }
        return elements.toArray(new Element[0]);
    }

    /**
     * Creates the XML nodes containing the base relations.
     * 
     * @param doc The XML document.
     * @param baseStructure The base structure.
     * @return The XML elements.
     * @throws AoException Error reading base structure.
     */
    private Element[] createBaseRelationNodes(Document doc, BaseStructure baseStructure) throws AoException {
        List<Element> elements = new ArrayList<Element>();
        for (BaseElement baseElement : baseStructure.getElements("*")) {
            for (BaseRelation baseRelation : baseElement.getAllRelations()) {
                Element baseRelationElem = doc.createElement("BaseRelation");

                baseRelationElem.setAttribute("relationName", baseRelation.getRelationName());
                baseRelationElem.setAttribute("inverseRelationName", baseRelation.getInverseRelationName());

                baseRelationElem.setAttribute("elem1", baseRelation.getElem1().getType());
                baseRelationElem.setAttribute("elem2", baseRelation.getElem2().getType());

                RelationRange relationRange = baseRelation.getRelationRange();
                baseRelationElem.setAttribute("relationRangeMin", ODSHelper.relRange2string(relationRange.min));
                baseRelationElem.setAttribute("relationRangeMax", ODSHelper.relRange2string(relationRange.max));

                RelationRange inverseRelationRange = baseRelation.getInverseRelationRange();
                baseRelationElem.setAttribute("inverseRelationRangeMin",
                                              ODSHelper.relRange2string(inverseRelationRange.min));
                baseRelationElem.setAttribute("inverseRelationRangeMax",
                                              ODSHelper.relRange2string(inverseRelationRange.max));

                baseRelationElem.setAttribute("relationship",
                                              ODSHelper.relationship2string(baseRelation.getRelationship()));
                baseRelationElem.setAttribute("inverseRelationship",
                                              ODSHelper.relationship2string(baseRelation.getInverseRelationship()));

                baseRelationElem.setAttribute("relationType",
                                              ODSHelper.relationType2string(baseRelation.getRelationType()));

                elements.add(baseRelationElem);
            }
        }
        return elements.toArray(new Element[0]);
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static BaseStructureWriter getInstance() {
        if (instance == null) {
            instance = new BaseStructureWriter();
        }
        return instance;
    }

}
