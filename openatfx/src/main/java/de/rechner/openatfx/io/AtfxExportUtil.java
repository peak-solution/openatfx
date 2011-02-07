package de.rechner.openatfx.io;

import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
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
     * Returns the ATFX string representation of a boolean value.
     * 
     * @param b The boolean value.
     * @return The string.
     */
    public static String createBooleanString(boolean b) {
        return String.valueOf(b);
    }

    /**
     * Returns the ATFX string representation of a boolean sequence.
     * 
     * @param bAr The sequence of boolean values.
     * @return The string.
     */
    public static String createBooleanSeqString(boolean[] bAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bAr.length; i++) {
            sb.append(createBooleanString(bAr[i]));
            if (i < bAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a byte value.
     * 
     * @param b The byte value.
     * @return The string.
     */
    public static String createByteString(byte b) {
        return Integer.toString(b & 0xFF);
    }

    /**
     * Returns the ATFX string representation of a byte sequence.
     * 
     * @param bAr The sequence of byte values.
     * @return The string.
     */
    public static String createByteSeqString(byte[] bAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bAr.length; i++) {
            sb.append(createByteString(bAr[i]));
            if (i < bAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a short value.
     * 
     * @param s The short value.
     * @return The string.
     */
    public static String createShortString(short s) {
        return String.valueOf(s);
    }

    /**
     * Returns the ATFX string representation of a short sequence.
     * 
     * @param sAr The sequence of short values.
     * @return The string.
     */
    public static String createShortSeqString(short[] sAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sAr.length; i++) {
            sb.append(createShortString(sAr[i]));
            if (i < sAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a long value.
     * 
     * @param b The long value.
     * @return The string.
     */
    public static String createLongString(int l) {
        return Integer.toString(l);
    }

    /**
     * Returns the ATFX string representation of a long sequence.
     * 
     * @param lAr The sequence of long values.
     * @return The string.
     */
    public static String createLongSeqString(int[] lAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lAr.length; i++) {
            sb.append(createLongString(lAr[i]));
            if (i < lAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

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
            sb.append(createLongLongString(ll[i]));
            if (i < ll.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a float value.
     * 
     * @param f The float value.
     * @return The string.
     */
    public static String createFloatString(float f) {
        return Float.toString(f);
    }

    /**
     * Returns the ATFX string representation of a float sequence.
     * 
     * @param fAr The sequence of float values.
     * @return The string.
     */
    public static String createFloatSeqString(float[] fAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fAr.length; i++) {
            sb.append(createFloatString(fAr[i]));
            if (i < fAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a double value.
     * 
     * @param d The double value.
     * @return The string.
     */
    public static String createDoubleString(double d) {
        return Double.toString(d);
    }

    /**
     * Returns the ATFX string representation of a double sequence.
     * 
     * @param fAr The sequence of double values.
     * @return The string.
     */
    public static String createDoubleSeqString(double[] dAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < dAr.length; i++) {
            sb.append(createDoubleString(dAr[i]));
            if (i < dAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a T_COMPLEX value.
     * 
     * @param c The complex value.
     * @return The string.
     */
    public static String createComplexString(T_COMPLEX c) {
        StringBuffer sb = new StringBuffer();
        sb.append(c.r);
        sb.append(" ");
        sb.append(c.i);
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a T_COMPLEX sequence.
     * 
     * @param cAr The sequence of T_COMPLEX values.
     * @return The string.
     */
    public static String createComplexSeqString(T_COMPLEX[] cAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cAr.length; i++) {
            sb.append(createComplexString(cAr[i]));
            if (i < cAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a T_DCOMPLEX value.
     * 
     * @param c The double complex value.
     * @return The string.
     */
    public static String createDComplexString(T_DCOMPLEX c) {
        StringBuffer sb = new StringBuffer();
        sb.append(c.r);
        sb.append(" ");
        sb.append(c.i);
        return sb.toString();
    }

    /**
     * Returns the ATFX string representation of a T_DCOMPLEX sequence.
     * 
     * @param cAr The sequence of T_DCOMPLEX values.
     * @return The string.
     */
    public static String createDComplexSeqString(T_DCOMPLEX[] cAr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cAr.length; i++) {
            sb.append(createDComplexString(cAr[i]));
            if (i < cAr.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

}
