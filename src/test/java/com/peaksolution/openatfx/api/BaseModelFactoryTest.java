package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.asam.ods.RelationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import generated.ODSBaseModel;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;


class BaseModelFactoryTest {

    @ParameterizedTest
    @ValueSource(ints = {29, 30, 31, 32, 33, 34, 35, 36})
    void testGetBaseModel(int versionNumber) {
        String baseModelVersion = "asam" + versionNumber;
        BaseModelFactory factory = BaseModelFactory.getInstance();
        BaseModel model = factory.getBaseModel(baseModelVersion);
        
        // Enumerations
        Collection<EnumerationDefinition> enumerations = model.getEnumerations();
        Map<String, Map<Long, String>> expectedEnums = new HashMap<>();
        Map<Long, String> enumItems = expectedEnums.computeIfAbsent("datatype_enum", v -> new HashMap<>());
        enumItems.put(0L, "DT_UNKNOWN");
        enumItems.put(1L, "DT_STRING");
        enumItems.put(2L, "DT_SHORT");
        enumItems.put(3L, "DT_FLOAT");
        enumItems.put(4L, "DT_BOOLEAN");
        enumItems.put(5L, "DT_BYTE");
        enumItems.put(6L, "DT_LONG");
        enumItems.put(7L, "DT_DOUBLE");
        enumItems.put(8L, "DT_LONGLONG");
        enumItems.put(9L, "DT_ID");
        enumItems.put(10L, "DT_DATE");
        enumItems.put(11L, "DT_BYTESTR");
        enumItems.put(12L, "DT_BLOB");
        enumItems.put(13L, "DT_COMPLEX");
        enumItems.put(14L, "DT_DCOMPLEX");
        enumItems.put(28L, "DT_EXTERNALREFERENCE");
        enumItems.put(30L, "DT_ENUM");
        if (versionNumber >= 31) {
            enumItems = expectedEnums.computeIfAbsent("ao_storagetype_enum", v -> new HashMap<>());
            enumItems.put(0L, "database");
            enumItems.put(1L, "external_only");
            enumItems.put(2L, "mixed");
            enumItems.put(3L, "foreign_format");
        }
        enumItems = expectedEnums.computeIfAbsent("interpolation_enum", v -> new HashMap<>());
        enumItems.put(0L, "no_interpolation");
        enumItems.put(1L, "linear_interpolation");
        enumItems.put(2L, "application_specific");
        enumItems = expectedEnums.computeIfAbsent("typespec_enum", v -> new HashMap<>());
        enumItems.put(0L, "dt_boolean");
        enumItems.put(1L, "dt_byte");
        enumItems.put(2L, "dt_short");
        enumItems.put(3L, "dt_long");
        enumItems.put(4L, "dt_longlong");
        enumItems.put(5L, "ieeefloat4");
        enumItems.put(6L, "ieeefloat8");
        enumItems.put(7L, "dt_short_beo");
        enumItems.put(8L, "dt_long_beo");
        enumItems.put(9L, "dt_longlong_beo");
        enumItems.put(10L, "ieeefloat4_beo");
        enumItems.put(11L, "ieeefloat8_beo");
        enumItems.put(12L, "dt_string");
        enumItems.put(13L, "dt_bytestr");
        if (versionNumber < 33) {
            enumItems.put(14L, "dt_blob");
            enumItems.put(15L, "dt_boolean_flags_beo");
            enumItems.put(16L, "dt_byte_flags_beo");
            enumItems.put(17L, "dt_string_flags_beo");
        }
        enumItems.put(18L, "dt_bytestr_beo");
        enumItems.put(19L, "dt_sbyte");
        enumItems.put(20L, "dt_sbyte_flags_beo");
        enumItems.put(21L, "dt_ushort");
        enumItems.put(22L, "dt_ushort_beo");
        enumItems.put(23L, "dt_ulong");
        enumItems.put(24L, "dt_ulong_beo");
        if (versionNumber >= 31) {
            enumItems.put(25L, "dt_string_utf8");
            if (versionNumber < 33) {
                enumItems.put(26L, "dt_string_utf8_beo");
            } else {
                enumItems.put(26L, "dt_string_utf8_flags_beo");
            }
            enumItems.put(27L, "dt_bit_int");
            enumItems.put(28L, "dt_bit_int_beo");
            enumItems.put(29L, "dt_bit_uint");
            enumItems.put(30L, "dt_bit_uint_beo");
            if (versionNumber < 33) {
                enumItems.put(31L, "dt_bit_float");
                enumItems.put(32L, "dt_bit_float_beo");
            } else {
                enumItems.put(31L, "dt_bit_ieeefloat");
                enumItems.put(32L, "dt_bit_ieeefloat_beo");
            }
            enumItems.put(33L, "dt_bytestr_leo");
        }
        enumItems = expectedEnums.computeIfAbsent("seq_rep_enum", v -> new HashMap<>());
        enumItems.put(0L, "explicit");
        enumItems.put(1L, "implicit_constant");
        enumItems.put(2L, "implicit_linear");
        enumItems.put(3L, "implicit_saw");
        enumItems.put(4L, "raw_linear");
        enumItems.put(5L, "raw_polynomial");
        enumItems.put(6L, "formula");
        enumItems.put(7L, "external_component");
        enumItems.put(8L, "raw_linear_external");
        enumItems.put(9L, "raw_polynomial_external");
        enumItems.put(10L, "raw_linear_calibrated");
        enumItems.put(11L, "raw_linear_calibrated_external");
        if (versionNumber >= 33) {
            enumItems.put(12L, "raw_rational");
            enumItems.put(13L, "raw_rational_external");
        }
        
        Map<String, Map<Long, String>> collectedEnums = new HashMap<>(); 
        for (EnumerationDefinition enumDef : enumerations) {
            Map<Long, String> collectedItems = collectedEnums.computeIfAbsent(enumDef.getName(), v -> new HashMap<>());
            for (String itemName : enumDef.listItemNames()) {
                long item = enumDef.getItem(itemName);
                collectedItems.put(item, itemName);
            }
        }
        assertThat(collectedEnums).containsExactlyInAnyOrderEntriesOf(expectedEnums);
        
        // Elements
        Collection<String> expectedBaseElementNames = new ArrayList<>();
        expectedBaseElementNames.addAll(Arrays.asList("AoNameMap", "AoAttributeMap", "AoParameter", "AoParameterSet",
                                                      "AoEnvironment", "AoTest", "AoSubTest", "AoMeasurement",
                                                      "AoMeasurementQuantity", "AoSubmatrix", "AoExternalComponent",
                                                      "AoLocalColumn", "AoUnitUnderTest", "AoUnitUnderTestPart",
                                                      "AoTestSequence", "AoTestSequencePart", "AoTestEquipment",
                                                      "AoTestEquipmentPart", "AoTestDevice", "AoQuantity", "AoUnit",
                                                      "AoPhysicalDimension", "AoQuantityGroup", "AoUnitGroup", "AoLog",
                                                      "AoAny", "AoUser", "AoUserGroup"));
        if (versionNumber >= 31) {
            expectedBaseElementNames.add("AoFile");
        }
        if (versionNumber >= 33) {
            expectedBaseElementNames.add("AoMimetypeMap");
        }
        
        BaseElement[] baseElements = model.getElements("*");
        Collection<String> collectedBaseElementNames = Arrays.stream(baseElements).map(BaseElement::getType).toList();
        assertThat(collectedBaseElementNames).containsExactlyInAnyOrderElementsOf(expectedBaseElementNames);
        
        // Attributes
        BaseElement[] meaElements = model.getElements("aomeasurement");
        assertThat(meaElements).hasSize(1);
        BaseElement meaElement = meaElements[0];
        Set<String> expectedAttributeNames = new HashSet<>();
        expectedAttributeNames.addAll(Arrays.asList("name", "id", "version", "description", "version_date", "mime_type",
                                                    "external_references", "objecttype", "measurement_begin",
                                                    "measurement_end"));
        if (versionNumber >= 31) {
            expectedAttributeNames.addAll(Arrays.asList("ao_created", "ao_created_by", "ao_last_modified",
                                                        "ao_last_modified_by", "ao_values_accessed",
                                                        "ao_values_accessed_by", "ao_values_modified",
                                                        "ao_values_modified_by", "ao_storagetype", "ao_mea_size"));
        }
        
        Set<String> collectedAttributeNames = new HashSet<>();
        for (BaseAttribute attr : meaElement.getAttributes("*")) {
            String attrName = attr.getName();
            collectedAttributeNames.add(attrName);
            
            assertThat(attr.getBaseElement()).isEqualTo(meaElement);
            if ("name".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isTrue();
                assertThat(attr.isObligatory()).isTrue();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("id".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_LONGLONG);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isTrue();
                assertThat(attr.isObligatory()).isTrue();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isTrue();
            } else if ("version".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("description".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("version_date".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("mime_type".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("external_references".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DS_EXTERNALREFERENCE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("objecttype".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_LONGLONG);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_created".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_created_by".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_last_modified".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_last_modified_by".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_values_accessed".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_values_accessed_by".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_values_modified".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_values_modified_by".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_STRING);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("measurement_begin".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("measurement_end".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_DATE);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isFalse();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_storagetype".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_ENUM);
                assertThat(attr.getEnumName()).isEqualTo("ao_storagetype_enum");
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            } else if ("ao_mea_size".equals(attrName)) {
                assertThat(attr.getDataType()).isEqualTo(DataType.DT_LONGLONG);
                assertThat(attr.getEnumName()).isNull();
                assertThat(attr.isMandatory()).isFalse();
                assertThat(attr.isObligatory()).isFalse();
                assertThat(attr.isAutogenerated()).isTrue();
                assertThat(attr.isUnique()).isFalse();
            }
        }
        assertThat(collectedAttributeNames).containsExactlyInAnyOrderElementsOf(expectedAttributeNames);
        
        // Relations
        Set<String> expectedRelationNames = new HashSet<>();
        expectedRelationNames.addAll(Arrays.asList("test", "measurement_quantities", "submatrices", "units_under_test", "sequences",
                                                    "equipments"));
        if (versionNumber >= 31) {
            expectedRelationNames.add("ao_file_children");
        }
        
        Set<String> collectedRelationNames = new HashSet<>();
        boolean testRelationFound = false;
        for (BaseRelation rel : meaElement.getRelations()) {
            String relName = rel.getName();
            collectedRelationNames.add(relName);
            
            assertThat(rel.getElem1()).isEqualTo(meaElement);
            if ("ao_file_children".equals(relName)) {
                BaseElement[] fileElements = model.getElements("aofile");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(fileElements);
                assertThat(rel.getInverseName()).isEqualTo("ao_file_parent");
                assertThat(rel.isMandatory()).isFalse();
                assertThat(rel.getRelationship()).isEqualTo(Relationship.CHILD);
                assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(rel.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) 1);
            } else if ("test".equals(relName)) {
                testRelationFound = true;
                BaseElement[] testElements = model.getElements("aotest");
                BaseElement[] subTestElements = model.getElements("aosubtest");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(new BaseElement[] {testElements[0], subTestElements[0]});
                assertThat(rel.getInverseName()).isEqualTo("children");
                assertThat(rel.isMandatory()).isTrue();
                assertThat(rel.getRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.CHILD);
                assertThat(rel.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 1);
                assertThat(rel.getRelationRange().max).isEqualTo((short) 1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) -1);
            } else if ("measurement_quantities".equals(relName)) {
                BaseElement[] meaQuElements = model.getElements("aomeasurementquantity");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(meaQuElements);
                assertThat(rel.getInverseName()).isEqualTo("measurement");
                assertThat(rel.isMandatory()).isTrue();
                assertThat(rel.getRelationship()).isEqualTo(Relationship.CHILD);
                assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(rel.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 1);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) 1);
            } else if ("submatrices".equals(relName)) {
                BaseElement[] matrixElements = model.getElements("aosubmatrix");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(matrixElements);
                assertThat(rel.getInverseName()).isEqualTo("measurement");
                assertThat(rel.isMandatory()).isTrue();
                assertThat(rel.getRelationship()).isEqualTo(Relationship.CHILD);
                assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.FATHER);
                assertThat(rel.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 1);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) 1);
            } else if ("units_under_test".equals(relName)) {
                BaseElement[] uutElements = model.getElements("aounitundertest");
                BaseElement[] uutPartElements = model.getElements("aounitundertestpart");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(new BaseElement[] {uutElements[0], uutPartElements[0]});
                assertThat(rel.getInverseName()).isEqualTo("measurement");
                assertThat(rel.isMandatory()).isFalse();
                if (versionNumber < 33) {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_TO);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_FROM);
                } else {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_REL);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_REL);
                }
                assertThat(rel.getRelationType()).isEqualTo(RelationType.INFO);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) -1);
            } else if ("sequences".equals(relName)) {
                BaseElement[] tsElements = model.getElements("aotestsequence");
                BaseElement[] tsPartElements = model.getElements("aotestsequencepart");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(new BaseElement[] {tsElements[0], tsPartElements[0]});
                assertThat(rel.getInverseName()).isEqualTo("measurement");
                assertThat(rel.isMandatory()).isFalse();
                if (versionNumber < 33) {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_TO);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_FROM);
                } else {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_REL);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_REL);
                }
                assertThat(rel.getRelationType()).isEqualTo(RelationType.INFO);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) -1);
            } else if ("equipments".equals(relName)) {
                BaseElement[] teElements = model.getElements("aotestequipment");
                BaseElement[] tePartElements = model.getElements("aotestequipmentpart");
                BaseElement[] deviceElements = model.getElements("aotestdevice");
                assertThat(rel.getElem2()).containsExactlyInAnyOrder(new BaseElement[] {teElements[0], tePartElements[0], deviceElements[0]});
                assertThat(rel.getInverseName()).isEqualTo("measurement");
                assertThat(rel.isMandatory()).isFalse();
                if (versionNumber < 33) {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_TO);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_FROM);
                } else {
                    assertThat(rel.getRelationship()).isEqualTo(Relationship.INFO_REL);
                    assertThat(rel.getInverseRelationship()).isEqualTo(Relationship.INFO_REL);
                }
                assertThat(rel.getRelationType()).isEqualTo(RelationType.INFO);
                assertThat(rel.getRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getRelationRange().max).isEqualTo((short) -1);
                assertThat(rel.getInverseRelationRange().min).isEqualTo((short) 0);
                assertThat(rel.getInverseRelationRange().max).isEqualTo((short) -1);
            }
        }
        assertThat(testRelationFound).isTrue();
        assertThat(collectedRelationNames).containsExactlyInAnyOrderElementsOf(expectedRelationNames);
    }

    @Test
    void testJaxbModel() throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(ODSBaseModel.class);
        ODSBaseModel model = (ODSBaseModel) context.createUnmarshaller()
                                                   .unmarshal(new FileReader("src/main/resources/com/peaksolution/openatfx/api/ODSBaseModel_asam36.xml"));
        assertThat(model).isNotNull();
    }
}
