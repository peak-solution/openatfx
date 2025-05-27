package com.peaksolution.openatfx.api;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.asam.ods.ErrorCode;

import com.peaksolution.openatfx.util.ODSHelper;

public class SingleValue
{
  private static final String MSG_WRONG_DATATYPE = "Requested wrong datatype from union!";
  // the character used to separate the elements of a value
  public static final String VALUE_SEPARATOR_STRING = " ";
  // the string/character used to separate the elements of a sequence
  public static final String SEQ_SEPARATOR_STRING = ",";
  public static final char SEQ_SEPARATOR_CHAR = ',';
 
  private short flag;
  
  private DataType discriminator;
  private BasicValue<?> value;

  public SingleValue()
  {
    this.discriminator = DataType.DT_UNKNOWN;
  }
  
  public SingleValue(DataType discriminator)
  {
    this.discriminator = discriminator;
    setValue(null);
  }
  
  public SingleValue(DataType discriminator, Object value)
  {
    this.discriminator = discriminator;
    setValue(value);
  }
  
  public SingleValue(SingleValue org)
  {
    this.discriminator = org.discriminator();
    setValue(org.getValue());
    setFlag(org.getFlag());
  }
  
  public boolean isValid()
  {
    return (short)15 == getFlag();
  }
  
  public void setDiscriminator(DataType discriminator)
  {
    this.discriminator = discriminator;
  }
  
  public int getLength()
  {
    int length = 0;
    Object value = getValue();
    
    if (value != null)
    {
      if (discriminator.isSequenceType())
      {
        length = Array.getLength(value);
      }
      else
      {
        length = 1;
      }
    }
    return length;
  }

  public DataType discriminator()
  {
    return discriminator;
  }
  
  /**
   * @return the flag
   */
  public short getFlag()
  {
    return flag;
  }

  /**
   * @param flag
   *        the flag to set
   */
  public void setFlag(short flag)
  {
    this.flag = flag;
  }
  
  public void stringToValue(String valueString)
  {
    if (valueString == null)
    {
      return;
    }
    valueString = valueString.trim();
    if (valueString.isEmpty())
    {
      setValue(null);
      return;
    }
    
    switch (discriminator)
    {
    case DT_BOOLEAN:
      setValue(Boolean.parseBoolean(valueString));
      break;
    case DT_BYTE:
      setValue(ODSHelper.hexToByte(valueString));
      break;
    case DT_COMPLEX:
      String[] parts = valueString.split(VALUE_SEPARATOR_STRING);
      setValue(new Complex(Float.parseFloat(parts[0]), Float.parseFloat(parts[1])));
      break;
    case DT_DATE:
      setValue(valueString);
      break;
    case DT_DCOMPLEX:
      parts = valueString.split(VALUE_SEPARATOR_STRING);
      setValue(new DoubleComplex(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
      break;
    case DT_DOUBLE:
      setValue(Double.parseDouble(valueString));
      break;
    case DT_ENUM:
      setValue(Integer.parseInt(valueString));
      break;
    case DT_EXTERNALREFERENCE:
      String[] parsedStrings = ODSHelper.parseStringSequenceFromValue(valueString);
      setValue(ExternalReference.getExtRef(parsedStrings));
      break;
    case DT_FLOAT:
      setValue(Float.parseFloat(valueString));
      break;
    case DT_LONG:
      setValue(Integer.parseInt(valueString));
      break;
    case DT_LONGLONG:
      setValue(Long.parseLong(valueString));
      break;
    case DT_SHORT:
      setValue(Short.parseShort(valueString));
      break;
    case DT_STRING:
      setValue(valueString);
      break;
    case DT_BYTESTR:
      char[] chars = valueString.toCharArray();
      byte[] byteArray = new byte[chars.length + 1];
      int i;
      for (i = 0; i < chars.length; i++)
      {
        byteArray[i] = ODSHelper.hexToByte(Character.toString(chars[i]));
      }
      byteArray[i + 1] = ODSHelper.hexToByte(VALUE_SEPARATOR_STRING);
      setValue(byteArray);
      break;
    case DS_BOOLEAN:
      String[] values = valueString.split(SEQ_SEPARATOR_STRING);
      boolean[] booleanValues = new boolean[values.length];
      for (i = 0; i < booleanValues.length; i++)
      {
        booleanValues[i] = Boolean.parseBoolean(values[i]);
      }
      setValue(booleanValues);
      break;
    case DS_BYTE:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      byte[] byteValues = new byte[values.length];
      for (i = 0; i < values.length; i++)
      {
        byteValues[i] = ODSHelper.hexToByte(values[i]);
      }
      setValue(byteValues);
      break;
    case DS_BYTESTR:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      byte[][] byteStrValues = new byte[values.length][];
      for (i = 0; i < values.length; i++)
      {
          chars = values[i].toCharArray();
        byteArray = new byte[chars.length + 1];
        int j;
        for (j = 0; j < chars.length; j++)
        {
          byteArray[i] = ODSHelper.hexToByte(Character.toString(chars[i]));
        }
        byteArray[j + 1] = ODSHelper.hexToByte(VALUE_SEPARATOR_STRING);
        byteStrValues[i] = byteArray;
      }
      setValue(byteStrValues);
      break;
    case DS_COMPLEX:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      Complex[] complexValues = new Complex[values.length];
      for (i = 0; i < values.length; i++)
      {
        parts = values[i].split(VALUE_SEPARATOR_STRING);
        complexValues[i] = new Complex(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
      }
      setValue(complexValues);
      break;
    case DS_DATE:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      dateSeq(values);
      break;
    case DS_DCOMPLEX:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      DoubleComplex[] dcomplexValues = new DoubleComplex[values.length];
      for (i = 0; i < values.length; i++)
      {
        parts = values[i].split(VALUE_SEPARATOR_STRING);
        dcomplexValues[i] = new DoubleComplex(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
      }
      setValue(dcomplexValues);
      break;
    case DS_DOUBLE:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      double[] doubleValues = new double[values.length];
      for (i = 0; i < doubleValues.length; i++)
      {
        doubleValues[i] = Double.parseDouble(values[i]);
      }
      setValue(doubleValues);
      break;
    case DS_ENUM:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      int[] enumValues = new int[values.length];
      for (i = 0; i < enumValues.length; i++)
      {
        enumValues[i] = Integer.parseInt(values[i]);
      }
      setValue(enumValues);
      break;
    case DS_EXTERNALREFERENCE:
      parsedStrings = ODSHelper.parseStringSequenceFromValue(valueString);
      ExternalReference[] extRefValues = ExternalReference.getExtRefs(Arrays.asList(parsedStrings));
      setValue(extRefValues);
      break;
    case DS_FLOAT:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      float[] floatValues = new float[values.length];
      for (i = 0; i < floatValues.length; i++)
      {
        floatValues[i] = Float.parseFloat(values[i]);
      }
      setValue(floatValues);
      break;
    case DS_LONG:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      int[] longValues = new int[values.length];
      for (i = 0; i < longValues.length; i++)
      {
        longValues[i] = Integer.parseInt(values[i]);
      }
      setValue(longValues);
      break;
    case DS_LONGLONG:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      long[] longlongValues = new long[values.length];
      for (i = 0; i < longlongValues.length; i++)
      {
        longlongValues[i] = Long.parseLong(values[i]);
      }
      setValue(longlongValues);
      break;
    case DS_SHORT:
      values = valueString.split(SEQ_SEPARATOR_STRING);
      short[] shortValues = new short[values.length];
      for (i = 0; i < shortValues.length; i++)
      {
        shortValues[i] = Short.parseShort(values[i]);
      }
      setValue(shortValues);
      break;
    case DS_STRING:
      values = ODSHelper.parseStringSequenceFromValue(valueString);
      setValue(values);
      break;
    default:
      throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "DataType not supported: " + discriminator);
    }
  }
  
  public void setValue(Object value)
  {
    setValue(value, (short)15);
  }
  
  public void setValue(Object value, short flag)
  {
    if (isInvalidValue(value))
    {
      this.flag = 0;
      setValueUnchecked(getEmptyValue(discriminator));
    }
    else
    {
      this.flag = flag;
      setValueUnchecked(value);
    }
  }
  
  private void setValueUnchecked(Object value)
  {
    Object valueToSet = value;
    if (value == null)
    {
      if (DataType.DT_DATE == discriminator || DataType.DT_STRING == discriminator)
      {
        valueToSet = ""; 
      }
    }
    else if (DataType.DT_STRING == discriminator)
    {
      valueToSet = value.toString();
    }
    else if (DataType.DT_LONG == discriminator)
    {
      if (!(value instanceof Integer))
      {
        valueToSet = Math.toIntExact((long)value);
      }
    }
    else if (DataType.DT_LONGLONG == discriminator)
    {
      if (value instanceof Integer)
      {
        valueToSet = (int)value;
      }
    }
    else if (DataType.DS_FLOAT == discriminator)
    {
      valueToSet = convertToFloatSequence(value);
    }
    else if (DataType.DS_LONGLONG == discriminator)
    {
      if (value instanceof Long)
      {
        // tolerate a single value and transform to array
        valueToSet = new long[] {(long)value};
      }
    }
    this.value = new BasicValue<>(valueToSet);
  }
  
  private float[] convertToFloatSequence(Object value)
  {
    if (value instanceof float[])
    {
        return (float[])value;
    }
    else if (value instanceof long[])
    {
        long[] longlongArray = (long[])value;
      float[] convertedValues = new float[ArrayUtils.getLength(value)];
      for (int i = 0; i < convertedValues.length; i++)
      {
        convertedValues[i] = longlongArray[i];
      }
      return convertedValues;
    }
    else if (value instanceof double[])
    {
        double[] doubleArray = (double[])value;
      float[] convertedValues = new float[ArrayUtils.getLength(value)];
      for (int i = 0; i < convertedValues.length; i++)
      {
        double val = doubleArray[i];
        if (val > Float.MAX_VALUE)
        {
          throw new OpenAtfxException(ErrorCode.AO_BAD_OPERATION, "Tolerant conversion of double value " + value + " to float impossible, since the value is too big!");
        }
        convertedValues[i] = (float)val;
      }
      return convertedValues;
    }
    throw new OpenAtfxException(
        ErrorCode.AO_NOT_IMPLEMENTED, "Value conversion from type " + value.getClass().getSimpleName() + " not yet supported or not possible");
  }
  
  public void removeValue(Object value)
  {
    if (discriminator.isSequenceType())
    {
      int index = 0;
      switch (discriminator)
      {
      case DS_LONG:
        int[] longSeq = (int[])this.value.getValue();
        index = ArrayUtils.indexOf(longSeq, (int)value);
        if (index >= 0)
        {
          this.value = new BasicValue<>(ArrayUtils.remove(longSeq, index));
        }
        break;
      case DS_LONGLONG:
        long[] longlongSeq = (long[])this.value.getValue();
        index = ArrayUtils.indexOf(longlongSeq, (long)value);
        if (index >= 0)
        {
          this.value = new BasicValue<>(ArrayUtils.remove(longlongSeq, index));
        }
        break;
      default:
          throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                      "DataType not supported at removeValue(): " + discriminator);
      }
    }
    else
    {
      setValue(null);
    }
  }
  
  /**
   * Adds the given value to this value's sequence. Throws exception if this is
   * not a sequence {@link DataType}.
   * 
   * @param value
   */
  public void addValueToSequence(Object value)
  {
    if (!discriminator.isSequenceType())
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                    "NameValueUnit.addValueToSequence() called for non-sequence datatype "
                                            + discriminator);
    }
    
    Object newValue = null;
    if (DataType.DS_LONG == discriminator
        || DataType.DS_ENUM == discriminator)
    {
      if (value instanceof Integer)
      {
        int[] longSeq = (int[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(longSeq, (int)value);
        }
        else
        {
          newValue = new int[] {(int)value};
        }
      }
    }
    else if (DataType.DS_LONGLONG == discriminator)
    {
      if (value instanceof Long)
      {
        long[] longlongSeq = (long[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(longlongSeq, (long)value);
        }
        else
        {
          newValue = new long[] {(long)value};
        }
      }
    }
    else if (DataType.DS_SHORT == discriminator)
    {
      if (value instanceof Short)
      {
        short[] shortSeq = (short[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(shortSeq, (short)value);
        }
        else
        {
          newValue = new short[] {(short)value};
        }
      }
    }
    else if (DataType.DS_STRING == discriminator
        || (DataType.DS_DATE == discriminator))
    {
      if (value instanceof String)
      {
        String[] stringSeq = (String[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(stringSeq, String.valueOf(value));
        }
        else
        {
          newValue = new String[] {String.valueOf(value)};
        }
      }
    }
    else if (DataType.DS_FLOAT == discriminator)
    {
      if (value instanceof Float)
      {
        float[] floatSeq = (float[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(floatSeq, (float)value);
        }
        else
        {
          newValue = new float[] {(float)value};
        }
      }
    }
    else if (DataType.DS_DOUBLE == discriminator)
    {
      if (value instanceof Double)
      {
        double[] doubleSeq = (double[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(doubleSeq, (double)value);
        }
        else
        {
          newValue = new double[] {(double)value};
        }
      }
    }
    else if (DataType.DS_BOOLEAN == discriminator)
    {
      if (value instanceof Boolean)
      {
        boolean[] booleanSeq = (boolean[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(booleanSeq, (boolean)value);
        }
        else
        {
          newValue = new boolean[] {(boolean)value};
        }
      }
    }
    else if (DataType.DS_COMPLEX == discriminator)
    {
      if (value instanceof Complex)
      {
          Complex complexVal = (Complex)value;
        Complex[] complexSeq = (Complex[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(complexSeq, complexVal);
        }
        else
        {
          newValue = new Complex[] {complexVal};
        }
      }
    }
    else if (DataType.DS_DCOMPLEX == discriminator)
    {
      if (value instanceof DoubleComplex)
      {
          DoubleComplex dComplexVal = (DoubleComplex)value;
        DoubleComplex[] dcomplexSeq = (DoubleComplex[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(dcomplexSeq, dComplexVal);
        }
        else
        {
          newValue = new DoubleComplex[] {dComplexVal};
        }
      }
    }
    else if (DataType.DS_EXTERNALREFERENCE == discriminator)
    {
      if (value instanceof ExternalReference)
      {
        ExternalReference extRefVal = (ExternalReference)value;
        ExternalReference[] extRefSeq = (ExternalReference[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(extRefSeq, extRefVal);
        }
        else
        {
          newValue = new ExternalReference[] {extRefVal};
        }
      }
    }
    else if (DataType.DS_BYTE == discriminator)
    {
      if (value instanceof Byte)
      {
        byte[] byteSeq = (byte[])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(byteSeq, (byte)value);
        }
        else
        {
          newValue = new byte[] {(byte)value};
        }
      }
    }
    else if (DataType.DS_BYTESTR == discriminator)
    {
      if (value instanceof byte[])
      {
        byte[] byteArrayVal = (byte[])value;
        byte[][] bytestrSeq = (byte[][])this.value.getValue();
        if (isValid())
        {
          newValue = ArrayUtils.add(bytestrSeq, byteArrayVal);
        }
        else
        {
          newValue = new byte[][] {byteArrayVal};
        }
      }
    }
    else
    {
      throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "addValueToSequence() not implemented for DataType " + discriminator);
    }
    
    if (newValue != null)
    {
      setValue(newValue);
    }
  }
  
  private boolean isInvalidValue(Object value)
  {
    return value == null || value.toString().isEmpty();
  }
  
  public Object getValue()
  {
    if ((short)15 != getFlag())
    {
      return null;
    }
    return value.getValue();
  }
  
  public List<Object> getValuesSequence()
  {
    if ((short)15 != getFlag())
    {
      return new ArrayList<>();
    }
    
    if (!discriminator.isSequenceType())
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                    "SingleValue.getValueSequence() cannot be called for non-sequence datatypes, this values has "
                                            + discriminator);
    }
    
    Object arrayValue = value.getValue();
    if (!arrayValue.getClass().isArray())
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Value of a sequence type value "
                + discriminator + " unexpectedly is no array: " + arrayValue);
    }
    
    List<Object> values = new ArrayList<>();
    switch (discriminator)
    {
    case DS_BOOLEAN:
      for (Boolean b : (boolean[])arrayValue)
      {
        values.add(b);
      }
      return values;
    case DS_BYTE:
      for (Byte b : (byte[])arrayValue)
      {
        values.add(b);
      }
      return values;
    case DS_BYTESTR:
      return Arrays.asList((Object[])(byte[][])arrayValue);
    case DS_COMPLEX:
      return Arrays.asList((Object[])(Complex[])arrayValue);
    case DS_DATE:
      return Arrays.asList((Object[])(String[])arrayValue);
    case DS_DCOMPLEX:
      return Arrays.asList((Object[])(DoubleComplex[])arrayValue);
    case DS_DOUBLE:
      return Arrays.stream((double[])arrayValue).boxed().collect(Collectors.toList());
    case DS_ENUM:
      return Arrays.stream((int[])arrayValue).boxed().collect(Collectors.toList());
    case DS_EXTERNALREFERENCE:
      return Arrays.asList((Object[])(ExternalReference[])arrayValue);
    case DS_FLOAT:
      for (Float f : (float[])arrayValue)
      {
        values.add(f);
      }
      return values;
    case DS_LONG:
      return Arrays.stream((int[])arrayValue).boxed().collect(Collectors.toList());
    case DS_LONGLONG:
      return Arrays.stream((long[])arrayValue).boxed().collect(Collectors.toList());
    case DS_SHORT:
      for (Short s : (short[])arrayValue)
      {
        values.add(s);
      }
      return values;
    case DS_STRING:
      return Arrays.asList((Object[])(String[])arrayValue);
    default:
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM,
                                    "SingleValue.getValueSequence() cannot be called for non-sequence datatypes, this values has "
                                            + discriminator);
}
  }
  
  public static Object getEmptyValue(DataType discriminator)
  {
    switch (discriminator)
    {
    case DT_BLOB:
      return null;
    case DT_BOOLEAN:
      return false;
    case DT_BYTE:
      return (byte) 0;
    case DT_BYTESTR:
      return new byte[0];
    case DT_COMPLEX:
      return new Complex(0, 0);
    case DT_DATE:
      return "";
    case DT_DCOMPLEX:
      return new DoubleComplex(0, 0);
    case DT_DOUBLE:
      return 0d;
    case DT_ENUM:
      return 0;
    case DT_EXTERNALREFERENCE:
      return new ExternalReference("", "", "");
    case DT_FLOAT:
      return 0f;
    case DT_LONG:
      return 0;
    case DT_LONGLONG:
      return 0L;
    case DT_SHORT:
      return (short)0;
    case DT_STRING:
      return "";
    case DS_BOOLEAN:
      return new boolean[0];
    case DS_BYTE:
      return new byte[0];
    case DS_BYTESTR:
      return new byte[0][];
    case DS_COMPLEX:
      return new Complex[0];
    case DS_DATE:
        return new String[0];
    case DS_STRING:
      return new String[0];
    case DS_DCOMPLEX:
      return new DoubleComplex[0];
    case DS_DOUBLE:
      return new double[0];
    case DS_ENUM:
        return new int[0];
    case DS_LONG:
      return new int[0];
    case DS_EXTERNALREFERENCE:
      return new ExternalReference[0];
    case DS_FLOAT:
      return new float[0];
    case DS_LONGLONG:
      return new long[0];
    case DS_SHORT:
      return new short[0];
    default:
      throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "DataType not supported: " + discriminator);
    }
  }
  
  public SingleValue getSequenceValue(int index)
  {
    if (discriminator.isSequenceType() && Array.getLength(getValue()) > index)
    {
      Object val = Array.get(getValue(), index);
      return new SingleValue(discriminator.getAccordingSingleType(), val);
    }
    return null;
  }
  
  /**
   * Specifically for the tolerance of ID values being DT_LONG or DT_LONGLONG.
   * Will return the value as long no matter which actual datatype it has.
   * 
   * @return
   */
  public long getLongValue()
  {
    if (DataType.DT_LONG == discriminator || DataType.DT_LONGLONG == discriminator)
    {
      return ((Number) value.getValue()).longValue();
    }
    throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Unsupported datatype to get a long value from: " + discriminator);
  }
  
  /**
   * Specifically for the tolerance of ID values being DS_LONG or DS_LONGLONG in
   * relations. Will return the values as longs no matter which actual datatype
   * they have.
   * 
   * @return
   */
  public long[] getLongValues()
  {
    if (DataType.DS_LONG == discriminator)
    {
      return Arrays.stream(longSeq()).asLongStream().toArray();
    }
    else if (DataType.DS_LONGLONG == discriminator)
    {
      return longlongSeq();
    }
    throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Unsupported datatype to get long values from: " + discriminator);
  }

  public String stringVal()
  {
    if (discriminator != DataType.DT_STRING)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (String)value.getValue();
  }

  public void stringVal(String val)
  {
    discriminator = DataType.DT_STRING;
    setValue(val);
  }

  public short shortVal()
  {
    if (discriminator != DataType.DT_SHORT)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    short val = (short)value.getValue();
    return isValid() ? val : 0;
  }

  public void shortVal(short val)
  {
    discriminator = DataType.DT_SHORT;
    setValue(val);
  }

  public float floatVal()
  {
    if (discriminator != DataType.DT_FLOAT)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (float)value.getValue();
  }

  public void floatVal(float val)
  {
    discriminator = DataType.DT_FLOAT;
    setValue(val);
  }

  public byte byteVal()
  {
    if (discriminator != DataType.DT_BYTE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (byte)value.getValue();
  }

  public void byteVal(byte val)
  {
    discriminator = DataType.DT_BYTE;
    setValue(val);
  }

  public boolean booleanVal()
  {
    if (discriminator != DataType.DT_BOOLEAN)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (boolean)value.getValue();
  }

  public void booleanVal(boolean val)
  {
    discriminator = DataType.DT_BOOLEAN;
    setValue(val);
  }

  public int longVal()
  {
    if (discriminator != DataType.DT_LONG)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (int)value.getValue();
  }

  public void longVal(int val)
  {
    discriminator = DataType.DT_LONG;
    setValue(val);
  }

  public double doubleVal()
  {
    if (discriminator != DataType.DT_DOUBLE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (double)value.getValue();
  }

  public void doubleVal(double val)
  {
    discriminator = DataType.DT_DOUBLE;
    setValue(val);
  }

  public long longlongVal()
  {
    if (discriminator != DataType.DT_LONGLONG)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (long)value.getValue();
  }

  public void longlongVal(long val)
  {
    discriminator = DataType.DT_LONGLONG;
    setValue(val);
  }

  public Complex complexVal()
  {
    if (discriminator != DataType.DT_COMPLEX)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (Complex)value.getValue();
  }

  public void complexVal(Complex val)
  {
    discriminator = DataType.DT_COMPLEX;
    setValue(val);
  }

  public DoubleComplex dcomplexVal()
  {
    if (discriminator != DataType.DT_DCOMPLEX)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (DoubleComplex)value.getValue();
  }

  public void dcomplexVal(DoubleComplex val)
  {
    discriminator = DataType.DT_DCOMPLEX;
    setValue(val);
  }

  public String dateVal()
  {
    if (discriminator != DataType.DT_DATE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (String)value.getValue();
  }

  public void dateVal(String val)
  {
    discriminator = DataType.DT_DATE;
    setValue(val);
  }

  public byte[] bytestrVal()
  {
    if (discriminator != DataType.DT_BYTESTR)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (byte[])value.getValue();
  }

  public void bytestrVal(byte[] val)
  {
    discriminator = DataType.DT_BYTESTR;
    setValue(val);
  }

  public Blob blobVal()
  {
    if (discriminator != DataType.DT_BLOB)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (Blob)value.getValue();
  }

  public void blobVal(Blob val)
  {
    discriminator = DataType.DT_BLOB;
    setValue(val);
  }

  public String[] stringSeq()
  {
    if (discriminator != DataType.DS_STRING)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (String[])value.getValue();
  }

  public void stringSeq(String[] seq)
  {
    discriminator = DataType.DS_STRING;
    setValue(seq);
  }

  public short[] shortSeq()
  {
    if (discriminator != DataType.DS_SHORT)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (short[])value.getValue();
  }

  public void shortSeq(short[] seq)
  {
    discriminator = DataType.DS_SHORT;
    setValue(seq);
  }

  public float[] floatSeq()
  {
    if (discriminator != DataType.DS_FLOAT)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (float[])value.getValue();
  }

  public void floatSeq(float[] seq)
  {
    discriminator = DataType.DS_FLOAT;
    setValue(seq);
  }

  public byte[] byteSeq()
  {
    if (discriminator != DataType.DS_BYTE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (byte[])value.getValue();
  }

  public void byteSeq(byte[] seq)
  {
    discriminator = DataType.DS_BYTE;
    setValue(seq);
  }

  public boolean[] booleanSeq()
  {
    if (discriminator != DataType.DS_BOOLEAN)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (boolean[])value.getValue();
  }

  public void booleanSeq(boolean[] seq)
  {
    discriminator = DataType.DS_BOOLEAN;
    setValue(seq);
  }

  public int[] longSeq()
  {
    if (discriminator != DataType.DS_LONG)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (int[])value.getValue();
  }

  public void longSeq(int[] seq)
  {
    discriminator = DataType.DS_LONG;
    setValue(seq);
  }

  public double[] doubleSeq()
  {
    if (discriminator != DataType.DS_DOUBLE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (double[])value.getValue();
  }

  public void doubleSeq(double[] seq)
  {
    discriminator = DataType.DS_DOUBLE;
    setValue(seq);
  }

  public long[] longlongSeq()
  {
    if (discriminator != DataType.DS_LONGLONG)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (long[])value.getValue();
  }

  public void longlongSeq(long[] seq)
  {
    discriminator = DataType.DS_LONGLONG;
    setValue(seq);
  }

  public Complex[] complexSeq()
  {
    if (discriminator != DataType.DS_COMPLEX)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (Complex[])value.getValue();
  }

  public void complexSeq(Complex[] seq)
  {
    discriminator = DataType.DS_COMPLEX;
    setValue(seq);
  }

  public DoubleComplex[] dcomplexSeq()
  {
    if (discriminator != DataType.DS_DCOMPLEX)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (DoubleComplex[])value.getValue();
  }

  public void dcomplexSeq(DoubleComplex[] seq)
  {
    discriminator = DataType.DS_DCOMPLEX;
    setValue(seq);
  }

  public String[] dateSeq()
  {
    if (discriminator != DataType.DS_DATE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (String[])value.getValue();
  }

  public void dateSeq(String[] seq)
  {
    discriminator = DataType.DS_DATE;
    setValue(seq);
  }

  public byte[][] bytestrSeq()
  {
    if (discriminator != DataType.DS_BYTESTR)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (byte[][])value.getValue();
  }

  public void bytestrSeq(byte[][] seq)
  {
    discriminator = DataType.DS_BYTESTR;
    setValue(seq);
  }

  public ExternalReference extRefVal()
  {
    if (discriminator != DataType.DT_EXTERNALREFERENCE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (ExternalReference)value.getValue();
  }

  public void extRefVal(ExternalReference val)
  {
    discriminator = DataType.DT_EXTERNALREFERENCE;
    setValue(val);
  }

  public ExternalReference[] extRefSeq()
  {
    if (discriminator != DataType.DS_EXTERNALREFERENCE)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (ExternalReference[])value.getValue();
  }

  public void extRefSeq(ExternalReference[] seq)
  {
    discriminator = DataType.DS_EXTERNALREFERENCE;
    setValue(seq);
  }

  public int enumVal()
  {
    if (discriminator != DataType.DT_ENUM)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    int enumVal = (int)value.getValue();
    return isValid() ? enumVal : 0;
  }

  public void enumVal(int val)
  {
    discriminator = DataType.DT_ENUM;
    setValue(val);
  }

  public int[] enumSeq()
  {
    if (discriminator != DataType.DS_ENUM)
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, MSG_WRONG_DATATYPE);
    return (int[])value.getValue();
  }

  public void enumSeq(int[] seq)
  {
    discriminator = DataType.DS_ENUM;
    setValue(seq);
  }
  
  public SingleValue getSubsetOfSequenceValue(int startIndex, int length)
  {
    if (!discriminator.isSequenceType())
    {
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, 
          "Cannot get a subset of values for single value datatypes, but was called with " + discriminator);
    }
    if (startIndex >= getLength())
    {
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, 
          "Requested subset beginning at index " + startIndex + " from sequence value of length " + getLength());
    }

    Object valueRange = getRange(startIndex, length);
    SingleValue subset = new SingleValue(this);
    subset.setValue(valueRange);
    return subset;
  }
  
  private Object getRange(int startIndex, int length)
  {
    switch (discriminator)
    {
    case DS_BOOLEAN:
      return Arrays.copyOfRange((boolean[])value.getValue(), startIndex, length);
    case DS_BYTE:
      return Arrays.copyOfRange((byte[])value.getValue(), startIndex, length);
    case DS_BYTESTR:
      return Arrays.copyOfRange((byte[][])value.getValue(), startIndex, length);
    case DS_COMPLEX:
      return Arrays.copyOfRange((Complex[])value.getValue(), startIndex, length);
    case DS_DATE:
      return Arrays.copyOfRange((String[])value.getValue(), startIndex, length);
    case DS_DCOMPLEX:
      return Arrays.copyOfRange((DoubleComplex[])value.getValue(), startIndex, length);
    case DS_DOUBLE:
      return Arrays.copyOfRange((double[])value.getValue(), startIndex, length);
    case DS_ENUM:
      return Arrays.copyOfRange((int[])value.getValue(), startIndex, length);
    case DS_EXTERNALREFERENCE:
      return Arrays.copyOfRange((ExternalReference[])value.getValue(), startIndex, length);
    case DS_FLOAT:
      return Arrays.copyOfRange((float[])value.getValue(), startIndex, length);
    case DS_LONG:
      return Arrays.copyOfRange((int[])value.getValue(), startIndex, length);
    case DS_LONGLONG:
      return Arrays.copyOfRange((long[])value.getValue(), startIndex, length);
    case DS_SHORT:
      return Arrays.copyOfRange((short[])value.getValue(), startIndex, length);
    case DS_STRING:
      return Arrays.copyOfRange((String[])value.getValue(), startIndex, length);
    default:
      throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "DataType not supported at TSUnion.getValue(): " + discriminator);
    }
  }
  
  public String valueToString()
  {
    switch (discriminator)
    {
    case DT_BOOLEAN:
    case DT_DATE:
    case DT_DOUBLE:
    case DT_ENUM:
    case DT_FLOAT:
    case DT_LONG:
    case DT_LONGLONG:
    case DT_SHORT:
    case DT_STRING:
      return String.valueOf(value.getValue());
    case DT_BYTE:
      return ODSHelper.byteToHex((byte)value.getValue());
    case DT_COMPLEX:
      StringBuilder complexStr = new StringBuilder();
      complexStr.append(((Complex)value.getValue()).getR());
      complexStr.append(VALUE_SEPARATOR_STRING.charAt(0));
      complexStr.append(((Complex)value.getValue()).getI());
      return complexStr.toString();
    case DT_DCOMPLEX:
      StringBuilder dComplexString = new StringBuilder();
      dComplexString.append(((DoubleComplex)value.getValue()).getR());
      dComplexString.append(VALUE_SEPARATOR_STRING.charAt(0));
      dComplexString.append(((DoubleComplex)value.getValue()).getI());
      return dComplexString.toString();
    case DT_EXTERNALREFERENCE:
      return ((ExternalReference)value.getValue()).asStringValue();
    case DT_BYTESTR:
      StringBuilder bytestrString = new StringBuilder();
      byte[] byteStreamValue = (byte[])value.getValue();
      for (int i = 0; i < byteStreamValue.length; i++)
      {
        bytestrString.append(ODSHelper.byteToHex(byteStreamValue[i]));
        if (i < byteStreamValue.length - 1)
        {
          bytestrString.append(VALUE_SEPARATOR_STRING.charAt(0));
        }
      }
      return bytestrString.toString();
    case DS_BOOLEAN:
      StringBuilder booleanSeqStr = new StringBuilder();
      boolean[] booleanSeq = (boolean[])value.getValue();
      for (int i = 0; i < booleanSeq.length; i++)
      {
        booleanSeqStr.append(booleanSeq[i]);
        if (i < booleanSeq.length - 1)
        {
          booleanSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return booleanSeqStr.toString();
    case DS_BYTE:
      StringBuilder byteSeqStr = new StringBuilder();
      byte[] byteSeq = (byte[])value.getValue();
      for (int i = 0; i < byteSeq.length; i++)
      {
        byteSeqStr.append(ODSHelper.byteToHex(byteSeq[i]));
        if (i < byteSeq.length - 1)
        {
          byteSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return byteSeqStr.toString();
    case DS_BYTESTR:
      StringBuilder bytestrSeqStr = new StringBuilder();
      byte[][] bytestrSeq = (byte[][])value.getValue();
      for (int x = 0; x < bytestrSeq.length; x++)
      {
        byte[] ar = bytestrSeq[x];
        for (int i = 0; i < ar.length; i++)
        {
          bytestrSeqStr.append(ODSHelper.byteToHex(ar[i]));
          if (i < ar.length - 1)
          {
            bytestrSeqStr.append(VALUE_SEPARATOR_STRING.charAt(0));
          }
        }
        if (x < bytestrSeq.length - 1)
        {
          bytestrSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return bytestrSeqStr.toString();
    case DS_COMPLEX:
      StringBuilder complexSeqStr = new StringBuilder();
      Complex[] complexSeq = (Complex[])value.getValue();
      for (int i = 0; i < complexSeq.length; i++)
      {
        complexSeqStr.append(complexSeq[i].getR());
        complexSeqStr.append(VALUE_SEPARATOR_STRING.charAt(0));
        complexSeqStr.append(complexSeq[i].getI());
        if (i < complexSeq.length - 1)
        {
          complexSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return complexSeqStr.toString();
    case DS_DATE:
      StringBuilder dateSeqStr = new StringBuilder();
      String[] dateSeq = (String[])value.getValue();
      for (int i = 0; i < dateSeq.length; i++)
      {
        dateSeqStr.append(dateSeq[i]);
        if (i < dateSeq.length - 1)
        {
          dateSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return dateSeqStr.toString();
    case DS_DCOMPLEX:
      StringBuilder dComplexSeqStr = new StringBuilder();
      DoubleComplex[] dComplexSeq = (DoubleComplex[])value.getValue();
      for (int i = 0; i < dComplexSeq.length; i++)
      {
        dComplexSeqStr.append(dComplexSeq[i].getR());
        dComplexSeqStr.append(VALUE_SEPARATOR_STRING.charAt(0));
        dComplexSeqStr.append(dComplexSeq[i].getI());
        if (i < dComplexSeq.length - 1)
        {
          dComplexSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return dComplexSeqStr.toString();
    case DS_DOUBLE:
      StringBuilder doubleSeqStr = new StringBuilder();
      double[] doubleSeq = (double[])value.getValue();
      for (int i = 0; i < doubleSeq.length; i++)
      {
        doubleSeqStr.append(doubleSeq[i]);
        if (i < doubleSeq.length - 1)
        {
          doubleSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return doubleSeqStr.toString();
    case DS_ENUM:
      StringBuilder enumSeqStr = new StringBuilder();
      int[] enumSeq = (int[])value.getValue();
      for (int i = 0; i < enumSeq.length; i++)
      {
        enumSeqStr.append(enumSeq[i]);
        if (i < enumSeq.length - 1)
        {
          enumSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return enumSeqStr.toString();
    case DS_EXTERNALREFERENCE:
      StringBuilder extRefSeqStr = new StringBuilder();
      ExternalReference[] extRefSeq = (ExternalReference[])value.getValue();
      for (int i = 0; i < extRefSeq.length; i++)
      {
        ExternalReference extRef = extRefSeq[i];
        if (extRef != null)
        {
          if (i > 0)
          {
            extRefSeqStr.append(SEQ_SEPARATOR_STRING);
          }
          extRefSeqStr.append(extRef.asStringValue());
        }
      }
      return extRefSeqStr.toString();
    case DS_FLOAT:
      StringBuilder floatSeqStr = new StringBuilder();
      float[] floatSeq = (float[])value.getValue();
      for (int i = 0; i < floatSeq.length; i++)
      {
        floatSeqStr.append(floatSeq[i]);
        if (i < floatSeq.length - 1)
        {
          floatSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return floatSeqStr.toString();
    case DS_LONG:
      StringBuilder longSeqStr = new StringBuilder();
      int[] longSeq = (int[])value.getValue();
      for (int i = 0; i < longSeq.length; i++)
      {
        longSeqStr.append(longSeq[i]);
        if (i < longSeq.length - 1)
        {
          longSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return longSeqStr.toString();
    case DS_LONGLONG:
      StringBuilder longlongSeqStr = new StringBuilder();
      long[] longlongSeq = (long[])value.getValue();
      for (int i = 0; i < longlongSeq.length; i++)
      {
        longlongSeqStr.append(longlongSeq[i]);
        if (i < longlongSeq.length - 1)
        {
          longlongSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return longlongSeqStr.toString();
    case DS_SHORT:
      StringBuilder shortSeqStr = new StringBuilder();
      short[] shortSeq = (short[])value.getValue();
      for (int i = 0; i < shortSeq.length; i++)
      {
        shortSeqStr.append(shortSeq[i]);
        if (i < shortSeq.length - 1)
        {
          shortSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return shortSeqStr.toString();
    case DS_STRING:
      StringBuilder stringSeqStr = new StringBuilder();
      String[] stringSeq = (String[])value.getValue();
      for (int i = 0; i < stringSeq.length; i++)
      {
        stringSeqStr.append(ODSHelper.escapeSeparatorCharInStringSequence(stringSeq[i]));
        if (i < stringSeq.length - 1)
        {
          stringSeqStr.append(SEQ_SEPARATOR_STRING);
        }
      }
      return stringSeqStr.toString();
    default:
      throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "DataType not supported: " + discriminator);
    }
  }
  
  @Override
  public int hashCode()
  {
    return Objects.hash(discriminator, flag, value);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingleValue other = (SingleValue) obj;
    return discriminator == other.discriminator && flag == other.flag && Objects.equals(value, other.value);
  }

  /**
   * @see java.lang.Object#toString()
   *
   * @return
   */
  @Override
  public String toString()
  {
    String values = valueToString();
    if (values.length() > 50)
    {
      values = values.substring(0, 50) + "...";
    }
    
    return "SingleValue [flag=" + flag + ", discriminator=" + discriminator + ", value=" + values + "]";
  }
}
