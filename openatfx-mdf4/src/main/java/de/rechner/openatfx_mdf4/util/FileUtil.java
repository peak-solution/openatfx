package de.rechner.openatfx_mdf4.util;

/**
 * Utility class for file handling.
 * 
 * @author Christian Rechner
 */
public class FileUtil {

    /**
     * Strips the file extension (e.g. '.txt').
     * 
     * @param s The file name.
     * @return File name without extension.
     */
    public static String stripExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }

}
