package de.rechner.openatfx.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.AoSessionHelper;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.BaseAttribute;
import org.asam.ods.BaseElement;
import org.asam.ods.BaseRelation;
import org.asam.ods.BaseStructure;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.RelationRange;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import de.rechner.openatfx.AoSessionImpl;
import de.rechner.openatfx.IFileHandler;
import de.rechner.openatfx.basestructure.BaseStructureFactory;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Object for reading ATFX files.
 * 
 * @author Christian Rechner
 */
public class AtfxReader {

    private static final Log LOG = LogFactory.getLog(AtfxReader.class);

    /** The singleton instance */
    private static volatile AtfxReader instance;

    /** cached model information for faster parsing */
    private final Map<String, String> documentation;
    private final Map<String, String> files;
    private final Map<String, ApplicationElement> applElems;
    private final Map<String, Map<String, ApplicationAttribute>> applAttrs; // aeName, aaName, aa
    private final Map<String, Map<String, ApplicationRelation>> applRels; // aeName, relName, rel

    /**
     * Non visible constructor.
     */
    private AtfxReader() {
        this.documentation = new HashMap<String, String>();
        this.files = new HashMap<String, String>();
        this.applElems = new HashMap<String, ApplicationElement>();
        this.applAttrs = new HashMap<String, Map<String, ApplicationAttribute>>();
        this.applRels = new HashMap<String, Map<String, ApplicationRelation>>();
    }

    /**
     * Returns the ASAM ODS aoSession object for a ATFX file.
     * 
     * @param orb The ORB.
     * @param in The ATFX file input stream.
     * @return The aoSession object.
     * @throws AoException Error getting aoSession.
     */
    public synchronized AoSession createSessionForATFX(ORB orb, IFileHandler fileHandler, String path)
            throws AoException {
        long start = System.currentTimeMillis();
        InputStream in = null;
        try {
            // get stream from file handler
            in = fileHandler.getFileStream(path);

            // open XML file
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader reader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());

            String baseModelVersion = "";
            AoSession aoSession = null;
            while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.ATFX_FILE))) {

                // parse 'documentation'
                if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION)) {
                    documentation.putAll(parseDocumentation(reader));
                }
                // parse 'base_model_version'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.BASE_MODEL_VERSION)) {
                    baseModelVersion = reader.getElementText();
                }
                // parse 'files'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.FILES)) {
                    files.putAll(parseFiles(reader));
                }
                // parse 'application_model'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_MODEL)) {
                    parseApplicationModel(aoSession.getApplicationStructure(), reader);
                }
                // parse 'instance_data'
                else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.INSTANCE_DATA)) {
                    // parseInstanceElements(aoSession, reader);
                    AtfxInstanceReader.getInstance().parseInstanceElements(aoSession, files, reader);
                }

                // create AoSession object and write documentation to context
                if ((baseModelVersion.length() > 0) && (aoSession == null)) {
                    BaseStructure bs = BaseStructureFactory.getInstance().getBaseStructure(orb, baseModelVersion);
                    POA modelPOA = createModelPOA(orb);
                    AoSessionImpl aoSessionImpl = new AoSessionImpl(modelPOA, fileHandler, path, bs);
                    modelPOA.activate_object(aoSessionImpl);
                    aoSession = AoSessionHelper.narrow(modelPOA.servant_to_reference(aoSessionImpl));
                }

                reader.nextTag();
            }

            // set context
            for (String docKey : documentation.keySet()) {
                aoSession.setContextString("documentation_" + docKey, documentation.get(docKey));
            }

            LOG.info("Read ATFX in " + (System.currentTimeMillis() - start) + "ms");
            return aoSession;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (XMLStreamException e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantAlreadyActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (WrongPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (ServantNotActive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            this.documentation.clear();
            this.files.clear();
            this.applElems.clear();
            this.applAttrs.clear();
            this.applRels.clear();
        }
    }

    /**
     * Creates a new POA for all elements of the application structure for the session.
     * 
     * @param orb The ORB object.
     * @return The POA.
     * @throws AoException Error creating POA.
     */
    private POA createModelPOA(ORB orb) throws AoException {
        try {
            String poaName = "AoSession.ModelPOA." + UUID.randomUUID().toString();
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POA poa = rootPOA.create_POA(poaName,
                                         null,
                                         new Policy[] {
                                                 rootPOA.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID),
                                                 rootPOA.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
                                                 rootPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.UNIQUE_ID),
                                                 rootPOA.create_implicit_activation_policy(ImplicitActivationPolicyValue.IMPLICIT_ACTIVATION),
                                                 rootPOA.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
                                                 rootPOA.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
                                                 rootPOA.create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL) });
            poa.the_POAManager().activate();
            LOG.debug("Created session POA");
            return poa;
        } catch (InvalidName e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterAlreadyExists e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (InvalidPolicy e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        } catch (AdapterInactive e) {
            LOG.error(e.getMessage(), e);
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
        }
    }

    /**
     * Parse the 'documentation' part of the ATFX file.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the documentation.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseDocumentation(XMLStreamReader reader) throws XMLStreamException {
        reader.nextTag();
        Map<String, String> map = new HashMap<String, String>();
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.DOCUMENTATION))) {
            if (reader.isStartElement()) {
                map.put(reader.getLocalName(), reader.getElementText());
            }
            reader.nextTag();
        }
        return map;
    }

    /***************************************************************************************
     * methods for parsing the component files declaration
     ***************************************************************************************/

    /**
     * Parse the 'files' part of the ATFX file.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the component files.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseFiles(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> map = new HashMap<String, String>();
        // 'files'
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.FILES))) {
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT)) {
                map.putAll(parseComponent(reader));
            }
            reader.next();
        }
        return map;
    }

    /**
     * Parse one 'component' part.
     * 
     * @param reader The XML stream reader.
     * @return Map containing the key value pairs of the component files.
     * @throws XMLStreamException Error parsing XML.
     */
    private Map<String, String> parseComponent(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> map = new HashMap<String, String>();
        String identifier = "";
        String filename = "";
        // 'component'
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT))) {
            // 'identifier'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT_IDENTIFIER)) {
                identifier = reader.getElementText();
            }
            // 'filename'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.COMPONENT_FILENAME)) {
                filename = reader.getElementText();
            }
            reader.next();
        }
        map.put(identifier, filename);
        return map;
    }

    /***************************************************************************************
     * methods for parsing the application model
     ***************************************************************************************/

    /**
     * Parse the application model.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing application structure.
     */
    private void parseApplicationModel(ApplicationStructure as, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        long start = System.currentTimeMillis();

        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_MODEL))) {
            // 'application_enumeration'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM)) {
                parseEnumerationDefinition(as, reader);
            }
            // 'application_element'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM)) {
                parseApplicationElement(as, reader);
            }
            reader.next();
        }

        // implicit create application element of "AoExternalComponent" if missing
        ApplicationElement[] aes = as.getElementsByBaseType("AoExternalComponent");
        ApplicationElement aeExtComp = null;
        if (aes.length == 1) {
            aeExtComp = aes[0];
        } else if (aes.length < 1) {
            aeExtComp = implicitCreateAoExtComp(as);
        } else if (aes.length > 1) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Multiple application elements of type 'AoExternalComponent' found");
        }
        // implicit create flag file attributes if missing
        if (aeExtComp != null) {
            implicitCreateAoExtCompOptAttrs(aeExtComp);
        }

        // fill map with application relations and sets the default relation range
        for (ApplicationElement ae : as.getElements("*")) {
            for (ApplicationRelation rel : ae.getAllRelations()) {
                // setting default relation range
                RelationRange relRange = rel.getRelationRange();
                if (relRange.min == -2 && relRange.max == -2) {
                    rel.setRelationRange(new RelationRange((short) 0, (short) -1));
                    LOG.warn("Setting default relation range [0,-1] to relation: " + rel.getRelationName());
                }
                this.applRels.get(ae.getName()).put(rel.getRelationName(), rel);

                // correct base relations in case of multiple possible target base elements
                BaseRelation baseRelation = rel.getBaseRelation();
                ApplicationElement elem1 = rel.getElem1();
                ApplicationElement elem2 = rel.getElem2();

                // check if reference is complete
                if (elem1 == null || elem2 == null) {
                    fixIncompleteReleation(as, baseRelation, rel);
                    elem1 = rel.getElem1();
                    elem2 = rel.getElem2();
                }
                if ((baseRelation != null) && (elem1 != null) && (elem2 != null)) {
                    String relType = elem2.getBaseElement().getType();
                    String bTypeElem2 = baseRelation.getElem2().getType();
                    if (!relType.equals(bTypeElem2)) {
                        rel.setBaseRelation(lookupBaseRelation(elem1, elem2, baseRelation.getRelationName(), relType));
                    }
                }

            }
        }

        LOG.info("Parsed application model in " + (System.currentTimeMillis() - start) + " ms");
    }

    private static BaseRelation lookupBaseRelation(ApplicationElement elem1, ApplicationElement elem2, String bRelName,
            String bType) throws AoException {
        for (BaseRelation baseRel : elem1.getBaseElement().getAllRelations()) {
            if (baseRel.getRelationName().equals(bRelName) && baseRel.getElem2().getType().equals(bType)) {
                return baseRel;
            }
        }
        throw new AoException(ErrorCode.AO_INVALID_RELATION, SeverityFlag.ERROR, 0, "BaseRelation not found for name='"
                + bRelName + "',targetBaseType='" + bType + "'");
    }

    /**
     * Tries to fix incomplete relations. The base relation is required for getting the needed information.
     * 
     * @param as The application structure.
     * @param base The base relation containing missing information.
     * @param rel The relation that shall be fixed.
     * @throws AoException Raised if an error occurs, or a fix is not possible.
     */
    private void fixIncompleteReleation(ApplicationStructure as, BaseRelation base, ApplicationRelation rel)
            throws AoException {
        ApplicationElement appelem = rel.getElem1();
        if (appelem == null && base != null) {
            BaseElement be = base.getElem1();
            ApplicationElement[] aes = as.getElementsByBaseType(be.getType());
            if (aes.length == 1) {
                rel.setElem1(aes[0]);
                appelem = rel.getElem1();
                LOG.info("Automatically fixed reverse relation for " + appelem.getName());
            } else {
                String message = "ATFX File is missing required inverse relations. "
                        + "Automatic correction is not possible.";
                LOG.error(message);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, message);
            }
        }
        appelem = rel.getElem2();
        if (appelem == null && base != null) {
            BaseElement be = base.getElem2();
            ApplicationElement[] aes = as.getElementsByBaseType(be.getType());
            if (aes.length == 1) {
                rel.setElem2(aes[0]);
                appelem = rel.getElem2();
                LOG.info("Automatically fixed reverse relation for " + appelem.getName());
            } else {
                String message = "ATFX File is missing required inverse relations. "
                        + "Automatic correction is not possible.";
                LOG.error(message);
                throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, message);
            }
        }
    }

    /**
     * Creates implicitly an application element of type "AoExternalComponent" including all relations.
     * 
     * @param as The application structure.
     * @return The created application element.
     * @throws AoException Error creating application element.
     */
    private ApplicationElement implicitCreateAoExtComp(ApplicationStructure as) throws AoException {
        ApplicationElement[] aeLCs = as.getElementsByBaseType("AoLocalColumn");
        if (aeLCs.length < 1) {
            return null;
        }
        ApplicationElement aeLC = aeLCs[0];

        LOG.info("No application element of type 'AoExternalComponent' found, creating dummy");

        // create application element, this includes all mandatory attributes
        BaseStructure bs = as.getSession().getBaseStructure();
        BaseElement beExtComp = bs.getElementByType("AoExternalComponent");
        ApplicationElement aeExtComp = as.createElement(beExtComp);
        aeExtComp.setName("ec");

        // create relation to LocalColumn
        ApplicationRelation rel = as.createRelation();
        rel.setElem1(aeLC);
        rel.setElem2(aeExtComp);
        rel.setBaseRelation(bs.getRelation(aeLC.getBaseElement(), beExtComp));
        rel.setRelationName("rel_lc");
        rel.setInverseRelationName("rel_ec");

        this.applRels.put("ec", new HashMap<String, ApplicationRelation>());
        return aeExtComp;
    }

    /**
     * Creates implicitly all non mandatory application attributes of type "AoExternalComponent".
     * 
     * @param as The application structure.
     * @return The created application element.
     * @throws AoException Error creating application element.
     */
    private void implicitCreateAoExtCompOptAttrs(ApplicationElement aeExtComp) throws AoException {

        // fetch existing attributes
        Set<String> existingBaNames = new HashSet<String>();
        for (ApplicationAttribute aa : aeExtComp.getAttributes("*")) {
            BaseAttribute ba = aa.getBaseAttribute();
            if (ba != null) {
                existingBaNames.add(ba.getName());
            }
        }

        String baseModelVersion = aeExtComp.getApplicationStructure().getSession().getBaseStructure().getVersion();
        int baseModelVersioNo = Integer.parseInt(baseModelVersion.replace("asam", ""));

        // flags_filename_url
        if (!existingBaNames.contains("flags_filename_url")) {
            ApplicationAttribute aa = aeExtComp.createAttribute();
            aa.setName("flags_filename_url");
            aa.setBaseAttribute(aeExtComp.getBaseElement().getAttributes("flags_filename_url")[0]);
        }
        // flags_start_offset
        if (!existingBaNames.contains("flags_start_offset")) {
            ApplicationAttribute aa = aeExtComp.createAttribute();
            aa.setName("flags_start_offset");
            aa.setBaseAttribute(aeExtComp.getBaseElement().getAttributes("flags_start_offset")[0]);
        }
        // ao_bit_count, only for asam31 models
        if ((baseModelVersioNo > 30) && !existingBaNames.contains("ao_bit_count")) {
            ApplicationAttribute aa = aeExtComp.createAttribute();
            aa.setName("bitcount");
            aa.setBaseAttribute(aeExtComp.getBaseElement().getAttributes("ao_bit_count")[0]);
        }
        // ao_bit_offset, only for asam31 models
        if ((baseModelVersioNo > 30) && !existingBaNames.contains("ao_bit_offset")) {
            ApplicationAttribute aa = aeExtComp.createAttribute();
            aa.setName("bitoffset");
            aa.setBaseAttribute(aeExtComp.getBaseElement().getAttributes("ao_bit_offset")[0]);
        }
    }

    /**
     * Parse an enumeration definition.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing application structure.
     */
    private void parseEnumerationDefinition(ApplicationStructure as, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, "Expected enumeration name");
        }
        EnumerationDefinition enumDef = as.createEnumerationDefinition(reader.getElementText());
        // items
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM))) {
            // 'item'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM)) {
                parseEnumerationItem(enumDef, reader);
            }
            reader.next();
        }
    }

    /**
     * Parse an enumeration item.
     * 
     * @param enumDef The enumeration definition.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to enumeration definition.
     */
    private void parseEnumerationItem(EnumerationDefinition enumDef, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_ITEM))) {
            // 'name'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ENUM_NAME)) {
                enumDef.addItem(reader.getElementText());
            }
            reader.next();
        }
    }

    /**
     * Parse an application element.
     * 
     * @param as The application structure.
     * @param reader The XML stream reader.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private void parseApplicationElement(ApplicationStructure as, XMLStreamReader reader) throws XMLStreamException,
            AoException {
        // 'name'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_NAME)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Expected application element 'name'");
        }
        String aeName = reader.getElementText();
        // 'basetype'
        reader.nextTag();
        if (!reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM_BASETYPE)) {
            throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0,
                                  "Expected application element 'basetype'");
        }
        String basetype = reader.getElementText();

        // create application element
        BaseElement be = as.getSession().getBaseStructure().getElementByType(basetype);
        ApplicationElement applElem = as.createElement(be);
        applElem.setName(aeName);

        // cache base attributes and base relations
        Map<String, BaseAttribute> baseAttrMap = new HashMap<String, BaseAttribute>();
        Map<String, BaseRelation> baseRelMap = new HashMap<String, BaseRelation>();
        for (BaseAttribute baseAttr : applElem.getBaseElement().getAttributes("*")) {
            baseAttrMap.put(baseAttr.getName().toLowerCase(), baseAttr);
        }
        for (BaseRelation baseRel : applElem.getBaseElement().getAllRelations()) {
            baseRelMap.put(baseRel.getRelationName().toLowerCase(), baseRel);
        }

        // add to global map
        this.applElems.put(aeName, applElem);
        this.applAttrs.put(aeName, new HashMap<String, ApplicationAttribute>());
        this.applRels.put(aeName, new HashMap<String, ApplicationRelation>());

        // attributes and relations
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ELEM))) {
            // 'application_attribute'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR)) {
                parseApplicationAttribute(applElem, reader, baseAttrMap);
            }
            // 'relation_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL)) {
                parseApplicationRelation(applElem, reader, baseRelMap);
            }
            reader.next();
        }
    }

    /**
     * Parse an application attribute.
     * 
     * @param applElem The application element.
     * @param reader The XML stream reader.
     * @param baseAttrMap Map containing all base attributes of the application element.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private void parseApplicationAttribute(ApplicationElement applElem, XMLStreamReader reader,
            Map<String, BaseAttribute> baseAttrMap) throws XMLStreamException, AoException {
        String aaNameStr = "";
        String baseAttrStr = "";
        String dataTypeStr = "";
        String lengthStr = "";
        String obligatoryStr = "";
        String uniqueStr = "";
        String autogeneratedStr = "";
        String enumtypeStr = "";
        String unitStr = "";
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR))) {
            // 'name'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_NAME)) {
                aaNameStr = reader.getElementText();
            }
            // 'base_attribute'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_BASEATTR)) {
                baseAttrStr = reader.getElementText();
            }
            // 'datatype'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_DATATYPE)) {
                dataTypeStr = reader.getElementText();
            }
            // 'length'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_LENGTH)) {
                lengthStr = reader.getElementText();
            }
            // 'obligatory'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_OBLIGATORY)) {
                obligatoryStr = reader.getElementText();
            }
            // 'unique'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIQUE)) {
                uniqueStr = reader.getElementText();
            }
            // 'autogenerated'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_AUTOGENERATE)) {
                autogeneratedStr = reader.getElementText();
            }
            // 'enumeration_type'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_ENUMTYPE)) {
                enumtypeStr = reader.getElementText();
            }
            // 'unit'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_ATTR_UNIT)) {
                unitStr = reader.getElementText();
            }
            reader.next();
        }

        // check if base attribute already exists (obligatory base attributes are generated automatically)
        ApplicationAttribute aa = null;
        BaseAttribute baseAttr = null;
        if (baseAttrStr != null && baseAttrStr.length() > 0) {
            baseAttr = baseAttrMap.get(baseAttrStr);
            if (baseAttr == null) {
                throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Base attribute '" + baseAttrStr
                        + "' not found");
            }
            if (baseAttr.isObligatory()) {
                aa = applElem.getAttributeByBaseName(baseAttrStr);
            }
        }
        if (aa == null) {
            aa = applElem.createAttribute();
        }
        aa.setName(aaNameStr);

        // base attribute?
        if (baseAttr != null) {
            aa.setBaseAttribute(baseAttr);
        }
        // datatype & obligatory
        if (dataTypeStr != null && dataTypeStr.length() > 0) {
            DataType datatype = ODSHelper.string2dataType(dataTypeStr);
            aa.setDataType(datatype);
        }
        // obligatory
        if (obligatoryStr != null && obligatoryStr.length() > 0) {
            aa.setIsObligatory(Boolean.valueOf(obligatoryStr));
        }
        // length
        if (lengthStr != null && lengthStr.length() > 0) {
            aa.setLength(AtfxParseUtil.parseLong(lengthStr));
        }
        // unique (set is only allowed on non base attrs)
        if ((uniqueStr != null) && (uniqueStr.length() > 0) && (baseAttr == null)) {
            aa.setIsUnique(AtfxParseUtil.parseBoolean(uniqueStr));
        }
        // autogenerated
        if (autogeneratedStr != null && autogeneratedStr.length() > 0) {
            aa.setIsAutogenerated(AtfxParseUtil.parseBoolean(autogeneratedStr));
        }
        // enumeration
        if (enumtypeStr != null && enumtypeStr.length() > 0) {
            EnumerationDefinition enumDef = applElem.getApplicationStructure().getEnumerationDefinition(enumtypeStr);
            aa.setEnumerationDefinition(enumDef);
        }
        // unit
        if (unitStr != null && unitStr.length() > 0) {
            aa.setUnit(AtfxParseUtil.parseLongLong(unitStr));
        }

        // add to global map
        this.applAttrs.get(applElem.getName()).put(aaNameStr, aa);
    }

    /**
     * Parse an application relation.
     * 
     * @param applElem The application element
     * @param reader The XML stream reader.
     * @param baseRelMap Map containing all base relations of the application element.
     * @throws XMLStreamException Error parsing XML.
     * @throws AoException Error writing to application model.
     */
    private void parseApplicationRelation(ApplicationElement applElem, XMLStreamReader reader,
            Map<String, BaseRelation> baseRelMap) throws XMLStreamException, AoException {
        String elem2Name = "";
        String relName = "";
        String inverseRelName = "";
        String brName = "";
        String minStr = "";
        String maxStr = "";
        while (!(reader.isEndElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL))) {
            // 'ref_to'
            if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_REFTO)) {
                elem2Name = reader.getElementText();
            }
            // 'name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_NAME)) {
                relName = reader.getElementText();
            }
            // 'inverse_name'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_INVNAME)) {
                inverseRelName = reader.getElementText();
            }
            // 'base_relation'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_BASEREL)) {
                brName = reader.getElementText();
            }
            // 'min_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MIN)) {
                minStr = reader.getElementText();
            }
            // 'max_occurs'
            else if (reader.isStartElement() && reader.getLocalName().equals(AtfxTagConstants.APPL_REL_MAX)) {
                maxStr = reader.getElementText();
            }
            reader.next();
        }

        // check if relation has already been defined by the created inverse relation
        ApplicationRelation rel = getApplRel(elem2Name, inverseRelName);

        // NEW
        if (rel == null) {
            rel = applElem.getApplicationStructure().createRelation();
            // relation names
            rel.setRelationName(relName);
            rel.setInverseRelationName(inverseRelName);
            // elems
            rel.setElem1(applElem);
            ApplicationElement elem2 = getApplElem(elem2Name);
            if (elem2 != null) {
                rel.setElem2(elem2);
            }
            // base relation
            if (brName != null && brName.length() > 0) {
                BaseRelation baseRel = baseRelMap.get(brName.toLowerCase());
                if (baseRel == null) {
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "BaseRelation '" + brName
                            + "' not found'");
                }
                rel.setBaseRelation(baseRel);
            }
            // relation range
            if (minStr.length() > 0 && maxStr.length() > 0) {
                RelationRange relRange = new RelationRange();
                relRange.min = ODSHelper.string2relRange(minStr);
                relRange.max = ODSHelper.string2relRange(maxStr);
                rel.setRelationRange(relRange);
            }
            this.applRels.get(applElem.getName()).put(relName, rel);
        }

        // EXISTING
        else {
            // set elem2 (may not exist because application element may have not yet been living)
            rel.setElem2(applElem);
            // relation names
            rel.setInverseRelationName(relName);
            rel.setRelationName(inverseRelName);
            // relation range
            if (minStr.length() > 0 && maxStr.length() > 0) {
                RelationRange relRange = new RelationRange();
                relRange.min = ODSHelper.string2relRange(minStr);
                relRange.max = ODSHelper.string2relRange(maxStr);
                rel.setInverseRelationRange(relRange);
            }
        }
    }

    /***************************************************************************************
     * methods for parsing instance elements
     ***************************************************************************************/

    /**
     * Returns the application element object for given application element name.
     * 
     * @param aeName The application element name.
     * @return The application element, null if not found.
     */
    private ApplicationElement getApplElem(String aeName) {
        return this.applElems.get(aeName);
    }

    /**
     * Returns the application relation object for given application element and relation name.
     * 
     * @param aeName The application element name.
     * @param name The relation name.
     * @return The application relation, null if not attribute found.
     */
    private ApplicationRelation getApplRel(String aeName, String name) {
        Map<String, ApplicationRelation> relMap = this.applRels.get(aeName);
        if (relMap != null) {
            return relMap.get(name);
        }
        return null;
    }

    /***************************************************************************************
     * methods for parsing attribute values
     ***************************************************************************************/

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static AtfxReader getInstance() {
        if (instance == null) {
            instance = new AtfxReader();
        }
        return instance;
    }

    /**
     * Custom Stax filter for only collect start and end elements.
     */
    private static class StartEndElementFilter implements StreamFilter {

        public boolean accept(XMLStreamReader myReader) {
            if (myReader.isStartElement() || myReader.isEndElement()) {
                return true;
            } else {
                return false;
            }
        }
    }

}
