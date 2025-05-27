package com.peaksolution.openatfx.api;

import java.util.Objects;

public class BasicValue<T>
{
  private T value;
  
  public BasicValue(T value)
  {
    this.value = value;
  }
  
  public T getValue()
  {
    return value;
  }
  
  public Long getLongValue()
  {
    if (value != null)
    {
      try
      {
        return (Long)value;
      }
      catch (Exception ex)
      {
        return ((Integer)value).longValue();
      }
    }
    return null;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(value);
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
    BasicValue<T> other = (BasicValue) obj;
    return Objects.equals(value, other.value);
  }
  
  @Override
  public String toString()
  {
    return value.toString();
  }
}
