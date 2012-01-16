package de.rechner.openatfx.main;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.SeverityFlag;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class for cleanup DIAdem ATFX files. This includes
 * <ul>
 * <li>Converting the string in the instance attribute</li>
 * </ul>
 * 
 * @author FGAAW8T
 */
public abstract class DIAdemHelper {

    private static final Log LOG = LogFactory.getLog(DIAdemHelper.class);

    private static final String BE_MEASUREMENT = "AoMeasurement";
    private static final String BE_MEASUREMENT_QUANTITY = "AoMeasurementQuantity";
    private static final String BE_SUBMATRIX = "AoSubMatrix";
    private static final String BE_LOCALCOLUMN = "AoLocalColumn";
    private static final String BE_UNIT = "AoUnit";
    private static final String BA_NUMBER_OF_ROWS = "number_of_rows";
    private static final String BR_SUBMATRICES = "submatrices";
    private static final String BR_LOCAL_COLUMNS = "local_columns";
    private static final String BR_UNIT = "unit";

    private static final String IA_UNIT = "unit_string"; // unit instance
                                                         // attribute

    private static final String AA_START_DATE = "Startdatum";
    private static final String AA_START_TIME = "Startzeit";
    private static final String AA_END_DATE = "Enddatum";
    private static final String AA_END_TIME = "Endzeit";
    private static final String DATEFORMAT = "dd.MM.yyyy HH:mm:ss";

    /**
     * Parse the measurement begin date from the specific instance attributes 'Startdatum' and 'Startzeit'.
     * 
     * @param ieMea The measurement instance element.
     * @return The measurement begin ODS date.
     * @throws AoException Error retrieving instance attributes.
     */
    public static String getMeaBegin(InstanceElement ieMea) throws AoException {
        String startDate = "";
        String startTime = "";
        for (NameValueUnit nvu : ieMea.getValueSeq(ieMea.listAttributes("*", AttrType.INSTATTR_ONLY))) {
            if (nvu.valName.equals(AA_START_DATE)) {
                startDate = ODSHelper.getStringVal(nvu).trim();
            } else if (nvu.valName.equals(AA_START_TIME)) {
                startTime = ODSHelper.getStringVal(nvu).trim();
            }
        }
        String startDateTime = startDate + " " + startTime;
        if (startDateTime.length() > 1) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
            try {
                Date date = sdf.parse(startDateTime);
                return ODSHelper.asODSDate(date);
            } catch (ParseException e) {
                LOG.warn(e.getMessage());
            }
        }
        return "";
    }

    /**
     * Parse the measurement begin date from the specific instance attributes 'Enddatum' and 'Endzeit'.
     * 
     * @param ieMea The measurement instance element.
     * @return The measurement end ODS date.
     * @throws AoException Error retrieving instance attributes.
     */
    public static String getMeaEnd(InstanceElement ieMea) throws AoException {
        String endDate = "";
        String endTime = "";
        for (NameValueUnit nvu : ieMea.getValueSeq(ieMea.listAttributes("*", AttrType.INSTATTR_ONLY))) {
            if (nvu.valName.equals(AA_END_DATE)) {
                endDate = ODSHelper.getStringVal(nvu).trim();
            } else if (nvu.valName.equals(AA_END_TIME)) {
                endTime = ODSHelper.getStringVal(nvu).trim();
            }
        }
        String endDateTime = endDate + " " + endTime;
        if (endDateTime.length() > 1) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
            try {
                Date date = sdf.parse(endDateTime);
                return ODSHelper.asODSDate(date);
            } catch (ParseException e) {
                LOG.warn(e.getMessage());
            }
        }
        return "";
    }

    /**
     * Creates 'real' AoUnit instances from the DIAdem unit strings stored in the instance attributes of
     * "AoMeasurementQuantity".
     * 
     * @param aoSession The session.
     * @throws AoException Error creating units.
     */
    public static void createUnitInstances(AoSession aoSession) throws AoException {
        long start = System.currentTimeMillis();

        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement aeMeaQua = getAeByType(as, BE_MEASUREMENT_QUANTITY);
        ApplicationElement aeUnit = getAeByType(as, BE_UNIT);
        ApplicationRelation relMeaQuaUnit = getApplRelByBaseName(as, aeMeaQua, aeUnit, BR_UNIT);

        // cache existing unit instances
        Map<String, InstanceElement> ieUnitMap = new HashMap<String, InstanceElement>();
        InstanceElementIterator iterUnits = aeUnit.getInstances("*");
        for (InstanceElement ieUnit : iterUnits.nextN(iterUnits.getCount())) {
            ieUnitMap.put(ieUnit.getName(), ieUnit);
        }
        iterUnits.destroy();

        // iterate of all instances of 'AoMeasurementQuantity'
        InstanceElementIterator iterMeaQua = aeMeaQua.getInstances("*");
        for (InstanceElement ieMeaQua : iterMeaQua.nextN(iterMeaQua.getCount())) {
            // query for DIAdem unit instance attribute
            if (ieMeaQua.listAttributes(IA_UNIT, AttrType.ALL).length < 1) {
                continue;
            }

            for (NameValueUnit nvu : ieMeaQua.getValueSeq(new String[] { IA_UNIT })) {
                if ((nvu.value.flag != 15) || (nvu.value.u.discriminator() != DataType.DT_STRING)) {
                    continue;
                }
                String unitName = nvu.value.u.stringVal();
                // check if unit instance exists, if not - create
                InstanceElement ieUnit = ieUnitMap.get(unitName);
                if (ieUnit == null) {
                    ieUnit = aeUnit.createInstance(unitName);
                    ieUnitMap.put(unitName, ieUnit);
                }
                ieMeaQua.createRelation(relMeaQuaUnit, ieUnit);
                ieUnit.destroy();
            }

            ieMeaQua.destroy();
        }
        iterMeaQua.destroy();

        LOG.info("Created DIAdem unit instances in " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * @param odsCache
     * @throws AoException
     */
    public static void mergeSubMatrices(AoSession aoSession) throws AoException {
        long start = System.currentTimeMillis();

        ApplicationStructure as = aoSession.getApplicationStructure();
        ApplicationElement aeMea = getAeByType(as, BE_MEASUREMENT);

        // iterate of all instances of 'AoMeasurement'
        InstanceElementIterator iter = aeMea.getInstances("*");
        for (InstanceElement ieMea : iter.nextN(iter.getCount())) {
            mergeSubMatrices(as, ieMea);
            ieMea.destroy();
        }
        iter.destroy();

        LOG.info("Merged DIAdem submatrices instances in " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Merge the SubMatrices for given AoMeasurement instance.
     * 
     * @param ieMea The instance element of type AoMeasurement.
     * @throws AoException Error merging instances.
     */
    private static void mergeSubMatrices(ApplicationStructure as, InstanceElement ieMea) throws AoException {
        ApplicationElement aeMeasurement = getAeByType(as, BE_MEASUREMENT);
        ApplicationElement aeSubMatrix = getAeByType(as, BE_SUBMATRIX);
        ApplicationElement aeLocalColumn = getAeByType(as, BE_LOCALCOLUMN);
        ApplicationRelation relMeaSubMatrix = getApplRelByBaseName(as, aeMeasurement, aeSubMatrix, BR_SUBMATRICES);
        ApplicationRelation rel = getApplRelByBaseName(as, aeSubMatrix, aeLocalColumn, BR_LOCAL_COLUMNS);

        // iterate over all instances of 'AoSubmatrix'
        Map<Integer, InstanceElement> subMatrixMap = new HashMap<Integer, InstanceElement>(); // key=noOfRows
        InstanceElementIterator iterSM = ieMea.getRelatedInstances(relMeaSubMatrix, "*");
        for (InstanceElement ieSubmatrix : iterSM.nextN(iterSM.getCount())) {
            int noOfRows = ieSubmatrix.getValueByBaseName(BA_NUMBER_OF_ROWS).value.u.longVal();

            // find or create SubMatrix
            InstanceElement newIeSubMatrix = subMatrixMap.get(noOfRows);
            if (newIeSubMatrix == null) {
                ieSubmatrix.setName("SubMatrix#" + (subMatrixMap.size() + 1));
                subMatrixMap.put(noOfRows, ieSubmatrix);
                ieSubmatrix.destroy();
                continue;
            }
            // change relation
            else {
                InstanceElementIterator iterLC = ieSubmatrix.getRelatedInstances(rel, "*");
                for (InstanceElement ieLC : iterLC.nextN(iterLC.getCount())) {
                    ieSubmatrix.removeRelation(rel, ieLC);
                    newIeSubMatrix.createRelation(rel, ieLC);
                }
                iterLC.destroy();

                // delete SubMatrix
                aeSubMatrix.removeInstance(ieSubmatrix.getId(), true);
            }

        }
        iterSM.destroy();
    }

    /**
     * Returns an application element by given base type. If none or multiple application elements of this type are
     * found, an exception is thrown.
     * 
     * @param as The application structure.
     * @param bType The base element name.
     * @return The application element.
     * @throws AoException Error accessing application model or application element not found.
     */
    private static ApplicationElement getAeByType(ApplicationStructure as, String bType) throws AoException {
        ApplicationElement[] aes = as.getElementsByBaseType(bType);
        if (aes.length != 1) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                  "None or multiple application elements of type '" + bType + "' found");
        }
        return aes[0];
    }

    /**
     * Returns an application relation between two application elements having given base name.
     * 
     * @param as The application structure.
     * @param elem1 The first application element.
     * @param elem2 The second application element.
     * @param bRelName The base relation name.
     * @return The application relation.
     * @throws AoException Error accessing application model or relation not found.
     */
    private static ApplicationRelation getApplRelByBaseName(ApplicationStructure as, ApplicationElement elem1,
            ApplicationElement elem2, String bRelName) throws AoException {
        for (ApplicationRelation rel : as.getRelations(elem1, elem2)) {
            if (rel.getBaseRelation().getRelationName().equals(bRelName)) {
                return rel;
            }
        }
        throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "No application relation found between '"
                + elem1.getName() + "' and '" + elem2.getName() + "' with base name '" + bRelName + "'");
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        ORB orb = ORB.init(new String[0], System.getProperties());
        String file = "D:/PUBLIC/test.atfx";
        try {
            AoSession aoSession = AoServiceFactory.getInstance().newAoFactory(orb)
                                                  .newSession("FILENAME=" + new File(file));
            DIAdemHelper.createUnitInstances(aoSession);
            DIAdemHelper.mergeSubMatrices(aoSession);
            DIAdemHelper.mergeSubMatrices(aoSession);
            aoSession.commitTransaction();
        } catch (AoException aoe) {
            System.err.println(aoe.reason);
        }
    }

}
