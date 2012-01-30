package de.rechner.openatfx.converter.diadem_dat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Reads DIAdem DAT files into a cache.
 * 
 * @author Christian Rechner
 */
final class DatHeaderReader {

    private static final String ENCODING = "ISO-8859-1";

    private static final String BEGINGLOBALHEADER = "#BEGINGLOBALHEADER";
    private static final String ENDGLOBALHEADER = "#ENDGLOBALHEADER";
    private static final String BEGINCHANNELHEADER = "#BEGINCHANNELHEADER";
    private static final String ENDCHANNELHEADER = "#ENDCHANNELHEADER";
    private static final Integer KEY_CHANNEL_NAME = 200;
    private static final Integer KEY_COMMENT_NAME = 106;
    private static final Integer KEY_COMMENT_VALUE = 102;

    private static volatile DatHeaderReader instance;

    private final Log log = LogFactory.getLog(DatHeaderReader.class);
    private final Pattern linePattern;

    /**
     * Private constructor.
     */
    private DatHeaderReader() {
        this.linePattern = Pattern.compile("(\\d*),(.*)");
    }

    /**
     * Reads an DAT header file.
     * 
     * @param file The file to read.
     * @param initialValues The initial values, may be null.
     * @return The DAT header data object.
     * @throws IOException Unable to read file.
     */
    public DatHeader readFile(File file) throws IOException {
        DatHeader header = new DatHeader(file);

        long start = System.currentTimeMillis();
        Scanner scanner = new Scanner(file, ENCODING);
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // is global header?
                if (line.equals(BEGINGLOBALHEADER)) {
                    readGlobalHeader(scanner, header);
                }

                // is channel header?
                if (line.equals(BEGINCHANNELHEADER)) {
                    readChannel(scanner, header);
                }
            }

            long duration = System.currentTimeMillis() - start;
            log.info("Read DAT header file: \"" + file.getAbsolutePath() + "\" in " + duration + "ms");
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return header;
    }

    /**
     * Reads the global header entry block.
     * 
     * @param scanner
     * @param datHeader
     * @throws IOException
     */
    private void readGlobalHeader(Scanner scanner, DatHeader datHeader) throws IOException {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // end reached
            if (line.equals(ENDGLOBALHEADER)) {
                return;
            }
            // read pair of name and value
            Entry<Integer, String> entry = parseLineEntry(line).entrySet().iterator().next();
            if (entry.getKey().equals(KEY_COMMENT_VALUE)) {
                line = scanner.nextLine();
                Entry<Integer, String> nameEntry = parseLineEntry(line).entrySet().iterator().next();
                if (!nameEntry.getKey().equals(KEY_COMMENT_NAME)) {
                    throw new IOException("Invalid comment (name missing): " + line);
                }
                datHeader.addGlobalHeaderComment(nameEntry.getValue(), entry.getValue());
            }
            // common header entry
            else {
                datHeader.addGlobalHeaderEntry(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Reads a channel header entry block.
     * 
     * @param scanner The file header.
     * @param datHeader The dat header to insert the data into.
     * @throws IOException Error parsing block.
     */
    private void readChannel(Scanner scanner, DatHeader datHeader) throws IOException {
        Map<Integer, String> map = new HashMap<Integer, String>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // end reached
            if (line.equals(ENDCHANNELHEADER)) {
                break;
            }
            map.putAll(parseLineEntry(line));
        }

        // try to parse channel name
        String channelName = map.get(KEY_CHANNEL_NAME);
        if (channelName == null || channelName.length() < 1) {
            throw new IOException("Channel name not found: " + map);
        }
        datHeader.addChannelHeaderEntry(channelName, map);
    }

    private Map<Integer, String> parseLineEntry(String line) throws IOException {
        Map<Integer, String> map = new HashMap<Integer, String>(1);
        Matcher matcher = linePattern.matcher(line);
        if (!matcher.matches()) {
            throw new IOException("Unable to parse line: " + line);
        }
        Integer key = Integer.valueOf(matcher.group(1));
        String value = matcher.group(2);
        map.put(key, value);
        return map;
    }

    /**
     * Returns the singleton instance.
     * 
     * @return The singleton instance.
     */
    public static DatHeaderReader getInstance() {
        if (instance == null) {
            instance = new DatHeaderReader();
        }
        return instance;
    }

}
