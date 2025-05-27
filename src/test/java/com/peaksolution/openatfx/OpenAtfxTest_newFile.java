package com.peaksolution.openatfx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.peaksolution.openatfx.api.EnumerationDefinition;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.api.OpenAtfxException;

class OpenAtfxNnewFileTest {
    
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
    void testContext(int baseModelVersion, @TempDir Path tempDir) {
        OpenAtfx openAtfx = new OpenAtfx();
        OpenAtfxAPI api = openAtfx.createNewFile(Paths.get(tempDir.toString(), "context_asam" + baseModelVersion + ".atfx"), baseModelVersion);
        
        assertThat(api.getContext()).hasSize(18);
        //TODO test context further
    }
    
    @ParameterizedTest
    @MethodSource("baseModelVersions")
    void testEnumerations(int baseModelVersion, @TempDir Path tempDir) {
        OpenAtfx openAtfx = new OpenAtfx();
        OpenAtfxAPI api = openAtfx.createNewFile(Paths.get(tempDir.toString(), "enumerations_asam" + baseModelVersion + ".atfx"), baseModelVersion);
        
        // base enums
        Collection<String> expectedBaseEnums = new HashSet<>(Arrays.asList("interpolation_enum", "typespec_enum", "datatype_enum", "seq_rep_enum"));
        if (baseModelVersion > 30) {
            expectedBaseEnums.add("ao_storagetype_enum");
        }
        assertThat(api.listEnumerationNames(true)).containsExactlyInAnyOrderElementsOf(expectedBaseEnums);
        
        // create enums
        String enumName1 = "testEnum";
        api.createEnumeration(enumName1);
        api.addEnumerationItem(enumName1, 1, "one");
        api.addEnumerationItem(enumName1, 2, "two");
        api.addEnumerationItem(enumName1, 3, "three");
        
        EnumerationDefinition enumDef = api.getEnumerationDefinition(enumName1);
        assertThat(enumDef.getItem("one")).isEqualTo(1);
        assertThat(enumDef.getItemName(2)).isEqualTo("two");
        assertThat(enumDef.getItem("three")).isEqualTo(3);
        
        String[] itemNames = enumDef.listItemNames();
        for (int i = 1; i <= itemNames.length; i++) {
            assertThat(itemNames[i - 1]).isEqualTo(enumDef.getItemName(i));
        }
        
        String enumName2 = "testEnum_unsorted";
        api.createEnumeration(enumName2);
        api.addEnumerationItem(enumName2, 3, "drei");
        api.addEnumerationItem(enumName2, 1, "eins");
        api.addEnumerationItem(enumName2, 2, "zwei");
        
        enumDef = api.getEnumerationDefinition(enumName2);
        assertThat(enumDef.getItem("eins")).isEqualTo(1);
        assertThat(enumDef.getItemName(2)).isEqualTo("zwei");
        assertThat(enumDef.getItem("drei")).isEqualTo(3);
        
        itemNames = enumDef.listItemNames();
        for (int i = 1; i <= itemNames.length; i++) {
            assertThat(itemNames[i - 1]).isEqualTo(api.getEnumerationItemName(enumName2, i));
        }
        assertThat(api.listEnumerationNames(false)).containsExactlyInAnyOrder(new String[] {enumName1, enumName2});
        
        // remove enums
        api.removeEnumeration(enumName2);
        assertThat(api.listEnumerationNames(false)).containsExactlyInAnyOrder(new String[] {enumName1});
        
        // prevent base enum deletion
        OpenAtfxException thrown = assertThrows("Expected doThing() to throw, but it didn't", OpenAtfxException.class,
                                                () -> api.removeEnumeration("datatype_enum"));
        assertThat(thrown.getMessage().contains("not allowed!")).isTrue();
    }
}
