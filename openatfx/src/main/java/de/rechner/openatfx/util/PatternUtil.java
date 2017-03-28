package de.rechner.openatfx.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;


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
        if (pattern.equals("*")) {
            return true;
        }

        String regex = wildcardToRegex(pattern);
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return p.matcher(value).matches();
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
        if (pattern.equals("*")) {
            return true;
        }

        String regex = wildcardToRegex(pattern);
        return Pattern.matches(regex, value);
    }

    /**
     * Converts given string containing wildcards (* or ?) to its corresponding regular expression.
     * 
     * @param wildcard the string
     * @return the regular expression
     */
    private static String wildcardToRegex(String wildcard) {
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                break;
                case '?':
                    s.append(".");
                break;
                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '+':
                case '\\':
                    s.append("\\");
                    s.append(c);
                break;
                default:
                    s.append(c);
                break;
            }
        }
        s.append('$');
        return (s.toString());
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
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(name);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '[') {
                result.append("\\[");
            } else if (character == ']') {
                result.append("\\]");
            } else if (character == ';') {
                result.append("\\;");
            } else if (character == '/') {
                result.append("\\/");
            } else if (character == '\\') {
                result.append("\\\\");
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
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
        name = name.replace("\\[", "[");
        name = name.replace("\\]", "]");
        name = name.replace("\\;", ";");
        name = name.replace("\\/", "/");
        name = name.replace("\\\\", "\\");
        return name;
    }

}
