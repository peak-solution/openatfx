package com.peaksolution.openatfx.util;


/**
 * Utility class for ODS string pattern matches.
 * 
 * @author Christian Rechner
 */
public abstract class PatternUtil {

    /**
     * Non visible constructor.
     */
    private PatternUtil() {}

    /**
     * Checks if given value string matches the given pattern.<br>
     * The pattern may contain '*' and '?'.<br>
     * The lookup will be performed case insensitive.
     * 
     * @param value the value
     * @param pattern the pattern to match
     * @return true if matches, otherwise false
     */
    public static boolean nameFilterMatchCI(String value, String pattern) {
        return com.peaksolution.datamodel.util.PatternUtil.nameFilterMatchCI(value, pattern);
    }

    /**
     * Checks if given value string matches the given pattern.<br>
     * The pattern may contain '*' and '?'.<br>
     * The lookup will be performed case sensitive.
     * 
     * @param value the value
     * @param pattern the pattern to match
     * @return true if matches, otherwise false
     */
    public static boolean nameFilterMatch(String value, String pattern) {
        return com.peaksolution.datamodel.util.PatternUtil.nameFilterMatch(value, pattern);
    }

    /**
     * Escapes a instance name to be used within an ASAM path.
     * <p>
     * Following escape sequences will be used:
     * <ul>
     * <li>[ = \[</li>
     * <li>] = \]</li>
     * <li>; = \;</li>
     * <li>/ = \/</li>
     * <li>\ = \\</li>
     * </ul>
     * 
     * @param name The name string to escape.
     * @return The escaped string.
     */
    public static String escapeNameForASAMPath(String name) {
        return com.peaksolution.datamodel.util.PatternUtil.escapeNameForASAMPath(name);
    }

    /**
     * Unescapes a name in an ASAM path to be used as instance name.
     * <p>
     * Following escape sequences will be used:
     * <ul>
     * <li>[ = \[</li>
     * <li>] = \]</li>
     * <li>; = \;</li>
     * <li>/ = \/</li>
     * <li>\ = \\</li>
     * </ul>
     * 
     * @param name The name string to escape.
     * @return The escaped string.
     */
    public static String unEscapeNameForASAMPath(String name) {
        return com.peaksolution.datamodel.util.PatternUtil.unEscapeNameForASAMPath(name);
    }

}
