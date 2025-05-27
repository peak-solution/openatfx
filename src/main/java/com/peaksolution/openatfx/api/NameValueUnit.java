package com.peaksolution.openatfx.api;


public class NameValueUnit {
    private String valName;
    private SingleValue value;
    private String unit;
    private boolean isInstanceAttribute;

    public NameValueUnit()
    {
    }

    public NameValueUnit(String valName, SingleValue value)
    {
      this(valName, value, "");
    }
    
    public NameValueUnit(String valName, SingleValue value, String unit)
    {
      this.setValName(valName);
      this.setValue(value);
      this.setUnit(unit);
    }
    
    public NameValueUnit(String valName, DataType dt, Object value)
    {
      this(valName, dt, value, "");
    }
    
    public NameValueUnit(String valName, DataType dt, Object value, String unit)
    {
      this.setValName(valName);
      this.setValue(new SingleValue(dt, value));
      this.setUnit(unit);
    }
    
    public NameValueUnit(NameValueUnit nvu)
    {
      this.valName = nvu.getValName();
      this.unit = nvu.getUnit();
      this.isInstanceAttribute = nvu.isInstanceAttribute();
      setValue(new SingleValue(nvu.getValue()));
    }
    
    public NameValueUnit(NameValueUnit nvu, SingleValue value)
    {
      this.valName = nvu.getValName();
      this.unit = nvu.getUnit();
      this.isInstanceAttribute = nvu.isInstanceAttribute();
      setValue(value);
    }
    
    /**
     * @return
     */
    public boolean isInstanceAttribute()
    {
      return isInstanceAttribute;
    }

    /**
     * @param isInstanceAttribute
     */
    public void setInstanceAttribute(boolean isInstanceAttribute)
    {
      this.isInstanceAttribute = isInstanceAttribute;
    }

    /**
     * @return the valName
     */
    public String getValName()
    {
      return valName;
    }

    /**
     * @param valName
     *        the valName to set
     */
    public void setValName(String valName)
    {
      this.valName = valName;
    }
    
    public boolean isValid()
    {
      if (value != null)
      {
        return value.isValid();
      }
      return false;
    }
    
    /**
     * @return true, if a value is set and valid
     */
    public boolean hasValidValue()
    {
      return value != null && value.isValid();
    }

    /**
     * @return the value
     */
    public SingleValue getValue()
    {
      return value;
    }
    
    public NameValueUnit getSubsetOfSequenceValue(int startIndex, int length)
    {
      return new NameValueUnit(this, value.getSubsetOfSequenceValue(startIndex, length));
    }
    
    public int getValueLength()
    {
      return value.getLength();
    }

    /**
     * @param value
     *        the value to set
     */
    public void setValue(SingleValue value)
    {
      this.value = value;
    }

    /**
     * @return the unit
     */
    public String getUnit()
    {
      return unit;
    }

    /**
     * @param unit
     *        the unit to set
     */
    public void setUnit(String unit)
    {
      this.unit = unit;
    }

    /**
     * @see java.lang.Object#toString()
     *
     * @return
     */
    @Override
    public String toString()
    {
      DataType dt = value.discriminator();
      return "NameValueUnit [valName=" + valName + ", DataType=" + dt + ", value=" + value + ", unit=" + unit + "]";
    }

    /**
     * @see java.lang.Object#hashCode()
     *
     * @return
     */
    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
      result = prime * result + ((valName == null) ? 0 : valName.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      NameValueUnit other = (NameValueUnit) obj;
      if (unit == null)
      {
        if (other.unit != null)
          return false;
      }
      else if (!unit.equals(other.unit))
        return false;
      if (valName == null)
      {
        if (other.valName != null)
          return false;
      }
      else if (!valName.equals(other.valName))
        return false;
      if (value == null)
      {
        if (other.value != null)
          return false;
      }
      else if (!value.equals(other.value))
        return false;
      return true;
    }
}
