package de.rechner.openatfx.avro.converter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.log4j.BasicConfigurator;

import de.rechner.openatfx_avro.TimeSeries;
import de.rechner.openatfx_avro.TimeSeriesValue;


public class Test {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        TimeSeries ts = new TimeSeries();
        ts.setName("Drehzahl");
        ts.setUnit("1/min");
        ts.setQuantity("");
        List<TimeSeriesValue> values = new ArrayList<>();
        for (int i = 0; i < 10000000; i++) {
            TimeSeriesValue value = new TimeSeriesValue();
            value.setRelTime((long) i);
            value.setTimestamp(new Date().getTime() + i);
            values.add(value);
        }
        ts.setVals(values);

        // ParquetWriter<Object> writer;
        // try {
        // writer = AvroParquetWriter.builder(new Path("file:///D:/PUBLIC/test.parquet")).withSchema(ts.getSchema())
        // .build();
        // writer.write(ts);
        // writer.close();
        // } catch (IllegalArgumentException | IOException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }

        DatumWriter<TimeSeries> userDatumWriter = new SpecificDatumWriter<TimeSeries>(TimeSeries.class);
        DataFileWriter<TimeSeries> dataFileWriter = new DataFileWriter<TimeSeries>(userDatumWriter);
        try {
            dataFileWriter.create(ts.getSchema(), new File("timeseries.avro"));
            dataFileWriter.append(ts);
            dataFileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}