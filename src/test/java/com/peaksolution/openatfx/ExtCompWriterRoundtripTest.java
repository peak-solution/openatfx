package com.peaksolution.openatfx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationAttribute;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.SetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.BaseRelation;
import com.peaksolution.openatfx.api.Complex;
import com.peaksolution.openatfx.api.DoubleComplex;
import com.peaksolution.openatfx.api.Element;
import com.peaksolution.openatfx.api.Instance;
import com.peaksolution.openatfx.api.NameValueUnit;
import com.peaksolution.openatfx.api.OpenAtfxAPI;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.api.Relation;

@ExtendWith(GlassfishCorbaExtension.class)
class ExtCompWriterRoundtripTest {

    @Test
    void testComplexAndDComplexRoundtrip(@TempDir Path tempDir) throws OpenAtfxException {
        Path atfxFile = tempDir.resolve("complex_roundtrip.atfx");
        OpenAtfx openAtfx = new OpenAtfx();
        OpenAtfxAPI api = openAtfx.createNewFile(atfxFile, 36);

        ensureUnitInfrastructure(api);

        // Build minimal model with local columns and external component relation for binary value storage.
        Element mqElement = api.createElement("AoMeasurementQuantity", "MeaQuantity");
        Element lcElement = api.createElement("AoLocalColumn", "LocalColumn");
        Element ecElement = api.createElement("AoExternalComponent", "ExternalComponent");
        com.peaksolution.openatfx.api.Attribute globalFlagAttribute = api.createAttribute(lcElement.getId(),
            "GlobalFlag", "global_flag", com.peaksolution.openatfx.api.DataType.DT_SHORT, 1, 0, null, false,
            false, false);

        BaseRelation lcMqBaseRel = api.getBaseRelation("aolocalcolumn", "aomeasurementquantity");
        Relation lcMqRel = api.createRelation(lcElement, mqElement, lcMqBaseRel, "MeasurementQuantity", "LocalColumns",
                (short) 1, (short) 1);

        BaseRelation ecLcBaseRel = api.getBaseRelation("AoExternalComponent", "AoLocalColumn");
        Relation ecLcRel = api.createRelation(ecElement, lcElement, ecLcBaseRel, "LocalColumn", "ExternalComponents",
                (short) 1, (short) 1);

        Instance mqComplex = createMq(api, mqElement, com.peaksolution.openatfx.api.DataType.DT_COMPLEX.ordinal(),
            "MqComplex");
        Instance mqDComplex = createMq(api, mqElement, com.peaksolution.openatfx.api.DataType.DT_DCOMPLEX.ordinal(),
            "MqDComplex");

        Complex[] complexExpected = new Complex[] {
                new Complex(1.1f, 0.1f),
                new Complex(-2.2f, 3.3f),
                new Complex(Float.MIN_VALUE, Float.MAX_VALUE)
        };
        DoubleComplex[] dcomplexExpected = new DoubleComplex[] {
                new DoubleComplex(1.11, 0.11),
                new DoubleComplex(-2.22, 3.33),
                new DoubleComplex(Double.MIN_VALUE, Double.MAX_VALUE)
        };

        Instance lcComplex = createLc(api, lcElement, globalFlagAttribute.getName(), "LC_COMPLEX",
            com.peaksolution.openatfx.api.DataType.DS_COMPLEX, complexExpected);
        Instance lcDComplex = createLc(api, lcElement, globalFlagAttribute.getName(), "LC_DCOMPLEX",
            com.peaksolution.openatfx.api.DataType.DS_DCOMPLEX, dcomplexExpected);

        api.setRelatedInstances(lcElement.getId(), lcComplex.getIid(), lcMqRel.getRelationName(),
                Arrays.asList(mqComplex.getIid()), SetType.UPDATE);
        api.setRelatedInstances(lcElement.getId(), lcDComplex.getIid(), lcMqRel.getRelationName(),
                Arrays.asList(mqDComplex.getIid()), SetType.UPDATE);

        Instance ecComplex = createEc(api, ecElement, "EC_COMPLEX");
        Instance ecDComplex = createEc(api, ecElement, "EC_DCOMPLEX");

        api.setRelatedInstances(ecElement.getId(), ecComplex.getIid(), ecLcRel.getRelationName(),
                Arrays.asList(lcComplex.getIid()), SetType.UPDATE);
        api.setRelatedInstances(ecElement.getId(), ecDComplex.getIid(), ecLcRel.getRelationName(),
                Arrays.asList(lcDComplex.getIid()), SetType.UPDATE);

        api.writeAtfx(atfxFile.toFile());

        try {
            ORB orb = ORB.init(new String[0], System.getProperties());
            AoFactory factory = AoServiceFactory.getInstance().newAoFactory(orb);
            AoSession session = factory.newSession("FILENAME=" + atfxFile.toFile());
            try {
                ApplicationStructure as = session.getApplicationStructure();
                ApplicationElement lcApplicationElement = as.getElementsByBaseType("AoLocalColumn")[0];
                ApplicationAttribute valuesAttribute = lcApplicationElement.getAttributeByBaseName("values");

                boolean foundComplex = false;
                boolean foundDComplex = false;

                InstanceElementIterator lcIterator = lcApplicationElement.getInstances("*");
                for (int i = 0; i < lcIterator.getCount(); i++) {
                    InstanceElement localColumn = lcIterator.nextOne();
                    String lcName = localColumn.getValueByBaseName("name").value.u.stringVal();
                    org.asam.ods.NameValueUnit values = localColumn.getValue(valuesAttribute.getName());

                    if ("LC_COMPLEX".equals(lcName)) {
                        foundComplex = true;
                        assertArrayEquals(toScalarArray(complexExpected), toScalarArray(values.value.u.complexSeq()),
                                0.00001f);
                    } else if ("LC_DCOMPLEX".equals(lcName)) {
                        foundDComplex = true;
                        assertArrayEquals(toScalarArray(dcomplexExpected), toScalarArray(values.value.u.dcomplexSeq()),
                                0.000000000001d);
                    }
                }

                assertTrue(foundComplex, "Complex local column should be found and readable");
                assertTrue(foundDComplex, "DComplex local column should be found and readable");
            } finally {
                session.close();
            }
        } catch (Exception e) {
            throw new AssertionError("Roundtrip validation failed", e);
        }
    }

    private void ensureUnitInfrastructure(OpenAtfxAPI api) throws OpenAtfxException {
        if (api.getElementsByBaseType("AoUnit").isEmpty()) {
            Element dimElement = api.createElement("AoPhysicalDimension", "PhysDim");
            Element unitElement = api.createElement("AoUnit", "Unit");
            api.createRelationFromBaseRelation(unitElement.getId(), dimElement.getId(), "phys_dimension", "dim",
                    "units");
        }
    }

    private Instance createMq(OpenAtfxAPI api, Element mqElement, int dataTypeOrdinal, String name)
            throws OpenAtfxException {
        Collection<NameValueUnit> values = new ArrayList<>();
        values.add(new NameValueUnit("name", com.peaksolution.openatfx.api.DataType.DT_STRING, name));
        values.add(new NameValueUnit(mqElement.getAttributeByBaseName("datatype").getName(),
                com.peaksolution.openatfx.api.DataType.DT_ENUM, dataTypeOrdinal));
        return api.createInstance(mqElement.getId(), values);
    }

    private Instance createLc(OpenAtfxAPI api, Element lcElement, String globalFlagAttributeName, String name,
            com.peaksolution.openatfx.api.DataType dsType, Object value) throws OpenAtfxException {
        Collection<NameValueUnit> values = new ArrayList<>();
        values.add(new NameValueUnit("name", com.peaksolution.openatfx.api.DataType.DT_STRING, name));
        values.add(new NameValueUnit(globalFlagAttributeName, com.peaksolution.openatfx.api.DataType.DT_SHORT,
                (short) 15));
        values.add(new NameValueUnit(lcElement.getAttributeByBaseName("values").getName(), dsType, value));
        return api.createInstance(lcElement.getId(), values);
    }

    private Instance createEc(OpenAtfxAPI api, Element ecElement, String name) throws OpenAtfxException {
        Collection<NameValueUnit> values = new ArrayList<>();
        values.add(new NameValueUnit(ecElement.getAttributeByBaseName("name").getName(),
                com.peaksolution.openatfx.api.DataType.DT_STRING, name));
        return api.createInstance(ecElement.getId(), values);
    }

    private float[] toScalarArray(Complex[] values) {
        float[] result = new float[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            result[i * 2] = values[i].getR();
            result[i * 2 + 1] = values[i].getI();
        }
        return result;
    }

    private float[] toScalarArray(org.asam.ods.T_COMPLEX[] values) {
        float[] result = new float[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            result[i * 2] = values[i].r;
            result[i * 2 + 1] = values[i].i;
        }
        return result;
    }

    private double[] toScalarArray(DoubleComplex[] values) {
        double[] result = new double[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            result[i * 2] = values[i].getR();
            result[i * 2 + 1] = values[i].getI();
        }
        return result;
    }

    private double[] toScalarArray(org.asam.ods.T_DCOMPLEX[] values) {
        double[] result = new double[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            result[i * 2] = values[i].r;
            result[i * 2 + 1] = values[i].i;
        }
        return result;
    }
}
