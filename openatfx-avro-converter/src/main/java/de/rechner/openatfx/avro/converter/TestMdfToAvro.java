package de.rechner.openatfx.avro.converter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.Column;
import org.asam.ods.DataType;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrix;
import org.omg.CORBA.ORB;

import de.rechner.openatfx_avro.TimeSeries;
import de.rechner.openatfx_avro.TimeSeriesValue;
import de.rechner.openatfx_mdf.ConvertException;
import de.rechner.openatfx_mdf.MDFConverter;
import de.rechner.openatfx_mdf.util.ODSHelper;


public class TestMdfToAvro {

    public static void main(String[] args) {
        AoSession aoSession = null;
        try {
            BasicConfigurator.configure();
            ORB orb = ORB.init(new String[0], System.getProperties());
            Path mdfFile = Paths.get("D:/PUBLIC/test/test.MDF");

            MDFConverter converter = new MDFConverter();
            aoSession = converter.getAoSessionForMDF(orb, mdfFile);

            ApplicationElement aeSm = aoSession.getApplicationStructure().getElementsByBaseType("AoSubMatrix")[0];
            InstanceElementIterator iter = aeSm.getInstances("*");
            for (int i = 0; i < iter.getCount(); i++) {
                InstanceElement ieSm = iter.nextOne();

                // skip lookup tables
                String mimeType = ieSm.getValueByBaseName("mime_type").value.u.stringVal();
                if (mimeType.startsWith("application/x-asam.aosubmatrix.lookup")) {
                    continue;
                }

                SubMatrix sm = ieSm.upcastSubMatrix();
                ValueMatrix vm = sm.getValueMatrix();

                // get time channel
                Column[] timeColumns = vm.getIndependentColumns("*");
                if (timeColumns.length != 1) {
                    throw new ConvertException("None or multiple independent channels found: " + sm.getAsamPath());
                }
                Column timeColumn = timeColumns[0];

                // read other values
                for (Column col : vm.getColumns("*")) {
                    TimeSeries timeSeries = new TimeSeries();
                    timeSeries.setName(col.getName());
                    timeSeries.setUnit(col.getUnit());
                    TS_ValueSeq valueSeq = vm.getValueVector(col, 0, 0);
                    List<TimeSeriesValue> tsvList = tsValueSeq2TimeSeriesValues(valueSeq);
                    timeSeries.setValues(tsvList);
                    col.destroy();
                }
            }

        } catch (ConvertException e) {
            System.err.println(e.getMessage());
        } catch (AoException e) {
            System.err.println(e.reason);
        }
    }

    private static List<TimeSeriesValue> tsValueSeq2TimeSeriesValues(TS_ValueSeq valuesSeq) {
        List<TimeSeriesValue> list = new ArrayList<TimeSeriesValue>(valuesSeq.flag.length);
        DataType dt = valuesSeq.u.discriminator();
        for (int i = 0; i < valuesSeq.flag.length; i++) {
            TimeSeriesValue tsv = new TimeSeriesValue();
            if (dt == DataType.DT_BOOLEAN) {
                tsv.setNumVal((double) (valuesSeq.u.booleanVal()[i] ? 1 : 0));
            } else if (dt == DataType.DT_BYTE) {
                tsv.setNumVal((double) valuesSeq.u.byteVal()[i]);
            } else if (dt == DataType.DT_SHORT) {
                tsv.setNumVal((double) valuesSeq.u.shortVal()[i]);
            } else if (dt == DataType.DT_LONG) {
                tsv.setNumVal((double) valuesSeq.u.longVal()[i]);
            } else if (dt == DataType.DT_LONGLONG) {
                tsv.setNumVal((double) ODSHelper.asJLong(valuesSeq.u.longlongVal()[i]));
            } else if (dt == DataType.DT_FLOAT) {
                tsv.setNumVal((double) valuesSeq.u.floatVal()[i]);
            } else if (dt == DataType.DT_DOUBLE) {
                tsv.setNumVal((double) valuesSeq.u.doubleVal()[i]);
            }
            list.add(tsv);
        }
        return list;
    }

}
