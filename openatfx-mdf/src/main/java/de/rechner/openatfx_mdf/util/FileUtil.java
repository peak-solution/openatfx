package de.rechner.openatfx_mdf.util;

/**
 * Utility class for file handling.
 * 
 * @author Christian Rechner
 */
public abstract class FileUtil {

    /**
     * Strips the file extension (e.g. '.txt').
     * 
     * @param s The file name.
     * @return File name without extension.
     */
    public static String stripExtension(final String s) {
        return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
    }

    public static String getResultName(String fileName, String resultSuffix) {
        String meaResultName = fileName.trim();
        if (resultSuffix != null && resultSuffix.length() > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(getFileNameWithoutExtension(fileName));
            sb.append(resultSuffix);
            sb.append(".");
            sb.append(getFileExtension(fileName));
            meaResultName = sb.toString();
        }
        return meaResultName;
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    public static String getFileExtension(String fileName) {
        String ext = null;
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1) {
            ext = fileName.substring(i + 1);
        }
        return ext;
    }

}
