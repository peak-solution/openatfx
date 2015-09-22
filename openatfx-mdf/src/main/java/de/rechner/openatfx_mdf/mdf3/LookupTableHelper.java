package de.rechner.openatfx_mdf.mdf3;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;

import de.rechner.openatfx_mdf.ConvertException;
import de.rechner.openatfx_mdf.util.ODSHelper;


class LookupTableHelper {

    public static void createMCD2TextTableMeasurement(InstanceElement ieMea, InstanceElement ieLc, CNBLOCK cnBlock,
            CCBLOCK ccBlock, File sourceFile, FileChannel binChannel, String dataFilename) throws AoException,
            IOException, ConvertException {
        ApplicationStructure as = ieMea.getApplicationElement().getApplicationStructure();

        ApplicationElement aeTst = as.getElementByName("tst");
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeMeq = as.getElementByName("meq");
        ApplicationElement aeSm = as.getElementByName("sm");
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeEc = as.getElementByName("ec");
        ApplicationRelation relMeaTst = as.getRelations(aeMea, aeTst)[0];
        ApplicationRelation relSmMea = as.getRelations(aeSm, aeMea)[0];
        // many relations from AoLocalColumn to AoSubMatrix may exist
        ApplicationRelation relSmLc = null;
        for (ApplicationRelation r : as.getRelations(aeSm, aeLc)) {
            if (r.getBaseRelation().getRelationName().equals("local_columns")) {
                relSmLc = r;
                break;
            }
        }
        ApplicationRelation relMeaMeq = as.getRelations(aeMea, aeMeq)[0];
        ApplicationRelation relLcMeq = as.getRelations(aeLc, aeMeq)[0];
        ApplicationRelation relLcEc = as.getRelations(aeLc, aeEc)[0];

        // create 'AoMeasurement' instance (if not yet existing)
        InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
        InstanceElement ieTst = iter.nextOne();
        iter.destroy();

        String meaName = ieMea.getName() + "_lookup";
        iter = ieTst.getRelatedInstancesByRelationship(Relationship.CHILD, meaName);
        InstanceElement ieMeaTable = null;
        if (iter.getCount() > 0) {
            ieMeaTable = iter.nextOne();
        } else {
            ieMeaTable = aeMea.createInstance(meaName);
            ieMeaTable.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aomeasurement.lookup"));
            ieMeaTable.setValue(ieMea.getValue("date_created"));
            ieMeaTable.setValue(ieMea.getValue("mea_begin"));
            ieMeaTable.setValue(ieMea.getValue("mea_end"));
            ieMeaTable.createRelation(relMeaTst, ieTst);
        }
        iter.destroy();

        // create 'AoSubMatrix' instance
        String lcName = ieLc.getName();
        InstanceElement ieSm = aeSm.createInstance(lcName);
        ieSm.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aosubmatrix.lookup.value_to_text"));
        ieSm.setValue(ODSHelper.createStringNVU("description", "ASAM-MCD2 text table"));
        ieSm.setValue(ODSHelper.createLongNVU("number_of_rows", ccBlock.getNoOfValuePairsForFormula()));
        ieSm.createRelation(relSmMea, ieMeaTable);

        // create 'AoLocalColumn' instance for key
        InstanceElement ieLcKey = aeLc.createInstance(lcName + "_key");
        ieLcKey.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aolocalcolumn.lookup.key"));
        ieLcKey.setValue(ODSHelper.createEnumNVU("seq_rep", 7)); // external_component
        ieLcKey.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLcKey.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLcKey.setValue(ODSHelper.createEnumNVU("axistype", 0));
        ieLcKey.setValue(ODSHelper.createEnumNVU("raw_datatype", 7));
        ieSm.createRelation(relSmLc, ieLcKey);

        // create 'AoExternalComponent' instance for key (referencing into MDF3)
        InstanceElement ieEcKey = aeEc.createInstance("extcomp");
        ieEcKey.setValue(ODSHelper.createStringNVU("filename_url", sourceFile.getName()));
        ieEcKey.setValue(ODSHelper.createEnumNVU("type", 6)); // ieeefloat8
        ieEcKey.setValue(ODSHelper.createLongLongNVU("sofs", cnBlock.getLnkCcBlock() + 46));
        ieEcKey.setValue(ODSHelper.createLongNVU("length", ccBlock.getNoOfValuePairsForFormula()));
        ieEcKey.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEcKey.setValue(ODSHelper.createLongNVU("blocksize", 40)); // 8 key, 32 value
        ieEcKey.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieLcKey.createRelation(relLcEc, ieEcKey);

        // create 'AoMeasurementQuantity' instance for key
        InstanceElement ieMeqKey = aeMeq.createInstance(lcName + "_key");
        ieMeqKey.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aomeasurementquantity.lookup.key"));
        ieMeqKey.setValue(ODSHelper.createStringNVU("description", "Keys for ASAM-MCD2 text table"));
        ieMeqKey.setValue(ODSHelper.createEnumNVU("dt", 7));
        ieMeaTable.createRelation(relMeaMeq, ieMeqKey);
        ieLcKey.createRelation(relLcMeq, ieMeqKey);

        // create 'AoLocalColumn' instance for values
        InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
        ieLcValues.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aolocalcolumn.lookup.value"));
        ieLcValues.setValue(ODSHelper.createEnumNVU("seq_rep", 7));
        ieLcValues.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLcValues.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLcValues.setValue(ODSHelper.createEnumNVU("axistype", 1));
        ieLcValues.setValue(ODSHelper.createEnumNVU("raw_datatype", 1));
        ieSm.createRelation(relSmLc, ieLcValues);

        // create 'AoExternalComponent' instance for values
        long writePos = binChannel.position();
        int length = 0;
        for (int i = 0; i < ccBlock.getNoOfValuePairsForFormula(); i++) {
            String text = ccBlock.getValuesForTextTable()[i] + '\0';
            byte[] bytes = text.getBytes();
            length += bytes.length;
            binChannel.write(ByteBuffer.wrap(bytes));
        }

        // create 'AoExternalComponent' instance for text
        InstanceElement ieEcValues = aeEc.createInstance("extcomp");
        ieEcValues.setValue(ODSHelper.createStringNVU("filename_url", dataFilename));
        ieEcValues.setValue(ODSHelper.createEnumNVU("type", 12));
        ieEcValues.setValue(ODSHelper.createLongLongNVU("sofs", writePos));
        ieEcValues.setValue(ODSHelper.createLongNVU("length", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("blocksize", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("valperblock", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieLcValues.createRelation(relLcEc, ieEcValues);

        // create 'AoMeasurementQuantity' instance for text
        InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
        ieMeqValues.setValue(ODSHelper.createStringNVU("mimetype",
                                                       "application/x-asam.aomeasurementquantity.lookup.value"));
        ieMeqValues.setValue(ODSHelper.createStringNVU("description", "Values for ASAM-MCD2 Text Table"));
        ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1));
        ieMeaTable.createRelation(relMeaMeq, ieMeqValues);
        ieLcValues.createRelation(relLcMeq, ieMeqValues);
    }

    public static void createMCD2TextRangeTableMeasurement(InstanceElement ieMea, InstanceElement ieLc,
            CNBLOCK cnBlock, CCBLOCK ccBlock, File sourceFile, FileChannel binChannel, String dataFilename)
            throws AoException, IOException, ConvertException {
        ApplicationStructure as = ieMea.getApplicationElement().getApplicationStructure();

        ApplicationElement aeTst = as.getElementByName("tst");
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeMeq = as.getElementByName("meq");
        ApplicationElement aeSm = as.getElementByName("sm");
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeEc = as.getElementByName("ec");
        ApplicationElement aeParamset = as.getElementByName("paramset");
        ApplicationElement aeParam = as.getElementByName("param");

        ApplicationRelation relMeaTst = as.getRelations(aeMea, aeTst)[0];
        ApplicationRelation relSmMea = as.getRelations(aeSm, aeMea)[0];
        // many relations from AoLocalColumn to AoSubMatrix may exist
        ApplicationRelation relSmLc = null;
        for (ApplicationRelation r : as.getRelations(aeSm, aeLc)) {
            if (r.getBaseRelation().getRelationName().equals("local_columns")) {
                relSmLc = r;
                break;
            }
        }
        ApplicationRelation relSmParamset = as.getRelations(aeSm, aeParamset)[0];
        ApplicationRelation relMeaMeq = as.getRelations(aeMea, aeMeq)[0];
        ApplicationRelation relLcMeq = as.getRelations(aeLc, aeMeq)[0];
        ApplicationRelation relLcEc = as.getRelations(aeLc, aeEc)[0];
        ApplicationRelation relParamsetParam = as.getRelations(aeParamset, aeParam)[0];

        // create 'AoMeasurement' instance (if not yet existing)
        InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
        InstanceElement ieTst = iter.nextOne();
        iter.destroy();

        String meaName = ieMea.getName() + "_lookup";
        iter = ieTst.getRelatedInstancesByRelationship(Relationship.CHILD, meaName);
        InstanceElement ieMeaTable = null;
        if (iter.getCount() > 0) {
            ieMeaTable = iter.nextOne();
        } else {
            ieMeaTable = aeMea.createInstance(meaName);
            ieMeaTable.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aomeasurement.lookup"));
            ieMeaTable.setValue(ieMea.getValue("date_created"));
            ieMeaTable.setValue(ieMea.getValue("mea_begin"));
            ieMeaTable.setValue(ieMea.getValue("mea_end"));
            ieMeaTable.createRelation(relMeaTst, ieTst);
        }
        iter.destroy();

        // create 'AoSubMatrix' instance
        String lcName = ieLc.getName();
        InstanceElement ieSm = aeSm.createInstance(lcName);
        ieSm.setValue(ODSHelper.createStringNVU("mimetype",
                                                "application/x-asam.aosubmatrix.lookup.value_range_to_value"));
        ieSm.setValue(ODSHelper.createStringNVU("description", "ASAM-MCD2 text range table"));
        ieSm.setValue(ODSHelper.createLongNVU("number_of_rows", ccBlock.getNoOfValuePairsForFormula() - 1));
        ieSm.createRelation(relSmMea, ieMeaTable);

        // create 'AoParameterSet' for default value
        String defaultValue = ccBlock.getDefaultTextForTextRangeTable();
        if (defaultValue != null && defaultValue.length() > 0) {
            InstanceElement ieParamSet = aeParamset.createInstance("basic");
            ieParamSet.setValue(ODSHelper.createStringNVU("iversion",
                                                          String.valueOf(ODSHelper.asJLong(ieParamSet.getId()))));
            ieSm.createRelation(relSmParamset, ieParamSet);
            InstanceElement ieParam = aeParam.createInstance("default_value");
            ieParam.setValue(ODSHelper.createEnumNVU("datatype", 1));
            ieParam.setValue(ODSHelper.createStringNVU("value", ccBlock.getDefaultTextForTextRangeTable()));
            ieParamSet.createRelation(relParamsetParam, ieParam);
        }

        // create 'AoLocalColumn' instance for key min
        InstanceElement ieLcKeyMin = aeLc.createInstance(lcName + "_key_min");
        ieLcKeyMin.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aolocalcolumn.lookup.key_min"));
        ieLcKeyMin.setValue(ODSHelper.createEnumNVU("seq_rep", 7)); // external_component
        ieLcKeyMin.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLcKeyMin.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLcKeyMin.setValue(ODSHelper.createEnumNVU("axistype", 0));
        ieLcKeyMin.setValue(ODSHelper.createEnumNVU("raw_datatype", 7));
        ieSm.createRelation(relSmLc, ieLcKeyMin);

        // create 'AoExternalComponent' instance for key min (referencing into MDF3)
        InstanceElement ieEcKeyMin = aeEc.createInstance("extcomp");
        ieEcKeyMin.setValue(ODSHelper.createStringNVU("filename_url", sourceFile.getName()));
        ieEcKeyMin.setValue(ODSHelper.createEnumNVU("type", 6)); // ieeefloat8
        ieEcKeyMin.setValue(ODSHelper.createLongLongNVU("sofs", cnBlock.getLnkCcBlock() + 66));
        ieEcKeyMin.setValue(ODSHelper.createLongNVU("length", ccBlock.getNoOfValuePairsForFormula() - 1));
        ieEcKeyMin.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEcKeyMin.setValue(ODSHelper.createLongNVU("blocksize", 20)); // 8 byte min key, 8 byte max key, 4 value
        ieEcKeyMin.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieLcKeyMin.createRelation(relLcEc, ieEcKeyMin);

        // create 'AoMeasurementQuantity' instance for key min
        InstanceElement ieMeqKeyMin = aeMeq.createInstance(lcName + "_key_min");
        ieMeqKeyMin.setValue(ODSHelper.createStringNVU("mimetype",
                                                       "application/x-asam.aomeasurementquantity.lookup.key_min"));
        ieMeqKeyMin.setValue(ODSHelper.createStringNVU("description", "Lower range keys for ASAM-MCD2 text table"));
        ieMeqKeyMin.setValue(ODSHelper.createEnumNVU("dt", 7));
        ieMeaTable.createRelation(relMeaMeq, ieMeqKeyMin);
        ieLcKeyMin.createRelation(relLcMeq, ieMeqKeyMin);

        // create 'AoLocalColumn' instance for key max
        InstanceElement ieLcKeyMax = aeLc.createInstance(lcName + "_key_max");
        ieLcKeyMax.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aolocalcolumn.lookup.key_max"));
        ieLcKeyMax.setValue(ODSHelper.createEnumNVU("seq_rep", 7)); // external_component
        ieLcKeyMax.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLcKeyMax.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLcKeyMax.setValue(ODSHelper.createEnumNVU("axistype", 0));
        ieLcKeyMax.setValue(ODSHelper.createEnumNVU("raw_datatype", 7));
        ieSm.createRelation(relSmLc, ieLcKeyMax);

        // create 'AoExternalComponent' instance for max min (referencing into MDF3)
        InstanceElement ieEcKeyMax = aeEc.createInstance("extcomp");
        ieEcKeyMax.setValue(ODSHelper.createStringNVU("filename_url", sourceFile.getName()));
        ieEcKeyMax.setValue(ODSHelper.createEnumNVU("type", 6)); // ieeefloat8
        ieEcKeyMax.setValue(ODSHelper.createLongLongNVU("sofs", cnBlock.getLnkCcBlock() + 66));
        ieEcKeyMax.setValue(ODSHelper.createLongNVU("length", ccBlock.getNoOfValuePairsForFormula() - 1));
        ieEcKeyMax.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEcKeyMax.setValue(ODSHelper.createLongNVU("blocksize", 20)); // 8 byte min key, 8 byte max key, 4 value
        ieEcKeyMax.setValue(ODSHelper.createLongNVU("valoffset", 8));
        ieLcKeyMax.createRelation(relLcEc, ieEcKeyMax);

        // create 'AoMeasurementQuantity' instance for key max
        InstanceElement ieMeqKeyMax = aeMeq.createInstance(lcName + "_key_max");
        ieMeqKeyMax.setValue(ODSHelper.createStringNVU("mimetype",
                                                       "application/x-asam.aomeasurementquantity.lookup.key_max"));
        ieMeqKeyMax.setValue(ODSHelper.createStringNVU("description", "Upper range keys for ASAM-MCD2 text table"));
        ieMeqKeyMax.setValue(ODSHelper.createEnumNVU("dt", 7));
        ieMeaTable.createRelation(relMeaMeq, ieMeqKeyMax);
        ieLcKeyMax.createRelation(relLcMeq, ieMeqKeyMax);

        // create 'AoLocalColumn' instance for values
        InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
        ieLcValues.setValue(ODSHelper.createStringNVU("mimetype", "application/x-asam.aolocalcolumn.lookup.value"));
        ieLcValues.setValue(ODSHelper.createEnumNVU("seq_rep", 7));
        ieLcValues.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLcValues.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLcValues.setValue(ODSHelper.createEnumNVU("axistype", 1));
        ieLcValues.setValue(ODSHelper.createEnumNVU("raw_datatype", 1));
        ieSm.createRelation(relSmLc, ieLcValues);

        // create 'AoExternalComponent' instance for values
        long writePos = binChannel.position();
        int length = 0;
        for (int i = 0; i < ccBlock.getNoOfValuePairsForFormula() - 1; i++) {
            String text = ccBlock.getValuesForTextRangeTable()[i] + '\0';
            byte[] bytes = text.getBytes();
            length += bytes.length;
            binChannel.write(ByteBuffer.wrap(bytes));
        }

        // create 'AoExternalComponent' instance for text
        InstanceElement ieEcValues = aeEc.createInstance("extcomp");
        ieEcValues.setValue(ODSHelper.createStringNVU("filename_url", dataFilename));
        ieEcValues.setValue(ODSHelper.createEnumNVU("type", 12));
        ieEcValues.setValue(ODSHelper.createLongLongNVU("sofs", writePos));
        ieEcValues.setValue(ODSHelper.createLongNVU("length", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("blocksize", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("valperblock", length));
        ieEcValues.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieLcValues.createRelation(relLcEc, ieEcValues);

        // create 'AoMeasurementQuantity' instance for value
        InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
        ieMeqValues.setValue(ODSHelper.createStringNVU("mimetype",
                                                       "application/x-asam.aomeasurementquantity.lookup.value"));
        ieMeqValues.setValue(ODSHelper.createStringNVU("description", "Values for ASAM-MCD2 Text Table"));
        ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1));
        ieMeaTable.createRelation(relMeaMeq, ieMeqValues);
        ieLcValues.createRelation(relLcMeq, ieMeqValues);
    }

    public static void createMCD2TextTableMeaQuantities(InstanceElement ieMea, InstanceElement ieSm,
            ByteBuffer recordBb, FileChannel binChannel, String meqName, DGBLOCK dgBlock, CGBLOCK cgBlock,
            CNBLOCK cnBlock, CCBLOCK ccBlock, String dataFilename) throws AoException, IOException, ConvertException {
        long writePos = binChannel.position();

        ApplicationStructure as = ieSm.getApplicationElement().getApplicationStructure();
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeMeq = as.getElementByName("meq");
        ApplicationElement aeSm = as.getElementByName("sm");
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeEc = as.getElementByName("ec");
        // many relations from AoLocalColumn to AoSubMatrix may exist
        ApplicationRelation relSmLc = null;
        for (ApplicationRelation r : as.getRelations(aeSm, aeLc)) {
            if (r.getBaseRelation().getRelationName().equals("local_columns")) {
                relSmLc = r;
                break;
            }
        }
        ApplicationRelation relMeaMeq = as.getRelations(aeMea, aeMeq)[0];
        ApplicationRelation relLcMeq = as.getRelations(aeLc, aeMeq)[0];
        ApplicationRelation relLcEc = as.getRelations(aeLc, aeEc)[0];

        // create 'AoLocalColumn' instance for text
        String meqNameTxt = meqName + "_text";
        InstanceElement ieLc = aeLc.createInstance(meqNameTxt);
        ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 7));
        ieLc.setValue(ODSHelper.createShortNVU("idp", (short) 0));
        ieLc.setValue(ODSHelper.createShortNVU("global", (short) 15));
        ieLc.setValue(ODSHelper.createEnumNVU("axistype", 1));
        ieLc.setValue(ODSHelper.createEnumNVU("raw_datatype", 1));
        ieSm.createRelation(relSmLc, ieLc);

        // create 'AoMeasurementQuantity' instance for text
        InstanceElement ieMeq = aeMeq.createInstance(meqNameTxt);
        ieMeq.setValue(ODSHelper.createStringNVU("description", cnBlock.getSignalDescription().trim()));
        ieMeq.setValue(ODSHelper.createEnumNVU("dt", 1));
        ieMea.createRelation(relMeaMeq, ieMeq);
        ieLc.createRelation(relLcMeq, ieMeq);

        // create map for text table
        Map<Double, String> map = new HashMap<Double, String>();
        for (int i = 0; i < ccBlock.getNoOfValuePairsForFormula(); i++) {
            map.put(ccBlock.getKeysForTextTable()[i], ccBlock.getValuesForTextTable()[i]);
        }

        // read enum values, perform mapping and write text to binary file
        ByteBuffer bb = Mdf3Util.readNumberValues(recordBb, dgBlock, cgBlock, cnBlock);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int length = 0;
        for (int i = 0; i < cgBlock.getNoOfRecords(); i++) {
            double enumValue = -1;
            if (cnBlock.getSignalDataType() == 0 && cnBlock.getNumberOfBits() == 16) {
                enumValue = Mdf3Util.readUInt16(bb);
            } else if (cnBlock.getSignalDataType() == 0 && cnBlock.getNumberOfBits() == 8) {
                enumValue = bb.get();
            } else if (cnBlock.getNumberOfBits() == 1) {
                boolean b = Mdf3Util.getBit(new byte[] { bb.get() }, 0);
                enumValue = b ? 1 : 0;
            } else {
                throw new ConvertException("Unsupported value datatype for MCD text table: " + cnBlock);
            }

            String text = map.get(enumValue);
            text = (text == null) ? String.valueOf(enumValue) : text.trim();
            text = text + '\0';

            byte[] bytes = text.getBytes();
            length += bytes.length;
            binChannel.write(ByteBuffer.wrap(bytes));
        }

        // create 'AoExternalComponent' instance for text
        InstanceElement ieEc = aeEc.createInstance("extcomp");
        ieEc.setValue(ODSHelper.createStringNVU("filename_url", dataFilename));
        ieEc.setValue(ODSHelper.createEnumNVU("type", 12));
        ieEc.setValue(ODSHelper.createLongLongNVU("sofs", writePos));
        ieEc.setValue(ODSHelper.createLongNVU("length", length));
        ieEc.setValue(ODSHelper.createLongNVU("blocksize", length));
        ieEc.setValue(ODSHelper.createLongNVU("valperblock", length));
        ieEc.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieLc.createRelation(relLcEc, ieEc);
    }
}
