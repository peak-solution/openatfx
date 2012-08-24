package de.rechner.openatfx.converter.maccor_csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.DataType;
import org.asam.ods.TS_Value;

import de.rechner.openatfx.converter.ConvertException;
import de.rechner.openatfx.util.ODSHelper;


class MaccorCSVReader {

    private static final Log LOG = LogFactory.getLog(MaccorCSVReader.class);

    private static final char CSV_SEPARATOR = ';';
    private static final int CHANNEL_NAME_LINE = 15;
    private static final String DEFAULT_UNIT = "-";
    private static final String REL_TIME_UNIT = "s";

    private final Pattern channelNamePattern = Pattern.compile("(.*)\\s\\[(.*)\\]");
    private final Pattern relativeTimePattern = Pattern.compile("\\d*d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d\\d\\d");
    private final Pattern absoluteTimePattern = Pattern.compile("\\d*/\\d*/\\d* \\d*:\\d*");
    private final NumberFormat intFormat;
    private final NumberFormat floatFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

    private final List<MaccorChannelHeader> channelHeader;

    private final CSVReader csvReader;
    private String[] lastReadLine = null;
    private int lastReadStep = 1;

    public MaccorCSVReader(File csvFile) throws ConvertException {
        this.intFormat = NumberFormat.getIntegerInstance(Locale.ENGLISH);

        // read channel header
        this.channelHeader = readChannelHeader(csvFile);
        // open CSV reader for measurement data
        try {
            this.csvReader = new CSVReader(new FileReader(csvFile), CSV_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER,
                                           CHANNEL_NAME_LINE + 1);
            this.lastReadLine = this.csvReader.readNext();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

    public List<MaccorChannelHeader> getChannelHeader() {
        return channelHeader;
    }

    public Map<String, Object[]> readNextStepDataBlock() throws ConvertException {
        try {
            Map<String, Object[]> map = new HashMap<String, Object[]>();

            while (lastReadLine != null) {
                Map<String, TS_Value> dataLine = parseDataLine(lastReadLine);
                TS_Value stepValue = dataLine.get("Step");
                if (stepValue == null) {
                    throw new ConvertException("Column 'Step' not found");
                }

                // step to next line
                lastReadLine = this.csvReader.readNext();
            }

            return map;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

    /**
     * Maps a data line values to the corresponding
     * 
     * @param line
     * @return
     * @throws ConvertException
     */
    private Map<String, TS_Value> parseDataLine(String[] line) throws ConvertException {
        Map<String, TS_Value> map = new HashMap<String, TS_Value>(1);
        // iterate throw columns of row
        for (int i = 0; i < this.channelHeader.size(); i++) {
            // check if line has enough entries
            if (i > line.length) {
                throw new ConvertException("Missing value in data row: " + Arrays.toString(line));
            }
            MaccorChannelHeader channelHeader = this.channelHeader.get(i);
            map.put(channelHeader.getChannelName(), parseValue(line[i], channelHeader.getDataType()));
        }
        return map;
    }

    /**
     * Reads the channel names from the CSV file. The units will not be included in the channel names.
     * 
     * @param csvFile The CSV file to read.
     * @return List of channel header information.
     * @throws IOException
     */
    private List<MaccorChannelHeader> readChannelHeader(File csvFile) throws ConvertException {
        CSVReader csvReader = null;

        try {
            List<MaccorChannelHeader> channels = new ArrayList<MaccorChannelHeader>();
            csvReader = new CSVReader(new FileReader(csvFile), CSV_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER,
                                      CHANNEL_NAME_LINE);

            // read channel name and unit from header line
            String[] line = csvReader.readNext();
            for (String str : line) {
                MaccorChannelHeader ch = new MaccorChannelHeader(str, DEFAULT_UNIT, DataType.DT_UNKNOWN);

                // read unit from channel name
                Matcher matcher = channelNamePattern.matcher(str);
                if (matcher.matches()) {
                    ch.setChannelName(matcher.group(1));
                    ch.setUnitName(matcher.group(2));
                }

                channels.add(ch);
            }

            // guess data types
            line = csvReader.readNext();
            while (line != null) {
                for (int i = 0; i < channels.size(); i++) {
                    MaccorChannelHeader channel = channels.get(i);
                    if (channel.getDataType() == DataType.DT_UNKNOWN) {
                        channel.setDataType(guessDataType(line[i]));
                    }
                    // set unit 's' for relative time
                    if (relativeTimePattern.matcher(line[i]).matches()) {
                        channel.setUnitName(REL_TIME_UNIT);
                    }

                }
                line = csvReader.readNext();
            }

            return Collections.unmodifiableList(channels);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            throw new ConvertException(ioe.getMessage(), ioe);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        NumberFormat nf = new DecimalFormat("#,##0.00");
        System.out.println(nf.parse("50.000,12458334"));
    }
    
    private TS_Value parseValue(String str, DataType dataType) throws ConvertException {
        try {
            TS_Value value = ODSHelper.createEmptyTS_Value(dataType);
            if (str != null && str.length() > 0) {
                // DT_FLOAT (relative time)
                if ((dataType == DataType.DT_FLOAT) && (relativeTimePattern.matcher(str).matches())) {
                    System.out.println(str);
                }
                // DT_FLOAT
                else if (dataType == DataType.DT_FLOAT) {
                    value.u.floatVal(Float.valueOf(str));
                }
            }
            return value;
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } catch (NumberFormatException nfe) {
            String message = "Unable to parse string '" + str + "' to datatype " + ODSHelper.dataType2String(dataType);
            LOG.error(message, nfe);
            throw new ConvertException(message, nfe);
        }
    }

    /**
     * Guesses the ODS data type from given input string.
     * 
     * @param str
     * @return
     */
    private DataType guessDataType(String str) {
        // DT_DATE (absolute time) ???
        Matcher matcher = absoluteTimePattern.matcher(str);
        if (matcher.matches()) {
            return DataType.DT_DATE;
        }
        // DT_FLOAT (relative time) ???
        matcher = relativeTimePattern.matcher(str);
        if (matcher.matches()) {
            return DataType.DT_FLOAT;
        }
        // DT_FLOAT ???
        try {
            floatFormat.parse(str);
            return DataType.DT_FLOAT;
        } catch (ParseException e) {
        }
        // DT_LONG ???
        try {
            intFormat.parse(str);
            return DataType.DT_LONG;
        } catch (ParseException e) {
        }
        // DT_STRING as fallback
        return DataType.DT_STRING;
    }

}
