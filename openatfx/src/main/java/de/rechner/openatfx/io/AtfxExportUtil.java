package de.rechner.openatfx.io;

import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class used to export ODS data to string format.
 * 
 * @author Christian Rechner
 */
abstract class AtfxExportUtil {

    /**
     * Non visible constructor.
     */
    private AtfxExportUtil() {}

    /**
     * Returns the ATFX string representation of a T_LONGLONG value.
     * 
     * @param l The value.
     * @return The string.
     */
    public static String createLongLongString(T_LONGLONG l) {
        return String.valueOf(ODSHelper.asJLong(l));
    }

    /**
     * Returns the ATFX string representation of a T_LONGLONG sequence.
     * 
     * @param ll The sequence.
     * @return The string.
     */
    public static String createLongLongSeqString(T_LONGLONG[] ll) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ll.length; i++) {
            sb.append(ODSHelper.asJLong(ll[i]));
            if (i < ll.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

}
