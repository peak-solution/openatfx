package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.RelationType;
import org.asam.ods.SetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.peaksolution.openatfx.LocalFileHandler;
import com.peaksolution.openatfx.OpenAtfx;

class AtfxWriterTest {
    
    static Stream<Arguments> baseModelVersions() {
        return Stream.of(
            Arguments.of(29),
            Arguments.of(30),
            Arguments.of(31),
            Arguments.of(32),
            Arguments.of(33),
            Arguments.of(34),
            Arguments.of(35),
            Arguments.of(36)
        );
      }
    
    @ParameterizedTest
    @MethodSource("baseModelVersions")
    void testAtfxWriter(int baseModelVersion, @TempDir Path tempDir) throws IOException, XMLStreamException {
        OpenAtfx openAtfx = new OpenAtfx();
        Path atfxFile = Paths.get(tempDir.toString(), "unittest_asam" + baseModelVersion + ".atfx");
        OpenAtfxAPI api = openAtfx.createNewFile(atfxFile, baseModelVersion);
        
        // create enumeration
        api.createEnumeration("testEnum");
        api.addEnumerationItem("testEnum", 1, "value1");
        api.addEnumerationItem("testEnum", 2, "value2");
        
        // create PhysicalDimension element
        Element dimElement = api.createElement("AoPhysicalDimension", "PhysDim");
        
        // create Unit element
        Element unitElement = api.createElement("aoUnit", "Unit");
        api.createRelationFromBaseRelation(unitElement.getId(), dimElement.getId(), "phys_dimension", "dim", "units");
        
        // create Role element
        Element ugElement = api.createElement("aousergroup", "Role");
        // create atributes
        api.createAttribute(ugElement.getId(), "customAttr1", null, DataType.DT_ENUM, 1, 0L, "testEnum", false, false, false);
        api.createAttribute(ugElement.getId(), "customAttr2", null, DataType.DT_STRING, 250, 0L, null, true, true, false);
        if (baseModelVersion >= 31) {
            api.createAttributeFromBaseAttribute(ugElement.getId(), "date_created", "ao_created");
        }
        
        // create User element
        Element userElement = api.createElement("AoUser", "User");
        // create relations
        api.createRelationFromBaseRelation(userElement.getId(), ugElement.getId(), "groups", "u2g", "g2u");
        AtfxWriter.getInstance().writeXML(atfxFile.toFile(), (OpenAtfxAPIImplementation) api);
        try (InputStream in = new BufferedInputStream(new FileInputStream(atfxFile.toFile()))) {
            AtfxReader atfxReader = new AtfxReader(new LocalFileHandler(), atfxFile, false, null);
            // open XML file
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader reader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            api = atfxReader.readFile(reader, Collections.emptyList());
        }
        
        // create instances
        
        // PhysDim instance
        Collection<NameValueUnit> dimAttrs = new ArrayList<>();
        dimAttrs.add(new NameValueUnit("name", DataType.DT_STRING, "Time"));
        dimAttrs.add(new NameValueUnit("time_exp", DataType.DT_LONG, 1));
        Instance dimTime = api.createInstance(dimElement.getId(), dimAttrs);
        
        // Unit instance
        Collection<NameValueUnit> unitAttrs = new ArrayList<>();
        unitAttrs.add(new NameValueUnit("name", DataType.DT_STRING, "s"));
        unitAttrs.add(new NameValueUnit("factor", DataType.DT_DOUBLE, 1d));
        unitAttrs.add(new NameValueUnit("offset", DataType.DT_DOUBLE, 0d));
        Instance unitSeconds = api.createInstance(unitElement.getId(), unitAttrs);
        api.setRelatedInstances(unitElement.getId(), unitSeconds.getIid(), "dim", Arrays.asList(dimTime.getIid()), SetType.INSERT);
        
        // extend User element with custom attribute with unit
        api.createAttribute(userElement.getId(), "attrWithUnit", null, DataType.DT_LONGLONG, 1, "s", null, false, false, false);
        // User instance
        Collection<NameValueUnit> userAttrs = new ArrayList<>();
        userAttrs.add(new NameValueUnit("attrWithUnit", DataType.DT_LONGLONG, 42L, "s"));
        Instance user = api.createInstance(userElement.getId(), userAttrs);
        
        assertThat(api.getContext()).hasSize(4);
        
        // test element
        Element returnedUgElement = api.getElementByName("Role");
        assertThat(returnedUgElement.getType()).isEqualToIgnoringCase("AoUsergroup");
        Collection<Attribute> ugAttributes = returnedUgElement.getAttributes();
        assertThat(ugAttributes).hasSize(baseModelVersion >= 31 ? 6 : 5);
        
        // test attributes
        Collection<String> foundAttributeNames = new HashSet<>();
        for (Attribute currentAttr : ugAttributes) {
            foundAttributeNames.add(currentAttr.getName());
            if ("customAttr1".equals(currentAttr.getName())) {
                assertThat(currentAttr).hasFieldOrPropertyWithValue("baseName", null)
                                       .hasFieldOrPropertyWithValue("unitId", 0L)
                                       .hasFieldOrPropertyWithValue("enumName", "testEnum")
                                       .hasFieldOrPropertyWithValue("dataType", DataType.DT_ENUM)
                                       .hasFieldOrPropertyWithValue("length", 1)
                                       .hasFieldOrPropertyWithValue("obligatory", false)
                                       .hasFieldOrPropertyWithValue("unique", false)
                                       .hasFieldOrPropertyWithValue("autogenerated", false);
            } else if ("customAttr2".equals(currentAttr.getName())) {
                assertThat(currentAttr).hasFieldOrPropertyWithValue("baseName", null)
                                       .hasFieldOrPropertyWithValue("unitId", 0L)
                                       .hasFieldOrPropertyWithValue("enumName", null)
                                       .hasFieldOrPropertyWithValue("dataType", DataType.DT_STRING)
                                       .hasFieldOrPropertyWithValue("length", 250)
                                       .hasFieldOrPropertyWithValue("obligatory", true)
                                       .hasFieldOrPropertyWithValue("unique", true)
                                       .hasFieldOrPropertyWithValue("autogenerated", false);
            } else if ("date_created".equals(currentAttr.getName())) {
                assertThat(currentAttr).hasFieldOrPropertyWithValue("baseName", "ao_created")
                                       .hasFieldOrPropertyWithValue("unitId", 0L)
                                       .hasFieldOrPropertyWithValue("enumName", null)
                                       .hasFieldOrPropertyWithValue("dataType", DataType.DT_DATE)
                                       .hasFieldOrPropertyWithValue("length", 30)
                                       .hasFieldOrPropertyWithValue("obligatory", false)
                                       .hasFieldOrPropertyWithValue("unique", false)
                                       .hasFieldOrPropertyWithValue("autogenerated", true);
            }
        }
        Collection<String> expectedAttributeNames = new HashSet<>();
        expectedAttributeNames.addAll(Arrays.asList("id", "name", "superuser_flag", "customAttr1", "customAttr2"));
        if (baseModelVersion >= 31) {
            expectedAttributeNames.add("date_created");
        }
        assertThat(foundAttributeNames).containsExactlyInAnyOrderElementsOf(expectedAttributeNames);
        
        // test relations
        Element returnedUserElement = api.getUniqueElementByBaseType("aouser");
        assertThat(returnedUserElement.getName()).isEqualTo("User");
        Collection<Relation> userRelations = returnedUserElement.getRelations();
        assertThat(userRelations).hasSize(1);
        Collection<String> collectedUserRelationNames = new HashSet<>();
        for (Relation currentRelation : userRelations) {
            collectedUserRelationNames.add(currentRelation.getRelationName());
            if ("u2g".equals(currentRelation.getRelationName())) {
                assertThat(currentRelation.getBaseRelation().getName()).isEqualToIgnoringCase("groups");
                assertThat(currentRelation.getElement1()).isEqualTo(userElement);
                assertThat(currentRelation.getElement2()).isEqualTo(ugElement);
                assertThat(currentRelation.getInverseRelation()).isNotNull();
                assertThat(currentRelation.getInverseRelationName()).isEqualTo("g2u");
                assertThat(currentRelation.getRelationRangeMax()).isEqualTo((short)-1);
                assertThat(currentRelation.getRelationRangeMin()).isEqualTo((short)0);
                if (baseModelVersion < 33) {
                    assertThat(currentRelation.getRelationship()).isEqualTo(Relationship.INFO_TO);
                } else {
                    assertThat(currentRelation.getRelationship()).isEqualTo(Relationship.INFO_REL);
                }
                assertThat(currentRelation.getRelationType()).isEqualTo(RelationType.INFO);
            }
        }
        Collection<String> expectedUserRelationNames = Arrays.asList("u2g");
        assertThat(collectedUserRelationNames).containsExactlyInAnyOrderElementsOf(expectedUserRelationNames);
        
        Collection<Relation> groupRelations = returnedUgElement.getRelations();
        assertThat(groupRelations).hasSize(1);
        Collection<String> collectedGroupRelationNames = new HashSet<>();
        assertThat(groupRelations).hasSize(1);
        for (Relation currentRelation : groupRelations) {
            collectedGroupRelationNames.add(currentRelation.getRelationName());
            if ("g2u".equals(currentRelation.getRelationName())) {
                assertThat(currentRelation.getBaseRelation().getName()).isEqualToIgnoringCase("users");
                assertThat(currentRelation.getElement1()).isEqualTo(ugElement);
                assertThat(currentRelation.getElement2()).isEqualTo(userElement);
                assertThat(currentRelation.getInverseRelation()).isNotNull();
                assertThat(currentRelation.getInverseRelationName()).isEqualTo("u2g");
                assertThat(currentRelation.getRelationRangeMax()).isEqualTo((short)-1);
                assertThat(currentRelation.getRelationRangeMin()).isEqualTo((short)0);
                if (baseModelVersion < 33) {
                    assertThat(currentRelation.getRelationship()).isEqualTo(Relationship.INFO_FROM);
                } else {
                    assertThat(currentRelation.getRelationship()).isEqualTo(Relationship.INFO_REL);
                }
                assertThat(currentRelation.getRelationType()).isEqualTo(RelationType.INFO);
            }
        }
        Collection<String> expectedGroupRelationNames = Arrays.asList("g2u");
        assertThat(collectedGroupRelationNames).containsExactlyInAnyOrderElementsOf(expectedGroupRelationNames);
        
        // test instances
        Instance dimTimeInstance = api.getInstanceById(dimElement.getId(), dimTime.getIid());
        assertThat(dimTimeInstance.getName()).isEqualTo(dimTime.getName());
        assertThat(dimTimeInstance.getValueByBaseName("time_exp").getValue().longVal()).isEqualTo(1);
        Instance unitSecondsInstance = api.getInstanceById(unitElement.getId(), unitSeconds.getIid());
        assertThat(unitSecondsInstance.getElementName()).isEqualTo(unitElement.getName());
        Instance userInstance = api.getInstanceById(userElement.getId(), user.getIid());
        NameValueUnit userAttrWithUnitValue = userInstance.getValue("attrWithUnit");
        assertThat(userAttrWithUnitValue.getValue().longlongVal()).isEqualTo(42);
        assertThat(userAttrWithUnitValue.getUnit()).isEqualTo("s");
    }
    
    @Test
    void testInstanceUpdates(@TempDir Path tempDir) {
        OpenAtfx openAtfx = new OpenAtfx();
        Path atfxFile = Paths.get(tempDir.toString(), "unittest_asam" + 36 + ".atfx");
        OpenAtfxAPI api = openAtfx.createNewFile(atfxFile, 36);
        
        // create test instance
        Element lcElement = api.createElement("AoLocalColumn", "LocalColumn");
        Attribute gfAttribute = api.createAttribute(lcElement.getId(), "GlobalFlag", "global_flag", DataType.DT_SHORT, 1, 0, null, false, false, false);
        Collection<NameValueUnit> lcValues = new ArrayList<>();
        NameValueUnit gfNvu = new NameValueUnit(gfAttribute.getName(), DataType.DT_SHORT, (short)15);
        lcValues.add(gfNvu);
        Instance instance = api.createInstance(lcElement.getId(), lcValues);
        assertThat(instance.getValue(gfAttribute.getName()).getValue().shortVal()).isEqualTo((short)15);
        
        // update attribute value
        Collection<NameValueUnit> updateValues = new ArrayList<>();
        NameValueUnit updateNvu = new NameValueUnit(gfAttribute.getName(), DataType.DT_SHORT, (short)6);
        updateValues.add(updateNvu);
        api.setAttributeValues(lcElement.getId(), instance.getIid(), updateValues);
        assertThat(instance.getValue(gfAttribute.getName()).getValue().shortVal()).isEqualTo((short)6);
    }
    
    @Test
    void testChannelFlagsWriting(@TempDir Path tempDir) {
        OpenAtfx openAtfx = new OpenAtfx();
        Path atfxFile = Paths.get(tempDir.toString(), "unittest_asam" + 36 + ".atfx");
        OpenAtfxAPI api = openAtfx.createNewFile(atfxFile, 36);
        
        Element mqElement = api.createElement("AoMeasurementQuantity", "MeaQuantity");
        Collection<NameValueUnit> mqValues = new ArrayList<>();
        mqValues.add(new NameValueUnit(mqElement.getAttributeByBaseName("datatype").getName(), DataType.DT_ENUM, DataType.DT_FLOAT.ordinal()));
        Instance mqInstance = api.createInstance(mqElement.getId(), mqValues);
        
        Element lcElement = api.createElement("AoLocalColumn", "LocalColumn");
        Attribute gfAttribute = api.createAttribute(lcElement.getId(), "GlobalFlag", "global_flag", DataType.DT_SHORT, 1, 0, null, false, false, false);
        Attribute valuesAttribute = lcElement.getAttributeByBaseName("values");
        api.createAttribute(lcElement.getId(), "Flags", "flags", DataType.DS_SHORT, 1, 0, null, false, false, false);
        Collection<NameValueUnit> lcValues = new ArrayList<>();
        lcValues.add(new NameValueUnit(gfAttribute.getName(), DataType.DT_SHORT, (short)15));
        lcValues.add(new NameValueUnit(valuesAttribute.getName(), DataType.DS_LONG, new int[] {5, 6, 7}));
        Instance lcInstance = api.createInstance(lcElement.getId(), lcValues);
        
        BaseRelation baseRel = api.getBaseRelation("aolocalcolumn", "aomeasurementquantity");
        Relation rel = api.createRelation(lcElement, mqElement, baseRel, "MeasurementQuantity", "LocalColumns", (short)1, (short)1);
        api.setRelatedInstances(lcElement.getId(), lcInstance.getIid(), rel.getRelationName(), Arrays.asList(mqInstance.getIid()), SetType.UPDATE);
        
        Element ecElement = api.createElement("AoExternalComponent", "ExternalComponent");
        Collection<NameValueUnit> ecValues = new ArrayList<>();
        ecValues.add(new NameValueUnit(ecElement.getAttributeByBaseName("name").getName(), DataType.DT_STRING, "EC_TEST"));
        Instance ecInstance = api.createInstance(ecElement.getId(), ecValues);
        BaseRelation ecLcBaseRel = api.getBaseRelation("AoExternalComponent", "AoLocalColumn");
        Relation ecLcRel = api.createRelation(ecElement, lcElement, ecLcBaseRel, "LocalColumn", "ExternalComponents", (short)1, (short)1);
        api.setRelatedInstances(ecElement.getId(), ecInstance.getIid(), ecLcRel.getRelationName(), Arrays.asList(lcInstance.getIid()), SetType.UPDATE);
        
        api.writeAtfx(atfxFile.toFile());
        assertThat(atfxFile.toFile()).isNotEmpty();
    }
}
