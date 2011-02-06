package de.rechner.openatfx.io;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_COMPLEX;
import org.asam.ods.T_DCOMPLEX;
import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Utility class used to parse ATFX content.
 * 
 * @author Christian Rechner
 */
abstract class AtfxParseUtil {

    /**
     * Non visible constructor.
     */
    private AtfxParseUtil() {}

    /**
     * Parse given string from an ATFX content and return the boolean value.
     * 
     * @param str The string to parse.
     * @return The boolean value.
     * @throws AoException Error parsing boolean.
     */
    public static boolean parseBoolean(String str) throws AoException {
        if (str != null && str.length() > 0) {
            String s = str.toLowerCase().trim();
            if (s.equals("true") || s.equals("1")) {
                return true;
            } else if (s.equals("false") || s.equals("0")) {
                return false;
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                              "Error parsing value of type DT_BOOLEAN '" + str + "'");
    }

    /**
     * Parse given string from an ATFX content and return the boolean array value.
     * 
     * @param str The string to parse.
     * @return The boolean array.
     * @throws AoException Error parsing boolean array.
     */
    public static boolean[] parseBooleanSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            boolean[] bAr = new boolean[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseBoolean(strAr[i]);
            }
            return bAr;
        }
        return new boolean[0];
    }

    /**
     * Parse given string from an ATFX content and return the long value.
     * 
     * @param str The string to parse.
     * @return The integer value.
     * @throws AoException Error parsing long value.
     */
    public static T_LONGLONG parseLongLong(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return ODSHelper.asODSLongLong(Long.parseLong(str.trim()));
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_LONGLONG '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the long long array value.
     * 
     * @param str The string to parse.
     * @return The long long array.
     * @throws AoException Error parsing long long array.
     */
    public static T_LONGLONG[] parseLongLongSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            T_LONGLONG[] bAr = new T_LONGLONG[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseLongLong(strAr[i]);
            }
            return bAr;
        }
        return new T_LONGLONG[0];
    }

    /**
     * Parse given string from an ATFX content and return the integer value.
     * 
     * @param str The string to parse.
     * @return The integer value.
     * @throws AoException Error parsing long value.
     */
    public static int parseLong(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_LONG '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the long array value.
     * 
     * @param str The string to parse.
     * @return The long array.
     * @throws AoException Error parsing long array.
     */
    public static int[] parseLongSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            int[] bAr = new int[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseLong(strAr[i]);
            }
            return bAr;
        }
        return new int[0];
    }

    /**
     * Parse given string from an ATFX content and return the float value.
     * 
     * @param str The string to parse.
     * @return The float value.
     * @throws AoException Error parsing long value.
     */
    public static float parseFloat(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return Float.parseFloat(str.trim());
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_FLOAT '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the float array value.
     * 
     * @param str The string to parse.
     * @return The float array.
     * @throws AoException Error parsing float array.
     */
    public static float[] parseFloatSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            float[] bAr = new float[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseFloat(strAr[i]);
            }
            return bAr;
        }
        return new float[0];
    }

    /**
     * Parse given string from an ATFX content and return the double value.
     * 
     * @param str The string to parse.
     * @return The double value.
     * @throws AoException Error parsing long value.
     */
    public static double parseDouble(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_DOUBLE '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the double array value.
     * 
     * @param str The string to parse.
     * @return The double array.
     * @throws AoException Error parsing double array.
     */
    public static double[] parseDoubleSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            double[] bAr = new double[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseDouble(strAr[i]);
            }
            return bAr;
        }
        return new double[0];
    }

    /**
     * Parse given string from an ATFX content and return the short value.
     * 
     * @param str The string to parse.
     * @return The short value.
     * @throws AoException Error parsing short value.
     */
    public static short parseShort(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return Short.parseShort(str.trim());
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_SHORT '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the short array value.
     * 
     * @param str The string to parse.
     * @return The short array.
     * @throws AoException Error parsing short array.
     */
    public static short[] parseShortSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            short[] bAr = new short[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseShort(strAr[i]);
            }
            return bAr;
        }
        return new short[0];
    }

    /**
     * Parse given string from an ATFX content and return the byte value.
     * 
     * @param str The string to parse.
     * @return The byte value.
     * @throws AoException Error parsing byte value.
     */
    public static byte parseByte(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                return (byte) Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_BYTE '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the byte array value.
     * 
     * @param str The string to parse.
     * @return The byte array value.
     * @throws AoException Error parsing byte array value.
     */
    public static byte[] parseByteSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            byte[] bAr = new byte[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                bAr[i] = parseByte(strAr[i]);
            }
            return bAr;
        }
        return new byte[0];
    }

    /**
     * Parse given string from an ATFX content and return the complex value.
     * 
     * @param str The string to parse.
     * @return The complex value.
     * @throws AoException Error parsing complex value.
     */
    public static T_COMPLEX parseComplex(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                String[] s = str.trim().split("\\s+");
                if (s.length != 2) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "Invalid representation of value T_COMPLEX '" + str + "'");
                }
                T_COMPLEX complex = new T_COMPLEX();
                complex.r = Float.parseFloat(s[0]);
                complex.i = Float.parseFloat(s[1]);
                return complex;
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type T_COMPLEX '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the complex value sequence.
     * 
     * @param str The string to parse.
     * @return The complex value sequence.
     * @throws AoException Error parsing complex value.
     */
    public static T_COMPLEX[] parseComplexSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            try {
                String[] strAr = input.split("\\s+");
                if (strAr.length % 2 != 0) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "Invalid representation of value T_COMPLEX[] '" + str + "'");
                }
                T_COMPLEX[] bAr = new T_COMPLEX[strAr.length / 2];
                for (int i = 0; i < strAr.length; i += 2) {
                    int x = i / 2;
                    bAr[x] = new T_COMPLEX();
                    bAr[x].r = Float.parseFloat(strAr[i]);
                    bAr[x].i = Float.parseFloat(strAr[i + 1]);
                }
                return bAr;
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_COMPLEX '" + str + "'");
            }
        }
        return new T_COMPLEX[0];
    }

    /**
     * Parse given string from an ATFX content and return the dcomplex value.
     * 
     * @param str The string to parse.
     * @return The dcomplex value.
     * @throws AoException Error parsing dcomplex value.
     */
    public static T_DCOMPLEX parseDComplex(String str) throws AoException {
        if (str != null && str.length() > 0) {
            try {
                String[] s = str.trim().split("\\s+");
                if (s.length != 2) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "Invalid representation of value T_DCOMPLEX '" + str + "'");
                }
                T_DCOMPLEX dcomplex = new T_DCOMPLEX();
                dcomplex.r = Double.parseDouble(s[0]);
                dcomplex.i = Double.parseDouble(s[1]);
                return dcomplex;
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type T_DCOMPLEX '" + str + "'");
            }
        }
        throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "Empty string not allowed");
    }

    /**
     * Parse given string from an ATFX content and return the complex value sequence.
     * 
     * @param str The string to parse.
     * @return The complex value sequence.
     * @throws AoException Error parsing complex value.
     */
    public static T_DCOMPLEX[] parseDComplexSeq(String str) throws AoException {
        String input = str.trim();
        if (input.length() > 0) {
            try {
                String[] strAr = input.split("\\s+");
                if (strAr.length % 2 != 0) {
                    throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                          "Invalid representation of value T_DCOMPLEX[] '" + str + "'");
                }
                T_DCOMPLEX[] bAr = new T_DCOMPLEX[strAr.length / 2];
                for (int i = 0; i < strAr.length; i += 2) {
                    int x = i / 2;
                    bAr[x] = new T_DCOMPLEX();
                    bAr[x].r = Double.parseDouble(strAr[i]);
                    bAr[x].i = Double.parseDouble(strAr[i + 1]);
                }
                return bAr;
            } catch (NumberFormatException nfe) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "Error parsing value of type DT_COMPLEX '" + str + "'");
            }
        }
        return new T_DCOMPLEX[0];
    }

}
