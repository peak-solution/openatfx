package de.rechner.openatfx.converter.digatron_csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
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


class DigatronCSVReader {

    private static final Log LOG = LogFactory.getLog(DigatronCSVReader.class);

    private static final char CSV_SEPARATOR = ';';
    private static final int CHANNEL_NAME_LINE = 12;
    private static final int UNIT_NAME_LINE = 13;

    private static final String DEFAULT_UNIT = "-";
    private static final DataType DEFAULT_DATATYPE = DataType.DT_FLOAT;

    private static final String UNIT_NAME_PATTERN = ".*\\[(.*)\\].*";
    private static final String TIME_PATTERN = "(\\d\\d):(\\d\\d):(\\d\\d)"; // hh:mm:ss
    private static final String MILLIS_PATTERN = "(\\d\\d):(\\d\\d),(\\d)"; // mm:ss:s

    private static final Map<String, DataType> STATIC_DATA_TYPES = new HashMap<String, DataType>();
    static {
        STATIC_DATA_TYPES.put("Schritt", DataType.DT_LONG);
        STATIC_DATA_TYPES.put("Zustand", DataType.DT_STRING);
        STATIC_DATA_TYPES.put("Zeit", DataType.DT_DATE);
        STATIC_DATA_TYPES.put("Schrittdauer", DataType.DT_LONG);
        STATIC_DATA_TYPES.put("Programmdauer", DataType.DT_FLOAT);
        STATIC_DATA_TYPES.put("Zyklus", DataType.DT_LONG);
        STATIC_DATA_TYPES.put("Zyklusebene", DataType.DT_LONG);
        STATIC_DATA_TYPES.put("Prozedur", DataType.DT_STRING);
    }

    private static final Map<String, String> STATIC_UNITS = new HashMap<String, String>();
    static {
        STATIC_UNITS.put("Schrittdauer", "s");
        STATIC_UNITS.put("Programmdauer", "s");
    }

    private final Pattern unitNamePattern;
    private final Pattern timePattern;
    private final Pattern millisPattern;
    private final NumberFormat intFormat;
    private final NumberFormat floatFormat;

    private final List<DigatronChannelHeader> channelHeader;

    private final CSVReader csvReader;
    private String[] lastReadLine = null;
    private String lastReadTestStepName = null;

    /**
     * @param csvFile
     * @throws ConvertException
     */
    public DigatronCSVReader(File csvFile) throws ConvertException {
        this.unitNamePattern = Pattern.compile(UNIT_NAME_PATTERN);
        this.timePattern = Pattern.compile(TIME_PATTERN);
        this.millisPattern = Pattern.compile(MILLIS_PATTERN);
        this.intFormat = NumberFormat.getIntegerInstance(Locale.GERMAN);
        this.floatFormat = NumberFormat.getNumberInstance(Locale.GERMAN);

        // read channel header
        this.channelHeader = readChannelHeader(csvFile);
        // open CSV reader for measurement data
        try {
            this.csvReader = new CSVReader(new FileReader(csvFile), CSV_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER,
                                           UNIT_NAME_LINE + 1);
            this.lastReadLine = this.csvReader.readNext();
            this.lastReadTestStepName = buildTestStepName(parseDataLine(this.lastReadLine));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

    public List<DigatronChannelHeader> getChannelHeader() {
        return channelHeader;
    }

    public String getLastReadTestStepName() {
        return lastReadTestStepName;
    }

    public Map<String, List<TS_Value>> readNextStepDataBlock() throws ConvertException {
        if (this.lastReadLine == null) {
            return null;
        }

        try {
            Map<String, List<TS_Value>> map = new HashMap<String, List<TS_Value>>();

            while (true) {
                // check if end of CSV file is reached
                if (this.lastReadLine == null) {
                    break;
                }

                // parse current line
                Map<String, TS_Value> dataLine = parseDataLine(this.lastReadLine);
                String testStepName = buildTestStepName(dataLine);

                // check if end of data block is reached
                if (!this.lastReadTestStepName.equals(testStepName)) {
                    this.lastReadTestStepName = testStepName;
                    break;
                }

                // step to next line
                this.lastReadLine = this.csvReader.readNext();
                System.out.println(this.lastReadLine);
            }

            return map;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

    private String buildTestStepName(Map<String, TS_Value> dataLine) throws ConvertException {
        StringBuffer sb = new StringBuffer();

        // Zyklus
        TS_Value value = dataLine.get("Zyklus");
        if (value == null) {
            throw new ConvertException("Unable to find value 'Zyklus'");
        }
        sb.append("Zyklus: ");
        sb.append(value.u.longVal());
        sb.append(", ");

        // Zyklusebene
        value = dataLine.get("Zyklusebene");
        if (value == null) {
            throw new ConvertException("Unable to find value 'Zyklusebene'");
        }
        sb.append("Zyklusebene: ");
        sb.append(value.u.longVal());
        sb.append(", ");

        // Schritt
        value = dataLine.get("Schritt");
        if (value == null) {
            throw new ConvertException("Unable to find value 'Schritt'");
        }
        sb.append("Schritt: ");
        sb.append(value.u.longVal());
        sb.append(", ");

        // Zustand
        value = dataLine.get("Zustand");
        if (value == null) {
            throw new ConvertException("Unable to find value 'Zustand'");
        }
        sb.append("Zustand: ");
        sb.append(value.u.stringVal());
        sb.append(", ");

        // Prozedur
        value = dataLine.get("Prozedur");
        if (value == null) {
            throw new ConvertException("Unable to find value 'Prozedur'");
        }
        sb.append("Prozedur: ");
        sb.append(value.u.stringVal());

        return sb.toString();
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
            DigatronChannelHeader channelHeader = this.channelHeader.get(i);
            map.put(channelHeader.getChannelName(), parseValue(line[i], channelHeader.getDataType()));
        }
        return map;
    }

    /**
     * Parse a value from the CSV file.
     * 
     * @param str The string value to parse.
     * @param dataType The target data type.
     * @return Map with channel name as key and ODS value as value.
     * @throws ConvertException Error parsing value.
     */
    private TS_Value parseValue(String str, DataType dataType) throws ConvertException {
        try {
            TS_Value value = ODSHelper.createEmptyTS_Value(dataType);
            if (str != null && str.length() > 0) {
                Matcher timePatternMatcher = timePattern.matcher(str);
                Matcher millisPatternMatcher = millisPattern.matcher(str);

                // relative time in seconds (hh:mm:ss)
                if ((dataType == DataType.DT_LONG) && timePatternMatcher.matches()) {
                    int hours = intFormat.parse(timePatternMatcher.group(1)).intValue();
                    int mins = intFormat.parse(timePatternMatcher.group(2)).intValue();
                    int secs = intFormat.parse(timePatternMatcher.group(3)).intValue();
                    int relSecs = (hours * 60 * 60) + (mins * 60) + secs;
                    value.u.longVal(relSecs);
                }
                // relative time in seconds (mm:ss:s)
                else if ((dataType == DataType.DT_FLOAT) && millisPatternMatcher.matches()) {
                    int mins = intFormat.parse(millisPatternMatcher.group(1)).intValue();
                    int secs = intFormat.parse(millisPatternMatcher.group(2)).intValue();
                    int millis = intFormat.parse(millisPatternMatcher.group(3)).intValue();
                    float relSecs = (mins * 60) + secs + (0.1f * millis);
                    value.u.floatVal(relSecs);
                }
                // DT_LONG
                else if (dataType == DataType.DT_LONG) {
                    value.u.longVal(intFormat.parse(str).intValue());
                }
                // DT_FLOAT
                else if (dataType == DataType.DT_FLOAT) {
                    value.u.floatVal(floatFormat.parse(str).floatValue());
                }
                // DT_STRING
                else if (dataType == DataType.DT_STRING) {
                    value.u.stringVal(str);
                }
                // DT_DATE
                else if (dataType == DataType.DT_DATE) {
                    value.u.dateVal(""); // TODO
                }
                // unsupported data type
                else {
                    throw new ConvertException("Unsupported datatype: " + ODSHelper.dataType2String(dataType));
                }
            }
            return value;
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } catch (ParseException pe) {
            String message = "Unable to parse string '" + str + "' to datatype " + ODSHelper.dataType2String(dataType);
            LOG.error(message, pe);
            throw new ConvertException(message, pe);
        }
    }

    /**
     * Reads the channel headers.
     * 
     * @param csvFile The CSV file to read.
     * @return List of channel header information.
     * @throws IOException
     */
    private List<DigatronChannelHeader> readChannelHeader(File csvFile) throws ConvertException {
        CSVReader csvReader = null;

        try {
            List<DigatronChannelHeader> channels = new ArrayList<DigatronChannelHeader>();
            csvReader = new CSVReader(new FileReader(csvFile), CSV_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER,
                                      CHANNEL_NAME_LINE);

            // read channel names
            List<String> channelNames = new ArrayList<String>();
            for (String str : csvReader.readNext()) {
                channelNames.add(str);
            }

            // read units
            List<String> unitNames = new ArrayList<String>();
            String[] lineData = csvReader.readNext();
            for (int i = 0; i < lineData.length; i++) {
                Matcher matcher = unitNamePattern.matcher(lineData[i]);
                if (matcher.matches()) {
                    unitNames.add(matcher.group(1));
                } else if (STATIC_UNITS.containsKey(channelNames.get(i))) {
                    unitNames.add(STATIC_UNITS.get(channelNames.get(i)));
                } else {
                    unitNames.add(DEFAULT_UNIT);
                }
            }

            // create channel header objects
            for (int i = 0; i < channelNames.size(); i++) {
                String channelName = channelNames.get(i);
                String unitName = unitNames.get(i);
                DataType dataType = STATIC_DATA_TYPES.get(channelName);
                if (dataType == null) {
                    dataType = DEFAULT_DATATYPE;
                }
                channels.add(new DigatronChannelHeader(channelName, unitName, dataType));
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

}
