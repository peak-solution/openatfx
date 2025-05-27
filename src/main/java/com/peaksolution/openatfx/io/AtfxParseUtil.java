package com.peaksolution.openatfx.io;

import org.asam.ods.ErrorCode;

import com.peaksolution.openatfx.api.Complex;
import com.peaksolution.openatfx.api.DoubleComplex;
import com.peaksolution.openatfx.api.OpenAtfxException;


/**
 * Utility class used to parse ATFX content.
 * 
 * @author Christian Rechner
 */
public abstract class AtfxParseUtil {

    /**
     * Non visible constructor.
     */
    private AtfxParseUtil() {}

    /**
     * Parse given string from an ATFX content and return the boolean value.
     * 
     * @param str The string to parse.
     * @return The boolean value.
     * @throws OpenAtfxException Error parsing boolean.
     */
    public static boolean parseBoolean(String str) throws OpenAtfxException {
        if (str != null && str.length() > 0) {
            String s = str.toLowerCase().trim();
            if (s.equals("true") || s.equals("1")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse given string from an ATFX content and return the boolean array value.
     * 
     * @param str The string to parse.
     * @return The boolean array.
     * @throws OpenAtfxException Error parsing boolean array.
     */
    public static boolean[] parseBooleanSeq(String str) throws OpenAtfxException {
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
     * Parse given string from an ATFX content and return the date array value.
     * 
     * @param str The string to parse.
     * @return The date value array.
     */
    public static String[] parseDateSeq(String str) {
        String input = str.trim();
        if (input.length() > 0) {
            return input.split("\\s+");
        }
        return new String[0];
    }

    /**
     * Parse given string from an ATFX content and return the long value.
     * 
     * @param str The string to parse.
     * @return The integer value.
     * @throws OpenAtfxException Error parsing long value.
     */
    public static Long parseLongLong(String str) throws OpenAtfxException {
        if (str != null && str.length() > 0) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_LONGLONG '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the long long array value.
     * 
     * @param str The string to parse.
     * @return The long long array.
     * @throws OpenAtfxException Error parsing long long array.
     */
    public static long[] parseLongLongSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            long[] bAr = new long[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Long val = parseLongLong(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
            }
            return bAr;
        }
        return new long[0];
    }

    /**
     * Parse given string from an ATFX content and return the integer value.
     * 
     * @param str The string to parse.
     * @return The integer value.
     * @throws OpenAtfxException Error parsing long value.
     */
    public static Integer parseLong(String str) throws OpenAtfxException {
        if (str != null && !str.isBlank()) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException nfe) {
                double sizeGB = ((Integer.MAX_VALUE) / (1073741824.0));
                String reason = "The value '" + str + "' is not parsable to an integer (DT_LONG) value or "
                        + "exceeds the maximal allowed range of '" + Integer.MAX_VALUE + "' byte (" + sizeGB + " GB)!";
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, reason);
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the long array value.
     * 
     * @param str The string to parse.
     * @return The long array.
     * @throws OpenAtfxException Error parsing long array.
     */
    public static int[] parseLongSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            int[] bAr = new int[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Integer val = parseLong(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
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
     * @throws OpenAtfxException Error parsing long value.
     */
    public static Float parseFloat(String str) throws OpenAtfxException {
        if (str != null && !str.isBlank()) {
            try {
                return Float.parseFloat(handleNaNorINFValue(str));
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_FLOAT '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the float array value.
     * 
     * @param str The string to parse.
     * @return The float array.
     * @throws OpenAtfxException Error parsing float array.
     */
    public static float[] parseFloatSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            float[] bAr = new float[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Float val = parseFloat(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
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
     * @throws OpenAtfxException Error parsing long value.
     */
    public static Double parseDouble(String str) throws OpenAtfxException {
        if (str != null && str.length() > 0) {
            try {
                return Double.parseDouble(handleNaNorINFValue(str));
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_DOUBLE '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the double array value.
     * 
     * @param str The string to parse.
     * @return The double array.
     * @throws OpenAtfxException Error parsing double array.
     */
    public static double[] parseDoubleSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            double[] bAr = new double[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Double val = parseDouble(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
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
     * @throws OpenAtfxException Error parsing short value.
     */
    public static Short parseShort(String str) throws OpenAtfxException {
        if (str != null && !str.isBlank()) {
            try {
                return Short.parseShort(str.trim());
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_SHORT '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the short array value.
     * 
     * @param str The string to parse.
     * @return The short array.
     * @throws OpenAtfxException Error parsing short array.
     */
    public static short[] parseShortSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            short[] bAr = new short[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Short val = parseShort(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
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
     * @throws OpenAtfxException Error parsing byte value.
     */
    public static Byte parseByte(String str) throws OpenAtfxException {
        if (str != null && str.length() > 0) {
            try {
                return (byte) Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_BYTE '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the byte array value.
     * 
     * @param str The string to parse.
     * @return The byte array value.
     * @throws OpenAtfxException Error parsing byte array value.
     */
    public static byte[] parseByteSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            String[] strAr = input.split("\\s+");
            byte[] bAr = new byte[strAr.length];
            for (int i = 0; i < strAr.length; i++) {
                Byte val = parseByte(strAr[i]);
                if (val != null) {
                    bAr[i] = val;
                } else {
                    bAr[i] = 0;
                }
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
     * @throws OpenAtfxException Error parsing complex value.
     */
    public static Complex parseComplex(String str) throws OpenAtfxException {
        if (str != null && !str.isBlank()) {
            try {
                String[] s = str.trim().split("\\s+");
                if (s.length != 2) {
                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                "Invalid representation of value T_COMPLEX '" + str + "'");
                }
                return new Complex(Float.parseFloat(s[0]), Float.parseFloat(s[1]));
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type T_COMPLEX '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the complex value sequence.
     * 
     * @param str The string to parse.
     * @return The complex value sequence.
     * @throws OpenAtfxException Error parsing complex value.
     */
    public static Complex[] parseComplexSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            try {
                String[] strAr = input.split("\\s+");
                if (strAr.length % 2 != 0) {
                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                "Invalid representation of value T_COMPLEX[] '" + str + "'");
                }
                Complex[] bAr = new Complex[strAr.length / 2];
                for (int i = 0; i < strAr.length; i += 2) {
                    int x = i / 2;
                    bAr[x] = new Complex(Float.parseFloat(strAr[i]), Float.parseFloat(strAr[i + 1]));
                }
                return bAr;
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_COMPLEX '" + str + "'");
            }
        }
        return new Complex[0];
    }

    /**
     * Parse given string from an ATFX content and return the dcomplex value.
     * 
     * @param str The string to parse.
     * @return The dcomplex value.
     * @throws OpenAtfxException Error parsing dcomplex value.
     */
    public static DoubleComplex parseDComplex(String str) throws OpenAtfxException {
        if (str != null && !str.isBlank()) {
            try {
                String[] s = str.trim().split("\\s+");
                if (s.length != 2) {
                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                "Invalid representation of value T_DCOMPLEX '" + str + "'");
                }
                return new DoubleComplex(Double.parseDouble(s[0]), Double.parseDouble(s[1]));
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type T_DCOMPLEX '" + str + "'");
            }
        }
        return null;
    }

    /**
     * Parse given string from an ATFX content and return the complex value sequence.
     * 
     * @param str The string to parse.
     * @return The complex value sequence.
     * @throws OpenAtfxException Error parsing complex value.
     */
    public static DoubleComplex[] parseDComplexSeq(String str) throws OpenAtfxException {
        String input = str.trim();
        if (input.length() > 0) {
            try {
                String[] strAr = input.split("\\s+");
                if (strAr.length % 2 != 0) {
                    throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                                "Invalid representation of value T_DCOMPLEX[] '" + str + "'");
                }
                DoubleComplex[] bAr = new DoubleComplex[strAr.length / 2];
                for (int i = 0; i < strAr.length; i += 2) {
                    int x = i / 2;
                    bAr[x] = new DoubleComplex(Double.parseDouble(strAr[i]), Double.parseDouble(strAr[i + 1]));
                }
                return bAr;
            } catch (NumberFormatException nfe) {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            "Error parsing value of type DT_COMPLEX '" + str + "'");
            }
        }
        return new DoubleComplex[0];
    }

    /**
     * ODS specification says in chapter 8.12.2, that applications need to support the strings NaN and NAN.
     * Therefore this value has to be handled accordingly, since Java's parseFloat/Double() methods only
     * work for NaN. Furthermore it is required to support following infinity representations: 
     * inf, INF, -inf, -INF in addition to Infinity / -Infinity
     * 
     * @param originalValue the original value
     * @return the probably adjusted value
     */
    private static String handleNaNorINFValue(String originalValue) {
        String adjustedValue = originalValue.trim();
        if ("nan".equalsIgnoreCase(adjustedValue)) {
            adjustedValue = "NaN";
        } else if ("inf".equalsIgnoreCase(adjustedValue)) {
            adjustedValue = "Infinity";
        } else if ("-inf".equalsIgnoreCase(adjustedValue)) {
            adjustedValue = "-Infinity";
        }
        return adjustedValue;
    }
}
