package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.RelationType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;

public class AtfxReaderTest {
    
    private static OpenAtfxAPIImplementation api;
    private static int baseModelVersionNr;
    
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        URL url = AtfxReaderTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        Path atfxFile = Path.of(url.toURI());
        String fileRoot = Paths.get(url.toURI()).getParent().toString();
        IFileHandler fileHandler = new LocalFileHandler();
        AtfxReader reader = new AtfxReader(fileHandler, atfxFile, false, null);
        baseModelVersionNr = 31; // the current model version of the example.atfx file
        
        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = reader.readFile(xmlReader, Collections.emptyList());
        }
        assertThat(api).isNotNull();
        
        api.setContext(new NameValueUnit("FILE_ROOT", DataType.DT_STRING, fileRoot));
    }
    
    @Test
    void test_model_enumerations() {
        EnumerationDefinition axistypeEnumDef = api.getEnumerationDefinition("axistype");
        assertThat(axistypeEnumDef).isNotNull();
        assertThat(axistypeEnumDef.getItem("Xaxis")).isZero();
        assertThat(axistypeEnumDef.getItem("Yaxis")).isEqualTo(1);
        assertThat(axistypeEnumDef.getItem("Both")).isEqualTo(2);
        
        EnumerationDefinition coordTypesEnumDef = api.getEnumerationDefinition("coordinate_system_types");
        assertThat(coordTypesEnumDef).isNotNull();
        assertThat(coordTypesEnumDef.getItem("Cartesian")).isZero();
        assertThat(coordTypesEnumDef.getItem("Polar")).isEqualTo(1);
        assertThat(coordTypesEnumDef.getItem("Cylindric")).isEqualTo(2);
        
        EnumerationDefinition locModesEnumDef = api.getEnumerationDefinition("location_modes");
        assertThat(locModesEnumDef).isNotNull();
        assertThat(locModesEnumDef.getItem("Fixed")).isZero();
        assertThat(locModesEnumDef.getItem("Varying")).isEqualTo(1);
    }
    
    @Test
    void test_model_elements() {
        Collection<Element> elements = api.getElements();
        assertThat(elements).hasSize(33);
        
        Element environmentElement = api.getElementByName("env");
        assertThat(environmentElement.getId()).isPositive();
        assertThat(environmentElement.getType()).isEqualToIgnoringCase("AoEnvironment");
        
        Element meaQuantityElement = api.getElementByName("meq");
        assertThat(meaQuantityElement.getId()).isPositive();
        assertThat(meaQuantityElement.getType()).isEqualToIgnoringCase("AoMeasurementQuantity");
    }
    
    @Test
    void test_model_attributes() {
        Element extCompElement = api.getElementByName("ec");
        Collection<Attribute> attributes = extCompElement.getAttributes();
        
        Collection<String> expectedAttributeNames = new ArrayList<>();
        expectedAttributeNames.addAll(Arrays.asList("ec_iid", "iname", "description", "ordinal_number",
                                                    "component_length", "filename_url", "value_type", "start_offset",
                                                    "block_size", "valuesperblock", "value_offset",
                                                    "flags_filename_url", "flags_start_offset"));
        if (baseModelVersionNr > 30) {
            expectedAttributeNames.add("bitcount");
            expectedAttributeNames.add("bitoffset");
        }
        Collection<String> collectedAttributeNames = new HashSet<>();
        for (Attribute attribute : attributes) {
            collectedAttributeNames.add(attribute.getName());
            assertThat(attribute.getAid()).isEqualTo(extCompElement.getId());
            
            if ("ec_iid".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("id");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONGLONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isTrue();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isTrue();
            } else if ("iname".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("name");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(254);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("description".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("description");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(254);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("ordinal_number".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("ordinal_number");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("component_length".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("component_length");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("filename_url".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("filename_url");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(254);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("value_type".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("value_type");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_ENUM);
                assertThat(attribute.getEnumName()).isEqualTo("typespec_enum");
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("start_offset".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("start_offset");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG); // overwritten in atfx file to old DT_LONG type
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("block_size".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("block_size");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("valuesperblock".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("valuesperblock");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("value_offset".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("value_offset");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isTrue();
                assertThat(attribute.isUnique()).isFalse();
            }
            // the following attributes are implicitly added by openATFX if missing in file
            else if ("flags_filename_url".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("flags_filename_url");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(254);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("flags_start_offset".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("flags_start_offset");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_LONGLONG);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("bitcount".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("ao_bit_count");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_SHORT);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            } else if ("bitoffset".equals(attribute.getName())) {
                assertThat(attribute.getBaseName()).isEqualToIgnoringCase("ao_bit_offset");
                assertThat(attribute.getDataType()).isEqualTo(DataType.DT_SHORT);
                assertThat(attribute.getEnumName()).isNull();
                assertThat(attribute.getLength()).isEqualTo(1);
                assertThat(attribute.getUnitId()).isZero();
                assertThat(attribute.isAutogenerated()).isFalse();
                assertThat(attribute.isObligatory()).isFalse();
                assertThat(attribute.isUnique()).isFalse();
            }
        }
        assertThat(collectedAttributeNames).containsExactlyInAnyOrderElementsOf(expectedAttributeNames);
    }
    
    @Test
    void test_model_relations() {
        // ExternalComponent
        Element extCompElement = api.getElementByName("ec");
        Element lcElement = api.getElementByName("lc");
        Collection<Relation> ecRelations = extCompElement.getRelations();
        Collection<String> expectedEcRelationNames = new ArrayList<>();
        expectedEcRelationNames.addAll(Arrays.asList("lc_iid"));
        Collection<String> collectedEcRelationNames = new HashSet<>();
        for (Relation relation : ecRelations) {
            collectedEcRelationNames.add(relation.getRelationName());
            assertThat(relation.getElement1()).isEqualTo(extCompElement);
            
            if ("lc_iid".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation().getName()).isEqualToIgnoringCase("local_column");
                assertThat(relation.getInverseRelationName()).isEqualTo("ec_iid");
                assertThat(relation.getElement2()).isEqualTo(lcElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 1);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) 1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
            }
        }
        assertThat(collectedEcRelationNames).containsExactlyInAnyOrderElementsOf(expectedEcRelationNames);
        
        // SubMatrix
        Element matrixElement = api.getElementByName("sm");
        Element meaElement = api.getElementByName("dts");
        Element geometryElement = api.getElementByName("geometry");
        Collection<Relation> smRelations = matrixElement.getRelations();
        Collection<String> expectedSmRelationNames = new ArrayList<>();
        expectedSmRelationNames.addAll(Arrays.asList("lc_iid", "dts_iid", "x-axis-for-y-axis", "z-axis-for-y-axis",
                                                     "y-axis-for-x-axis", "y-axis-for-z-axis", "geometry"));
        Collection<String> collectedSmRelationNames = new HashSet<>();
        for (Relation relation : smRelations) {
            collectedSmRelationNames.add(relation.getRelationName());
            assertThat(relation.getElement1()).isEqualTo(matrixElement);
            
            if ("lc_iid".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation().getName()).isEqualToIgnoringCase("local_columns");
                assertThat(relation.getInverseRelationName()).isEqualTo("sm_iid");
                assertThat(relation.getElement2()).isEqualTo(lcElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) -1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.CHILD);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
            } else if ("dts_iid".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation().getName()).isEqualToIgnoringCase("measurement");
                assertThat(relation.getInverseRelationName()).isEqualTo("sm_iid");
                assertThat(relation.getElement2()).isEqualTo(meaElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 1);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) 1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
            } else if ("x-axis-for-y-axis".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("y-axis-for-x-axis");
                assertThat(relation.getElement2()).isEqualTo(matrixElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) 1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_TO);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            } else if ("z-axis-for-y-axis".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("y-axis-for-z-axis");
                assertThat(relation.getElement2()).isEqualTo(matrixElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) 1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_TO);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            } else if ("y-axis-for-x-axis".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("x-axis-for-y-axis");
                assertThat(relation.getElement2()).isEqualTo(matrixElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) -1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_FROM);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            } else if ("y-axis-for-z-axis".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("z-axis-for-y-axis");
                assertThat(relation.getElement2()).isEqualTo(matrixElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) -1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_FROM);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            } else if ("y-axis-for-z-axis".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("z-axis-for-y-axis");
                assertThat(relation.getElement2()).isEqualTo(matrixElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) -1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_REL);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            } else if ("geometry".equals(relation.getRelationName())) {
                assertThat(relation.getBaseRelation()).isNull();
                assertThat(relation.getInverseRelationName()).isEqualTo("geometry_shapes");
                assertThat(relation.getElement2()).isEqualTo(geometryElement);
                assertThat(relation.getRelationRangeMin()).isEqualTo((short) 0);
                assertThat(relation.getRelationRangeMax()).isEqualTo((short) 1);
                assertThat(relation.getRelationship()).isEqualTo(Relationship.INFO_TO);
                assertThat(relation.getRelationType()).isEqualTo(RelationType.INFO);
            }
        }
        assertThat(collectedSmRelationNames).containsExactlyInAnyOrderElementsOf(expectedSmRelationNames);
    }
    
    @Test
    void test_instance_elements() {
        Element lcElement = api.getElementByName("lc");
        Collection<Instance> lcInstances = api.getInstances(lcElement.getId());
        assertThat(lcInstances).hasSize(17);
        
        // explicit lc with inline values
        Instance timeLc = api.getInstanceById(lcElement.getId(), 107);
        assertThat(timeLc.getAid()).isEqualTo(lcElement.getId());
        assertThat(timeLc.getElement()).isEqualTo(lcElement);
        assertThat(timeLc.getName()).isEqualTo("Time");
        NameValueUnit seqRepNvu = timeLc.getValue("sequence_representation");
        assertThat(seqRepNvu.getUnit()).isEmpty();
        assertThat(seqRepNvu.getValue().getFlag()).isEqualTo((short) 15);
        assertThat(seqRepNvu.getValue().discriminator()).isEqualTo(DataType.DT_ENUM);
        assertThat(seqRepNvu.getValue().enumVal()).isZero();
        NameValueUnit globalFlagNvu = timeLc.getValueByBaseName("global_flag");
        assertThat(globalFlagNvu.getValue().shortVal()).isEqualTo((short) 15);
        NameValueUnit valuesNvu = timeLc.getValueByBaseName("values");
        double[] timeLcValues = valuesNvu.getValue().doubleSeq();
        assertThat(timeLcValues).hasSize(174)
                                .startsWith(0.0000000000000000e+00, 1.6615629196166992e-02, 3.2942056655883789e-02,
                                            4.9268722534179688e-02)
                                .endsWith(2.4183712005615234e+00, 2.4307441711425781e+00, 2.4431018829345703e+00,
                                          2.4623992443084717e+00);

        // explicit lc with component file (is converted to external component by openATFX)
        Instance voltageLc = api.getInstanceById(lcElement.getId(), 109);
        assertThat(voltageLc.getAid()).isEqualTo(lcElement.getId());
        assertThat(voltageLc.getElement()).isEqualTo(lcElement);
        assertThat(voltageLc.getName()).isEqualTo("Voltage.NF.Trigger 2");
        seqRepNvu = voltageLc.getValue("sequence_representation");
        assertThat(seqRepNvu.getUnit()).isEmpty();
        assertThat(seqRepNvu.getValue().getFlag()).isEqualTo((short) 15);
        assertThat(seqRepNvu.getValue().discriminator()).isEqualTo(DataType.DT_ENUM);
        assertThat(seqRepNvu.getValue().enumVal()).isEqualTo(7);
        globalFlagNvu = voltageLc.getValueByBaseName("global_flag");
        assertThat(globalFlagNvu.getValue().shortVal()).isEqualTo((short) 15);
        assertThat(voltageLc.getValueByBaseName("values")).isNotNull();
        Relation lcToEcRelation = api.getRelationByBaseName(lcElement.getId(), "external_component");
        List<Long> relatedEcIids = api.getRelatedInstanceIds(lcElement.getId(), voltageLc.getIid(), lcToEcRelation);
        assertThat(relatedEcIids).hasSize(1);
        
        // ec of previous lc
        Element ecElement = api.getElementByName("ec");
        Instance voltageEc = api.getInstanceById(ecElement.getId(), relatedEcIids.get(0));
        EnumerationDefinition typespecEnumDef = api.getEnumerationDefinition("typespec_enum");
        assertThat(voltageEc.getValue("filename_url").getValue().getFlag()).isEqualTo((short) 15);
        assertThat(Long.valueOf(voltageEc.getValue("value_type").getValue().enumVal())).isEqualTo(typespecEnumDef.getItem("ieeefloat4"));
        assertThat(voltageEc.getValue("ordinal_number").getValue().longVal()).isEqualTo(1);
        assertThat(voltageEc.getValue("start_offset").getValue().longVal()).isEqualTo(214504);
        assertThat(voltageEc.getValue("block_size").getValue().longVal()).isEqualTo(124);
        assertThat(voltageEc.getValue("description").getValue().stringVal()).isEqualTo("PAK native file");
        assertThat(voltageEc.getValue("component_length").getValue().longVal()).isEqualTo(174);
        assertThat(voltageEc.getValue("valuesperblock").getValue().longVal()).isEqualTo(1);
        assertThat(voltageEc.getValue("value_offset").getValue().longVal()).isZero();
        assertThat(voltageEc.getValue("lc_iid").getValue().longlongVal()).isEqualTo(voltageLc.getIid());
        
        // instance attributes
        Element projectElement = api.getElementByName("prj");
        Instance projectInstance = api.getInstanceById(projectElement.getId(), 1);
        assertThat(api.listInstanceAttributes(projectElement.getId(),
                                              projectInstance.getIid())).containsExactlyInAnyOrder("inst_attr_dt_string",
                                                                                                   "inst_attr_dt_string_empty",
                                                                                                   "inst_attr_dt_float",
                                                                                                   "inst_attr_dt_double",
                                                                                                   "inst_attr_dt_byte",
                                                                                                   "inst_attr_dt_short",
                                                                                                   "inst_attr_dt_long",
                                                                                                   "inst_attr_dt_longlong",
                                                                                                   "inst_attr_dt_date");
        assertThat(projectInstance.getValue("inst_attr_dt_string").getValue().stringVal()).isEqualTo("hello");
        assertThat(projectInstance.getValue("inst_attr_dt_string_empty").getValue().stringVal()).isEmpty();
        NameValueUnit floatValue = projectInstance.getValue("inst_attr_dt_float");
        assertThat(floatValue.getValue().floatVal()).isEqualTo(123.456f);
        assertThat(floatValue.getUnit()).isEqualTo("Pa");
        assertThat(projectInstance.getValue("inst_attr_dt_double").getValue().doubleVal()).isEqualTo(654.321d);
        assertThat(projectInstance.getValue("inst_attr_dt_byte").getValue().byteVal()).isEqualTo((byte) 10);
        assertThat(projectInstance.getValue("inst_attr_dt_short").getValue().shortVal()).isEqualTo((short) 123);
        NameValueUnit longValue = projectInstance.getValue("inst_attr_dt_long");
        assertThat(longValue.getValue().longVal()).isEqualTo(456);
        assertThat(longValue.getUnit()).isEqualTo("s");
        assertThat(projectInstance.getValue("inst_attr_dt_longlong").getValue().longlongVal()).isEqualTo(789L);
        assertThat(projectInstance.getValue("inst_attr_dt_date").getValue().dateVal()).isEqualTo("20100101130059");
    }
    
    @Test
    void testReadNumberValues_signedByte() {
        ExtCompReader reader = new ExtCompReader(api);
        List<Number> numbers = reader.readNumberValues(116, null);
        assertThat(numbers).containsExactly((byte) 1, (byte) 0, (byte) -1, (byte) 126, (byte) 127, (byte) -127,
                                            (byte) -128, (byte) 42, (byte) -13, (byte) -111);
    }
    
    @Test
    void testReadNumberValues_unsignedByte() {
        ExtCompReader reader = new ExtCompReader(api);
        List<Number> numbers = reader.readNumberValues(119, null);
        int[] widenedNumbers = numbers.stream().mapToInt(b -> b.intValue()).toArray();
        assertThat(widenedNumbers).containsExactly(1, 0, 127, 128, 129, 254, 255, 42, 13, 111);
    }
}
