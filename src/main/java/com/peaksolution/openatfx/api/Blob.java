package com.peaksolution.openatfx.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.asam.ods.ErrorCode;


public class Blob
{
  private List<Byte> content = new ArrayList<>();
  private String header = "";

  public void append(byte[] value)
  {
    for (byte b : value)
    {
      this.content.add(b);
    }
  }

  public boolean compare(Blob aBlob)
  {
    return Arrays.equals(get(0, getLength()), aBlob.get(0, aBlob.getLength()));
  }

  public byte[] get(int offset, int length)
  {
    if ((offset + length) > getLength())
    {
      throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "The index must be in the range from 0 to max.");
    }
    byte[] b = new byte[length];
    for (int i = 0; i < length; i++)
    {
      b[i] = this.content.get(offset + i);
    }
    return b;
  }

  public String getHeader()
  {
    return header;
  }

  public int getLength()
  {
    return this.content.size();
  }

  public void set(byte[] value)
  {
    this.content.clear();
    append(value);
  }

  public void setHeader(java.lang.String header)
  {
    this.header = header;
  }
}
