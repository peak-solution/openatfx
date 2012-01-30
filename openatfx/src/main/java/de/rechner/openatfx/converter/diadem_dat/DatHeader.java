package de.rechner.openatfx.converter.diadem_dat;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


class DatHeader {

    // global header keys
    public static final int KEY_ORIGIN = 1;
    public static final int KEY_REVISION = 2;
    public static final int KEY_DESCRIPTION = 101;
    public static final int KEY_PERSON = 103;
    public static final int KEY_DATE = 104;
    public static final int KEY_TIME = 105;

    // channel header keys
    public static final int KEY_COMMENT = 201;
    public static final int KEY_UNIT = 202;
    public static final int KEY_CHANNEL_TYPE = 210;
    public static final int KEY_FILENAME = 211;
    public static final int KEY_METHOD = 213;
    public static final int KEY_DATATYPE = 214;
    public static final int KEY_NO_OF_VALUES = 220;
    public static final int KEY_FILE_OFFSET = 221;
    public static final int KEY_CHANNEL_OFFSET = 222;

    private final File sourceFile;
    private final Map<Integer, String> globalHeaderEntries; // <header key,value>
    private final Map<String, String> globalHeaderComments; // <name,value>
    private final Map<String, Map<Integer, String>> channelHeaderEntries; // <channel name<channel value key,value>>

    public DatHeader(File sourceFile) {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile must not be null");
        }
        this.sourceFile = sourceFile;
        this.globalHeaderEntries = new TreeMap<Integer, String>();
        this.globalHeaderComments = new LinkedHashMap<String, String>();
        this.channelHeaderEntries = new LinkedHashMap<String, Map<Integer, String>>();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void addGlobalHeaderEntry(int key, String value) {
        this.globalHeaderEntries.put(key, value);
    }

    public Collection<Integer> listGlobalHeaderEntries() {
        return this.globalHeaderEntries.keySet();
    }

    public String getGlobalHeaderEntry(int key) {
        return this.globalHeaderEntries.get(key);
    }

    public void addGlobalHeaderComment(String name, String value) {
        this.globalHeaderComments.put(name, value);
    }

    public Collection<String> listGlobalHeaderComments() {
        return this.globalHeaderComments.keySet();
    }

    public String getGlobalHeaderComment(String name) {
        return this.globalHeaderComments.get(name);
    }

    public void addChannelHeaderEntry(String channelName, Map<Integer, String> channelEntries) {
        Map<Integer, String> map = this.channelHeaderEntries.get(channelName);
        if (map == null) {
            map = new TreeMap<Integer, String>();
            this.channelHeaderEntries.put(channelName, map);
        }
        map.putAll(channelEntries);
    }

    public String getChannelHeaderEntry(String channelName, int key) {
        Map<Integer, String> map = this.channelHeaderEntries.get(channelName);
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    public Set<String> listChannelNames() {
        return this.channelHeaderEntries.keySet();
    }

}
