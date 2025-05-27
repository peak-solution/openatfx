package com.peaksolution.openatfx.api;

import org.asam.ods.ErrorCode;

public enum DataType
{
  DT_UNKNOWN("DT_UNKNOWN"),
  DT_STRING("DT_STRING"),     // 1
  DT_SHORT("DT_SHORT"),
  DT_FLOAT("DT_FLOAT"),
  DT_BOOLEAN("DT_BOOLEAN"),
  DT_BYTE("DT_BYTE"),         // 5
  DT_LONG("DT_LONG"),
  DT_DOUBLE("DT_DOUBLE"),
  DT_LONGLONG("DT_LONGLONG"),
  DT_ID("DT_ID"),
  DT_DATE("DT_DATE"),         // 10
  DT_BYTESTR("DT_BYTESTR"),
  DT_BLOB("DT_BLOB"),
  DT_COMPLEX("DT_COMPLEX"),
  DT_DCOMPLEX("DT_DCOMPLEX"),
  DS_STRING("DS_STRING"),     // 15
  DS_SHORT("DS_SHORT"),
  DS_FLOAT("DS_FLOAT"),
  DS_BOOLEAN("DS_BOOLEAN"),
  DS_BYTE("DS_BYTE"),
  DS_LONG("DS_LONG"),         // 20
  DS_DOUBLE("DS_DOUBLE"),
  DS_LONGLONG("DS_LONGLONG"),
  DS_COMPLEX("DS_COMPLEX"),
  DS_DCOMPLEX("DS_DCOMPLEX"),
  DS_ID("DS_ID"),             // 25
  DS_DATE("DS_DATE"),
  DS_BYTESTR("DS_BYTESTR"),
  DT_EXTERNALREFERENCE("DT_EXTERNALREFERENCE"),
  DS_EXTERNALREFERENCE("DS_EXTERNALREFERENCE"), 
  DT_ENUM("DT_ENUM"),         // 30
  DS_ENUM("DS_ENUM");

  private final String name;

  private DataType(String name)
  {
    this.name = name;
  }

  public static DataType fromString(String typeString)
  {
    for (DataType dt : values())
    {
      if (dt.name.equals(typeString))
      {
        return dt;
      }
    }
    throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "No data type can be identified by String '" + typeString + "'!");
  }

  public static DataType getByIndex(int i)
  {
    return DataType.values()[i];
  }
  
  public boolean isNumberType()
  {
    switch(this)
    {
    case DS_BOOLEAN:
    case DS_BYTESTR:
    case DS_DATE:
    case DS_EXTERNALREFERENCE:
    case DS_STRING:
    case DT_BLOB:
    case DT_BOOLEAN:
    case DT_BYTESTR:
    case DT_DATE:
    case DT_EXTERNALREFERENCE:
    case DT_STRING:
    case DT_UNKNOWN:
      return false;
    default:
      return true;
    }
  }
  
  public boolean isSequenceType()
  {
    return this.name().startsWith("DS_");
  }
  
  public DataType getAccordingSequenceType()
  {
    switch (this)
    {
    case DT_BOOLEAN:
      return DS_BOOLEAN;
    case DT_BYTE:
      return DS_BYTE;
    case DT_BYTESTR:
      return DS_BYTESTR;
    case DT_COMPLEX:
      return DS_COMPLEX;
    case DT_DATE:
      return DS_DATE;
    case DT_DCOMPLEX:
      return DS_DCOMPLEX;
    case DT_DOUBLE:
      return DS_DOUBLE;
    case DT_ENUM:
      return DS_ENUM;
    case DT_EXTERNALREFERENCE:
      return DS_EXTERNALREFERENCE;
    case DT_FLOAT:
      return DS_FLOAT;
    case DT_LONG:
      return DS_LONG;
    case DT_LONGLONG:
      return DS_LONGLONG;
    case DT_SHORT:
      return DS_SHORT;
    case DT_STRING:
      return DS_STRING;
    default:
      return this;
    }
  }
  
  public DataType getAccordingSingleType()
  {
    switch (this)
    {
    case DS_BOOLEAN:
      return DT_BOOLEAN;
    case DS_BYTE:
      return DT_BYTE;
    case DS_BYTESTR:
      return DT_BYTESTR;
    case DS_COMPLEX:
      return DT_COMPLEX;
    case DS_DATE:
      return DT_DATE;
    case DS_DCOMPLEX:
      return DT_DCOMPLEX;
    case DS_DOUBLE:
      return DT_DOUBLE;
    case DS_ENUM:
      return DT_ENUM;
    case DS_EXTERNALREFERENCE:
      return DT_EXTERNALREFERENCE;
    case DS_FLOAT:
      return DT_FLOAT;
    case DS_LONG:
      return DT_LONG;
    case DS_LONGLONG:
      return DT_LONGLONG;
    case DS_SHORT:
      return DT_SHORT;
    case DS_STRING:
      return DT_STRING;
    default:
      return this;
    }
  }
  
  @Override
  public String toString()
  {
    return name;
  }
}
