package com.peaksolution.openatfx.api;

import java.util.List;

public class Complex
{
  private float r;
  private float i;

  public Complex()
  {
  }

  public Complex(float r, float i)
  {
    this.setR(r);
    this.setI(i);
  }
  
  public static Complex[] getComplexValues(List<Float> values)
  {
    Complex[] complexArray = new Complex[values.size()];
    float r = 0;
    for (int i = 0; i < values.size(); i++)
    {
      if ((i + 1) % 2 == 0)
      {
        r = values.get(i);
      }
      else
      {
        complexArray[(i + 1) / 2] = new Complex(r, values.get(i));
      }
    }
    return complexArray;
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
    result = prime * result + Float.floatToIntBits(getI());
    result = prime * result + Float.floatToIntBits(getR());
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
    Complex other = (Complex) obj;
    if (Float.floatToIntBits(getI()) != Float.floatToIntBits(other.getI()))
      return false;
    return Float.floatToIntBits(getR()) == Float.floatToIntBits(other.getR());
  }

  /**
   * @see java.lang.Object#toString()
   *
   * @return
   */
  @Override
  public String toString()
  {
    return "T_COMPLEX [r=" + getR() + ", i=" + getI() + "]";
  }

  /**
   * @return the r
   */
  public float getR()
  {
    return r;
  }

  /**
   * @param r
   *        the r to set
   */
  public void setR(float r)
  {
    this.r = r;
  }

  /**
   * @return the i
   */
  public float getI()
  {
    return i;
  }

  /**
   * @param i
   *        the i to set
   */
  public void setI(float i)
  {
    this.i = i;
  }
}
