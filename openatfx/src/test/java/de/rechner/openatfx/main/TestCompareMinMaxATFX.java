package de.rechner.openatfx.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.Column;
import org.asam.ods.DataType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_Value;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrix;
import org.asam.ods.ValueMatrixMode;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.util.ODSHelper;


public class TestCompareMinMaxATFX {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        ORB orb = ORB.init(args, System.getProperties());
        File file = new File("D:\\PUBLIC\\test\\test\\test.atfx");
        List<String> failures = new ArrayList<String>();
        int checked = 0;

        try {
            AoSession aoSession = AoServiceFactory.getInstance().newAoSession(orb, file);
            ApplicationStructure as = aoSession.getApplicationStructure();

            ApplicationElement aeSM = as.getElementsByBaseType("AoSubMatrix")[0];
            InstanceElementIterator iter = aeSM.getInstances("*");
            for (int i = 0; i < iter.getCount(); i++) {
                InstanceElement ieSM = iter.nextOne();

                // load min/max
                Map<String, Double> max = new LinkedHashMap<String, Double>();
                InstanceElementIterator iterLc = ieSM.getRelatedInstancesByRelationship(Relationship.CHILD, "*");
                for (int x = 0; x < iterLc.getCount(); x++) {
                    InstanceElement ieLC = iterLc.nextOne();
                    String mt = ODSHelper.getStringVal(ieLC.getValueByBaseName("mime_type"));
                    if (mt != null && !mt.startsWith("application/x-asam.aolocalcolumn.lookup")) {
                        TS_Value v = ieLC.getValueByBaseName("maximum").value;
                        if (v.flag == 15) {
                            max.put(ieLC.getName(), v.u.doubleVal());
                        }
                    }
                }

                // load values
                SubMatrix sm = ieSM.upcastSubMatrix();
                ValueMatrix vm = sm.getValueMatrixInMode(ValueMatrixMode.STORAGE);

                Column[] columns = vm.getColumns("*");
                for (Column col : columns) {
                    TS_ValueSeq valueSeq = vm.getValueVector(col, 0, 0);

                    // check min
                    Map<String, Double> minMaxValues = calculate(valueSeq);
                    Double maxCalc = minMaxValues.get("Maximum");
                    Double maxFile = max.get(col.getName());
                    if (maxCalc != null && maxFile != null) {
                        if (maxCalc.compareTo(maxFile) != 0) {
                            String failure = col.getName() + ": in file stands maximum " + maxFile + ", calculated "
                                    + maxCalc;
                            failures.add(failure);
                            System.err.println(failure);
                        } else {
                            System.err.println(col.getName() + ": ok");
                        }
                        checked++;
                    }

                    // System.out.println(minCalc);
                    // System.out.println(valueSeq2String(valueSeq));

                }
            }

            System.err.println("---------------------------------");
            System.err.println("RESULT: " + failures.size() + " failures of " + checked);
            System.err.println("---------------------------------");
            for (String failure : failures) {
                System.err.println(failure);
            }
        } catch (AoException e) {
            System.err.println(e.reason);
            e.printStackTrace();
        }

        // CHECK MIN/MAX!

    }

    // private static String valueSeq2String(TS_ValueSeq valueSeq) {
    // DataType dt = valueSeq.u.discriminator();
    // if (dt == DataType.DT_BYTE) {
    // return Arrays.toString(valueSeq.u.byteVal());
    // } else if (dt == DataType.DT_SHORT) {
    // return Arrays.toString(valueSeq.u.shortVal());
    // } else if (dt == DataType.DT_LONG) {
    // return Arrays.toString(valueSeq.u.longVal());
    // } else if (dt == DataType.DT_DOUBLE) {
    // return Arrays.toString(valueSeq.u.doubleVal());
    // } else if (dt == DataType.DT_FLOAT) {
    // return Arrays.toString(valueSeq.u.floatVal());
    // } else if (dt == DataType.DT_STRING) {
    // return Arrays.toString(valueSeq.u.stringVal());
    // }
    // return valueSeq.toString();
    // }

    /**
     * Performs the calculation of min/max/avg/dev values on a value vector.
     * 
     * @param valueSeq The value vector
     * @return The calculation result as map with attribute name as key.
     */
    private static Map<String, Double> calculate(TS_ValueSeq valueSeq) {
        if (valueSeq.u.discriminator() == DataType.DT_LONGLONG) {
            return Collections.emptyMap();
        }

        System.out.println("Berechne auf... " + valueSeq.flag.length + " werten des Datentyp: "
                + ODSHelper.dataType2String(valueSeq.u.discriminator()));
        SummaryStatistics stat = new SummaryStatistics();
        DataType dt = valueSeq.u.discriminator();

        // iterate over values
        for (int i = 0; i < valueSeq.flag.length; i++) {
            if (valueSeq.flag[i] == (short) 15) {
                if (dt == DataType.DT_BOOLEAN) {
                    boolean b = valueSeq.u.booleanVal()[i];
                    stat.addValue(b ? 1 : 0);
                } else if (dt == DataType.DT_FLOAT) {
                    stat.addValue(valueSeq.u.floatVal()[i]);
                } else if (dt == DataType.DT_DOUBLE) {
                    stat.addValue(valueSeq.u.doubleVal()[i]);
                } else if (dt == DataType.DT_LONG) {
                    stat.addValue(valueSeq.u.longVal()[i]);
                } else if (dt == DataType.DT_BYTE) {
                    stat.addValue(valueSeq.u.byteVal()[i] & 0xff); // ODS is unsigned!
                } else if (dt == DataType.DT_ENUM) {
                    stat.addValue(valueSeq.u.enumVal()[i]);
                } else if (dt == DataType.DT_SHORT) {
                    stat.addValue(valueSeq.u.shortVal()[i]);
                } else if (dt == DataType.DT_LONGLONG) {
                    stat.addValue(ODSHelper.asJLong(valueSeq.u.longlongVal())[i]);
                }
            }
        }

        // build values map
        Map<String, Double> map = new HashMap<String, Double>(2);
        double min = stat.getMin();
        if (!Double.isNaN(min) && (min != Double.MIN_VALUE) && min != Double.MAX_VALUE) {
            map.put("Minimum", min);
        }
        double max = stat.getMax();
        if (!Double.isNaN(max) && (max != Double.MIN_VALUE) && max != Double.MAX_VALUE) {
            map.put("Maximum", max);
        }

        System.out.println(".... fertig");

        return map;
    }

}
