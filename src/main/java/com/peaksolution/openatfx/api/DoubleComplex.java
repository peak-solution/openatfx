package com.peaksolution.openatfx.api;

import java.util.List;

public class DoubleComplex
{
  private double r;
  private double i;

  public DoubleComplex()
  {
  }

  public DoubleComplex(double r, double i)
  {
    this.setR(r);
    this.setI(i);
  }
  
  public static DoubleComplex[] getDoubleComplexValues(List<Double> values)
  {
    DoubleComplex[] complexArray = new DoubleComplex[values.size()];
    double r = 0;
    for (int i = 0; i < values.size(); i++)
    {
      if ((i + 1) % 2 == 0)
      {
        r = values.get(i);
      }
      else
      {
        complexArray[(i + 1) / 2] = new DoubleComplex(r, values.get(i));
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
    long temp;
    temp = Double.doubleToLongBits(getI());
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(getR());
    result = prime * result + (int) (temp ^ (temp >>> 32));
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
    DoubleComplex other = (DoubleComplex) obj;
    if (Double.doubleToLongBits(getI()) != Double.doubleToLongBits(other.getI()))
      return false;
    return Double.doubleToLongBits(getR()) == Double.doubleToLongBits(other.getR());
  }

  /**
   * @see java.lang.Object#toString()
   *
   * @return
   */
  @Override
  public String toString()
  {
    return "T_DCOMPLEX [r=" + getR() + ", i=" + getI() + "]";
  }

  /**
   * @return the r
   */
  public double getR()
  {
    return r;
  }

  /**
   * @param r
   *        the r to set
   */
  public void setR(double r)
  {
    this.r = r;
  }

  /**
   * @return the i
   */
  public double getI()
  {
    return i;
  }

  /**
   * @param i
   *        the i to set
   */
  public void setI(double i)
  {
    this.i = i;
  }
}
