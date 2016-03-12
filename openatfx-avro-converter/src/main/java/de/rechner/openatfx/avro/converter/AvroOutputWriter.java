package de.rechner.openatfx.avro.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.Column;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.SeverityFlag;
import org.asam.ods.SubMatrix;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.ValueMatrix;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.AoServiceFactory;
import de.rechner.openatfx.avro.TimeSeries;
import de.rechner.openatfx.avro.TimeSeriesValue;
import de.rechner.openatfx_mdf.ConvertException;
import de.rechner.openatfx_mdf.MDFConverter;
import de.rechner.openatfx_mdf.util.ODSHelper;


public class AvroOutputWriter {

    private static final Log LOG = LogFactory.getLog(AvroOutputWriter.class);
    static DatumWriter<TimeSeries> timeSeriesWriter = new SpecificDatumWriter<TimeSeries>(TimeSeries.class);

    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure();
        String directory = args[0];
        if (directory == null) {
            throw new IOException("Parameter directory must be set!");
        }
        ORB orb = ORB.init(new String[0], System.getProperties());
        for (Path inputFile : Files.newDirectoryStream(Paths.get(args[0]))) {
            String filename = inputFile.toString().toLowerCase();
            if (filename.endsWith(".atfx") || filename.endsWith(".dat") || filename.endsWith(".mdf")
                    || filename.endsWith(".mf4")) {
                try {
                    convertFile2Avro(orb, inputFile, inputFile.getParent());
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public static void convertFile2Avro(ORB orb, Path inputFile, Path targetDir) throws IOException {
        AoSession aoSession = null;
        try {
            String filename = inputFile.toString().toLowerCase();
            // input is ATFX file
            if (filename.endsWith(".atfx")) {
                aoSession = AoServiceFactory.getInstance().newAoSession(orb, inputFile.toFile());
            }
            // input is MDF3 or MDF4 file
            else if (filename.endsWith(".dat") || filename.endsWith(".mdf") || filename.endsWith(".mf4")) {
                aoSession = new MDFConverter().getAoSessionForMDF(orb, inputFile);
            } else {
                return;
            }

            // get AoMeasurement instances
            ApplicationElement aeMea = aoSession.getApplicationStructure().getElementsByBaseType("AoMeasurement")[0];
            InstanceElementIterator iter = aeMea.getInstances("*");
            for (int i = 0; i < iter.getCount(); i++) {
                InstanceElement ieMea = iter.nextOne();
                String targetFilename = FilenameUtils.getBaseName(inputFile.toString()) + "_"
                        + ODSHelper.asJLong(ieMea.getId()) + ".avro";
                Path outputFile = Paths.get(targetDir.toString(), targetFilename);
                convertTimeSeries2Avro(ieMea, outputFile);
            }

        } catch (ConvertException e) {
            LOG.error(e.getMessage(), e);
            throw new IOException(e.getMessage());
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new IOException(e.reason);
        } finally {
            if (aoSession != null) {
                try {
                    aoSession.close();
                } catch (AoException e) {
                    LOG.fatal(e.getMessage(), e);
                }
            }
        }
    }

    public static void convertTimeSeries2Avro(InstanceElement ieMea, Path outputFile) throws AoException, IOException {
        DataFileWriter<TimeSeries> dataFileWriter = null;
        try {
            // check input
            if (!ieMea.getApplicationElement().getBaseElement().getType().equalsIgnoreCase("AoMeasurement")) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Only instances of type 'AoMeasurement' are allowed as input: "
                                              + ieMea.getAsamPath());
            }

            // open target file
            dataFileWriter = new DataFileWriter<TimeSeries>(timeSeriesWriter);
            dataFileWriter.create(TimeSeries.getClassSchema(), outputFile.toFile());

            // read data from source
            ApplicationRelation rel = ieMea.getApplicationElement().getRelationsByBaseName("submatrices")[0];
            InstanceElementIterator iter = ieMea.getRelatedInstances(rel, "*");
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
                    throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0,
                                          "None or multiple independent channels found: " + sm.getAsamPath());
                }
                Column timeColumn = timeColumns[0];

                // read other values
                for (Column col : vm.getColumns("*")) {
                    if (col.isIndependent()) { // skip time channels
                        continue;
                    }

                    TimeSeries timeSeries = new TimeSeries();
                    timeSeries.setName(col.getName());
                    timeSeries.setUnit(col.getUnit());
                    TS_ValueSeq valueSeq = vm.getValueVector(col, 0, 0);
                    List<TimeSeriesValue> tsvList = tsValueSeq2TimeSeriesValues(valueSeq);
                    timeSeries.setVals(tsvList);
                    dataFileWriter.append(timeSeries);

                    col.destroy();
                }
            }

        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw e;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (dataFileWriter != null) {
                    dataFileWriter.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private static List<TimeSeriesValue> tsValueSeq2TimeSeriesValues(TS_ValueSeq valuesSeq) {
        List<TimeSeriesValue> list = new ArrayList<TimeSeriesValue>(valuesSeq.flag.length);
        DataType dt = valuesSeq.u.discriminator();
        for (int i = 0; i < valuesSeq.flag.length; i++) {
            TimeSeriesValue tsv = new TimeSeriesValue();
            tsv.setRelTime((long) i);
            tsv.setTimestamp(new Date().getTime());
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
