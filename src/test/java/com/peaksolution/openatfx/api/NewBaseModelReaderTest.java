package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import generated.ODSBaseModel.Model.Element;
import generated.ODSBaseModel.Model.Enumeration;
import generated.ODSBaseModel.Model.Enumeration.Item;

class NewBaseModelReaderTest {

    private NewBaseModelReader reader;

    @BeforeEach
    public void setUp() {
        reader = new NewBaseModelReader();
    }

    @Test
    void testGetBaseModel_InvalidVersion() {
        // Test for an invalid version
        Exception exception = assertThrows(OpenAtfxException.class, () -> {
            reader.getBaseModel("invalid_version");
        });
        
        String expectedMessage = "No base model file found for version invalid_version";
        assertThat(exception.getMessage()).contains(expectedMessage);
    }

    @Test
    void testParseEnumerations() {
        Enumeration enumeration = new Enumeration();
        enumeration.setName("TestEnum");
        Item item = new Item();
        item.setValue(1);
        item.setName("Item1");
        enumeration.getItem().add(item);
        
        Collection<EnumerationDefinition> result = reader.parseEnumerations(Collections.singletonList(enumeration));
        
        assertThat(result).hasSize(1);
        EnumerationDefinition enumDef = result.iterator().next();
        assertThat(enumDef.getName()).isEqualTo("TestEnum");
        assertThat(enumDef.listItemNames()).hasSize(1);
        assertThat(enumDef.getItemName(1)).isEqualTo("Item1");
    }

    @Test
    void testParseElements() {
        // Create mock data
        Element mockElement = mock(Element.class);
        when(mockElement.getName()).thenReturn("TestElement");
        when(mockElement.getAttribute()).thenReturn(Collections.emptyList());
        when(mockElement.getUniqueness()).thenReturn(Collections.emptyList());

        Map<String, AtfxBaseElement> result = reader.parseElements(Collections.emptyList(), Collections.singletonList(mockElement));
        
        assertThat(result).hasSize(1);
        AtfxBaseElement baseElement = result.get("TestElement");
        assertThat(baseElement).isNotNull();
        assertThat(baseElement.getType()).isEqualTo("TestElement");
    }
}
