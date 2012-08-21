package de.rechner.openatfx.converter.maccor_csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asam.ods.DataType;


public class MaccorCSVReader {

    private static final char CSV_SEPARATOR = ';';
    private static final int CHANNEL_NAME_LINE = 15;
    private static final String DEFAULT_UNIT = "-";
    private static final String REL_TIME_UNIT = "s";

    private final Pattern channelNamePattern;
    private final Pattern relativeTimePattern;
    private final Pattern absoluteTimePattern;

    public MaccorCSVReader() {
        this.channelNamePattern = Pattern.compile("(.*)\\s\\[(.*)\\]");
        this.relativeTimePattern = Pattern.compile("\\d*d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d\\d\\d");
        this.absoluteTimePattern = Pattern.compile("\\d*/\\d*/\\d* \\d*:\\d*"); // 2/24/2011 16:41
    }

    /**
     * Reads the channel names from the CSV file. The units will not be included in the channel names.
     * 
     * @param csvFile
     * @return
     * @throws IOException
     */
    public List<MaccorChannelHeader> readChannelHeader(File csvFile) throws IOException {
        CSVReader reader = null;
        try {
            List<MaccorChannelHeader> channels = new ArrayList<MaccorChannelHeader>();
            reader = new CSVReader(new FileReader(csvFile), CSV_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER,
                                   CHANNEL_NAME_LINE);

            // read channel name and unit from header line
            String[] line = reader.readNext();
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
            line = reader.readNext();
            while (line != null) {
                for (int i = 0; i < channels.size(); i++) {
                    MaccorChannelHeader channel = channels.get(i);
                    if (channel.getDataType() == DataType.DT_UNKNOWN) {
                        channel.setDataType(guessDataType(line[i]));
                    }
                    // set unit 's' for relative time
                    if (this.relativeTimePattern.matcher(line[i]).matches()) {
                        channel.setUnitName(REL_TIME_UNIT);
                    }

                }
                line = reader.readNext();
            }

            return Collections.unmodifiableList(channels);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private DataType guessDataType(String str) {
        // DT_LONG ???
        try {
            Integer.valueOf(str);
            return DataType.DT_LONG;
        } catch (NumberFormatException nfe) {
        }
        // DT_FLOAT ???
        try {
            Float.valueOf(str);
            return DataType.DT_FLOAT;
        } catch (NumberFormatException nfe) {
        }
        // DT_FLOAT (relative time) ???
        Matcher matcher = this.relativeTimePattern.matcher(str);
        if (matcher.matches()) {
            return DataType.DT_FLOAT;
        }
        // DT_DATE
        matcher = this.absoluteTimePattern.matcher(str);
        if (matcher.matches()) {
            return DataType.DT_DATE;
        }
        // DT_STRING as fallback
        return DataType.DT_STRING;
    }

}
