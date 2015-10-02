package de.rechner.openatfx_mdf.util;

import java.io.IOException;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;


/**
 * Helper class to convert the MDF lookup tables to a ASAM ODS 'AoMeasurement'.
 * 
 * @author Christian Rechner
 */
public class LookupTableHelper {

    // the cached lookup instance element
    private InstanceElement lookupMeaIe;

    public synchronized void createMCD2TextTableMeasurement(ODSModelCache modelCache, InstanceElement ieMea,
            InstanceElement ieLc, double[] keys, String[] values) throws AoException, IOException {
        ApplicationElement aeMea = modelCache.getApplicationElement("mea");
        ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
        ApplicationElement aeSm = modelCache.getApplicationElement("sm");
        ApplicationElement aeLc = modelCache.getApplicationElement("lc");
        ApplicationRelation relMeaTst = modelCache.getApplicationRelation("mea", "tst", "tst");
        ApplicationRelation relSmMea = modelCache.getApplicationRelation("sm", "mea", "mea");
        ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");
        ApplicationRelation relMeaMeq = modelCache.getApplicationRelation("mea", "meq", "meqs");
        ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq");
        String lcName = ieLc.getName();

        // create 'AoMeasurement' instance (if not yet existing)
        if (this.lookupMeaIe == null) {
            // lookup parent 'AoTest' instance
            InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
            InstanceElement ieTst = iter.nextOne();
            iter.destroy();

            String meaName = ieMea.getName() + "_lookup";
            this.lookupMeaIe = aeMea.createInstance(meaName);
            this.lookupMeaIe.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurement.lookup"));
            this.lookupMeaIe.setValue(ieMea.getValue("date_created"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_end"));
            this.lookupMeaIe.createRelation(relMeaTst, ieTst);
        }

        // create 'AoSubMatrix' instance
        InstanceElement ieSm = aeSm.createInstance(lcName);
        ieSm.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aosubmatrix.lookup.value_to_text"));
        ieSm.setValue(ODSHelper.createLongNVU("rows", keys.length));
        ieSm.createRelation(relSmMea, this.lookupMeaIe);

        // create 'AoLocalColumn' instance for key
        NameValueUnit[] nvuLcKey = new NameValueUnit[6];
        nvuLcKey[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key");
        nvuLcKey[1] = ODSHelper.createEnumNVU("srp", 0);
        nvuLcKey[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcKey[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcKey[4] = ODSHelper.createEnumNVU("axistype", 0);
        nvuLcKey[5] = ODSHelper.createDoubleSeqNVU("val", keys);
        InstanceElement ieLcKey = aeLc.createInstance(lcName + "_key");
        ieLcKey.setValueSeq(nvuLcKey);
        ieSm.createRelation(relSmLc, ieLcKey);

        // create 'AoMeasurementQuantity' instance for key
        InstanceElement ieMeqKey = aeMeq.createInstance(lcName + "_key");
        ieMeqKey.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key"));
        ieMeqKey.setValue(ODSHelper.createEnumNVU("dt", 7));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqKey);
        ieLcKey.createRelation(relLcMeq, ieMeqKey);

        // create 'AoLocalColumn' instance for values
        NameValueUnit[] nvuLcValues = new NameValueUnit[6];
        nvuLcValues[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.value");
        nvuLcValues[1] = ODSHelper.createEnumNVU("srp", 0);
        nvuLcValues[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcValues[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcValues[4] = ODSHelper.createEnumNVU("axistype", 1);
        nvuLcValues[5] = ODSHelper.createStringSeqNVU("val", values);
        InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
        ieLcValues.setValueSeq(nvuLcValues);
        ieSm.createRelation(relSmLc, ieLcValues);

        // create 'AoMeasurementQuantity' instance for text
        InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
        ieMeqValues.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.value"));
        ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqValues);
        ieLcValues.createRelation(relLcMeq, ieMeqValues);
    }

    public synchronized void createMCD2TextRangeTableMeasurement(ODSModelCache modelCache, InstanceElement ieMea,
            InstanceElement ieLc, double[] keysMin, double[] keysMax, String[] values, String defaultValue)
            throws AoException, IOException {
        ApplicationElement aeMea = modelCache.getApplicationElement("mea");
        ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
        ApplicationElement aeSm = modelCache.getApplicationElement("sm");
        ApplicationElement aeLc = modelCache.getApplicationElement("lc");
        ApplicationRelation relMeaTst = modelCache.getApplicationRelation("mea", "tst", "tst");
        ApplicationRelation relSmMea = modelCache.getApplicationRelation("sm", "mea", "mea");
        ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");
        ApplicationRelation relMeaMeq = modelCache.getApplicationRelation("mea", "meq", "meqs");
        ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq");
        String lcName = ieLc.getName();

        // create 'AoMeasurement' instance (if not yet existing)
        if (this.lookupMeaIe == null) {
            // lookup parent 'AoTest' instance
            InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
            InstanceElement ieTst = iter.nextOne();
            iter.destroy();

            String meaName = ieMea.getName() + "_lookup";
            this.lookupMeaIe = aeMea.createInstance(meaName);
            this.lookupMeaIe.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurement.lookup"));
            this.lookupMeaIe.setValue(ieMea.getValue("date_created"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_end"));
            this.lookupMeaIe.createRelation(relMeaTst, ieTst);
        }

        // create 'AoSubMatrix' instance
        InstanceElement ieSm = aeSm.createInstance(lcName);
        ieSm.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aosubmatrix.lookup.value_range_to_value"));
        ieSm.setValue(ODSHelper.createLongNVU("rows", values.length));
        ieSm.createRelation(relSmMea, this.lookupMeaIe);

        // create 'AoLocalColumn' instance for key min
        NameValueUnit[] nvuLcKeyMin = new NameValueUnit[6];
        nvuLcKeyMin[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key_min");
        nvuLcKeyMin[1] = ODSHelper.createEnumNVU("srp", 0);
        nvuLcKeyMin[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcKeyMin[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcKeyMin[4] = ODSHelper.createEnumNVU("axistype", 0);
        nvuLcKeyMin[5] = ODSHelper.createDoubleSeqNVU("val", keysMin);
        InstanceElement ieLcKeyMin = aeLc.createInstance(lcName + "_key_min");
        ieLcKeyMin.setValueSeq(nvuLcKeyMin);
        ieSm.createRelation(relSmLc, ieLcKeyMin);

        // create 'AoMeasurementQuantity' instance for key min
        InstanceElement ieMeqKeyMin = aeMeq.createInstance(lcName + "_key_min");
        ieMeqKeyMin.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key_min"));
        ieMeqKeyMin.setValue(ODSHelper.createEnumNVU("dt", 7));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqKeyMin);
        ieLcKeyMin.createRelation(relLcMeq, ieMeqKeyMin);

        // create 'AoLocalColumn' instance for key max
        NameValueUnit[] nvuLcKeyMax = new NameValueUnit[6];
        nvuLcKeyMax[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key_max");
        nvuLcKeyMax[1] = ODSHelper.createEnumNVU("srp", 0);
        nvuLcKeyMax[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcKeyMax[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcKeyMax[4] = ODSHelper.createEnumNVU("axistype", 0);
        nvuLcKeyMax[5] = ODSHelper.createDoubleSeqNVU("val", keysMax);
        InstanceElement ieLcKeyMax = aeLc.createInstance(lcName + "_key_max");
        ieLcKeyMax.setValueSeq(nvuLcKeyMax);
        ieSm.createRelation(relSmLc, ieLcKeyMax);

        // create 'AoMeasurementQuantity' instance for key max
        InstanceElement ieMeqKeyMax = aeMeq.createInstance(lcName + "_key_max");
        ieMeqKeyMax.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key_max"));
        ieMeqKeyMax.setValue(ODSHelper.createEnumNVU("dt", 7));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqKeyMax);
        ieLcKeyMax.createRelation(relLcMeq, ieMeqKeyMax);

        // create 'AoLocalColumn' instance for values
        NameValueUnit[] nvuLcValues = new NameValueUnit[6];
        nvuLcValues[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.value");
        nvuLcValues[1] = ODSHelper.createEnumNVU("srp", 0);
        nvuLcValues[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcValues[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcValues[4] = ODSHelper.createEnumNVU("axistype", 1);
        nvuLcValues[5] = ODSHelper.createStringSeqNVU("val", values);
        InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
        ieLcValues.setValueSeq(nvuLcValues);
        ieSm.createRelation(relSmLc, ieLcValues);

        // create 'AoMeasurementQuantity' instance for value
        InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
        ieMeqValues.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.value"));
        ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqValues);
        ieLcValues.createRelation(relLcMeq, ieMeqValues);

        // create 'AoLocalColumn' instance for default value
        NameValueUnit[] nvuLcDefValue = new NameValueUnit[6];
        nvuLcDefValue[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.default_value");
        nvuLcDefValue[1] = ODSHelper.createEnumNVU("srp", 1);
        nvuLcDefValue[2] = ODSHelper.createShortNVU("idp", (short) 0);
        nvuLcDefValue[3] = ODSHelper.createShortNVU("glb", (short) 15);
        nvuLcDefValue[4] = ODSHelper.createEnumNVU("axistype", 1);
        nvuLcDefValue[5] = ODSHelper.createStringSeqNVU("val", new String[] { defaultValue });
        InstanceElement ieLcDefValue = aeLc.createInstance(lcName + "_default_value");
        ieLcDefValue.setValueSeq(nvuLcDefValue);
        ieSm.createRelation(relSmLc, ieLcDefValue);

        // create 'AoMeasurementQuantity' instance for default value
        InstanceElement ieMeqDefValue = aeMeq.createInstance(lcName + "_default_value");
        ieMeqDefValue.setValue(ODSHelper.createStringNVU("mt",
                                                         "application/x-asam.aomeasurementquantity.lookup.default_value"));
        ieMeqDefValue.setValue(ODSHelper.createEnumNVU("dt", 1));
        this.lookupMeaIe.createRelation(relMeaMeq, ieMeqDefValue);
        ieLcDefValue.createRelation(relLcMeq, ieMeqDefValue);
    }

}
