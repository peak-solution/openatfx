package com.peaksolution.openatfx.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.peaksolution.datamodel.Attribute;
import com.peaksolution.datamodel.DataType;
import com.peaksolution.datamodel.Element;
import com.peaksolution.datamodel.Instance;
import com.peaksolution.datamodel.NameValueUnit;
import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

class LazyLocalColumnLoadingTest {

    @Test
    void getValuesDefersLocalColumnValuesUntilExplicitAccess() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/example.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        String valuesAttrName = lcElement.getAttributeByBaseName("values").getName();
        String flagsAttrName = lcElement.getAttributeByBaseName("flags").getName();
        Instance voltageLc = getInstanceByName(api, lcElement.getId(), "Voltage.NF.Trigger 2");

        assertThat(voltageLc.getValues(true)).extracting(NameValueUnit::getValName)
                                           .doesNotContain(valuesAttrName, flagsAttrName);

        NameValueUnit valuesNvu = voltageLc.getValueByBaseName("values");
        assertThat(valuesNvu).isNotNull();
        assertThat(valuesNvu.isValid()).isTrue();
        assertThat(valuesNvu.getValue().floatSeq()).hasSize(174);

        assertThat(voltageLc.getValues(true)).extracting(NameValueUnit::getValName)
                                           .contains(valuesAttrName)
                                           .doesNotContain(flagsAttrName);
    }

    @Test
    void getValuesDefersLocalColumnFlagsUntilExplicitAccess() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/external_with_flags.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        String valuesAttrName = lcElement.getAttributeByBaseName("values").getName();
        String flagsAttrName = lcElement.getAttributeByBaseName("flags").getName();
        Instance lcWithExternalFlags = getInstanceByName(api, lcElement.getId(), "51900778_1");

        assertThat(lcWithExternalFlags.getValues(true)).extracting(NameValueUnit::getValName)
                                                      .doesNotContain(valuesAttrName, flagsAttrName);

        NameValueUnit flagsNvu = lcWithExternalFlags.getValueByBaseName("flags");
        assertThat(flagsNvu).isNotNull();
        assertThat(flagsNvu.isValid()).isTrue();
        assertThat(flagsNvu.getValue().shortSeq()).hasSize(300004)
                                                  .startsWith((short) 15, (short) 15, (short) 0, (short) 15);

        assertThat(lcWithExternalFlags.getValues(true)).extracting(NameValueUnit::getValName)
                                                      .contains(flagsAttrName)
                                                      .doesNotContain(valuesAttrName);
    }

    @Test
    void getValueByNameLoadsDeferredLocalColumnValuesOnDemand() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/example.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        Attribute valuesAttr = lcElement.getAttributeByBaseName("values");
        Instance voltageLc = getInstanceByName(api, lcElement.getId(), "Voltage.NF.Trigger 2");

        assertThat(voltageLc.getValues(true)).extracting(NameValueUnit::getValName)
                                            .doesNotContain(valuesAttr.getName());

        NameValueUnit valuesNvu = voltageLc.getValue(valuesAttr.getName());

        assertLocalColumnValues(valuesNvu, valuesAttr.getName());
    }

    @Test
    void getValueByNumberLoadsDeferredLocalColumnValuesOnDemand() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/example.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        Attribute valuesAttr = lcElement.getAttributeByBaseName("values");
        Instance voltageLc = getInstanceByName(api, lcElement.getId(), "Voltage.NF.Trigger 2");

        assertThat(voltageLc.getValues(true)).extracting(NameValueUnit::getValName)
                                            .doesNotContain(valuesAttr.getName());

        NameValueUnit valuesNvu = voltageLc.getValue(valuesAttr.getAttrNo());

        assertLocalColumnValues(valuesNvu, valuesAttr.getName());
    }

    @Test
    void getValueByNameLoadsDeferredLocalColumnFlagsOnDemand() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/external_with_flags.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        Attribute flagsAttr = lcElement.getAttributeByBaseName("flags");
        Instance lcWithExternalFlags = getInstanceByName(api, lcElement.getId(), "51900778_1");

        assertThat(lcWithExternalFlags.getValues(true)).extracting(NameValueUnit::getValName)
                                                       .doesNotContain(flagsAttr.getName());

        NameValueUnit flagsNvu = lcWithExternalFlags.getValue(flagsAttr.getName());

        assertLocalColumnFlags(flagsNvu, flagsAttr.getName());
    }

    @Test
    void getValueByNumberLoadsDeferredLocalColumnFlagsOnDemand() throws Exception {
        OpenAtfxAPIImplementation api = readApi("/com/peaksolution/openatfx/external_with_flags.atfx");
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        Attribute flagsAttr = lcElement.getAttributeByBaseName("flags");
        Instance lcWithExternalFlags = getInstanceByName(api, lcElement.getId(), "51900778_1");

        assertThat(lcWithExternalFlags.getValues(true)).extracting(NameValueUnit::getValName)
                                                       .doesNotContain(flagsAttr.getName());

        NameValueUnit flagsNvu = lcWithExternalFlags.getValue(flagsAttr.getAttrNo());

        assertLocalColumnFlags(flagsNvu, flagsAttr.getName());
    }

    private static void assertLocalColumnValues(NameValueUnit valuesNvu, String expectedAttrName) {
        assertThat(valuesNvu).isNotNull();
        assertThat(valuesNvu.getValName()).isEqualTo(expectedAttrName);
        assertThat(valuesNvu.isValid()).isTrue();
        assertThat(valuesNvu.getValue().floatSeq()).hasSize(174);
    }

    private static void assertLocalColumnFlags(NameValueUnit flagsNvu, String expectedAttrName) {
        assertThat(flagsNvu).isNotNull();
        assertThat(flagsNvu.getValName()).isEqualTo(expectedAttrName);
        assertThat(flagsNvu.isValid()).isTrue();
        assertThat(flagsNvu.getValue().shortSeq()).hasSize(300004)
                                                  .startsWith((short) 15, (short) 15, (short) 0, (short) 15)
                                                  .endsWith((short) 0, (short) 15, (short) 15, (short) 0);
    }

    private static OpenAtfxAPIImplementation readApi(String resourceName) throws Exception {
        URL url = LazyLocalColumnLoadingTest.class.getResource(resourceName);
      assert url != null;
      Path atfxFile = Path.of(url.toURI());
        String fileRoot = Paths.get(url.toURI()).getParent().toString();
        IFileHandler fileHandler = new LocalFileHandler();
        AtfxReader reader = new AtfxReader(fileHandler, atfxFile, false, null);

        try (InputStream in = fileHandler.getFileStream(atfxFile)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader xmlReader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());
            OpenAtfxAPIImplementation api = reader.readFile(xmlReader, Collections.emptyList());
            api.setContext(new NameValueUnit("FILE_ROOT", DataType.DT_STRING, fileRoot));
            return api;
        }
    }

    private static Instance getInstanceByName(OpenAtfxAPIImplementation api, long aid, String name) {
        Collection<Instance> instances = api.getInstances(aid);
        for (Instance instance : instances) {
            if (name.equals(instance.getName())) {
                return instance;
            }
        }
        throw new AssertionError("No instance named '" + name + "' found for aid=" + aid);
    }
}


