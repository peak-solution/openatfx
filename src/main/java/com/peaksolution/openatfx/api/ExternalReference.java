package com.peaksolution.openatfx.api;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.asam.ods.ErrorCode;


public class ExternalReference
{
  private java.lang.String description;
  private java.lang.String mimeType;
  private java.lang.String location;
  
  public ExternalReference()
  {
    setDescription("");
    setMimeType("");
    setLocation("");
  }

  public ExternalReference(java.lang.String description, java.lang.String mimeType, java.lang.String location)
  {
    this.setDescription(description);
    this.setMimeType(mimeType);
    this.setLocation(location);
  }
  
  public ExternalReference(List<Object> httpValues)
  {
    if (httpValues.size() < 3)
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Invalid number of Strings received in ExternalReference constructor: " + httpValues.size());
    }
    
    this.setDescription(httpValues.get(0).toString());
    this.setMimeType(httpValues.get(1).toString());
    this.setLocation(httpValues.get(2).toString());
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
    result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
    result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
    result = prime * result + ((getMimeType() == null) ? 0 : getMimeType().hashCode());
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
    ExternalReference other = (ExternalReference) obj;
    if (getDescription() == null)
    {
      if (other.getDescription() != null)
        return false;
    }
    else if (!getDescription().equals(other.getDescription()))
      return false;
    if (getLocation() == null)
    {
      if (other.getLocation() != null)
        return false;
    }
    else if (!getLocation().equals(other.getLocation()))
      return false;
    if (getMimeType() == null)
    {
      if (other.getMimeType() != null)
        return false;
    }
    else if (!getMimeType().equals(other.getMimeType()))
      return false;
    return true;
  }

  /**
   * @see java.lang.Object#toString()
   *
   * @return
   */
  @Override
  public String toString()
  {
    return "ExternalReference [description=" + getDescription() + ", mimeType=" + getMimeType() + ", location="
        + getLocation() + "]";
  }
  
  public String asStringValue()
  {
    // Note: an space string for a non-available value is required, otherwise
    // two consecutive separators would be interpreted as escaped separator
    // character and thus leading to an inconsistent number of values for the
    // external reference, causing an error later. The space prevents that and
    // will also be trimmed later anyways.
    return ((description == null || description.isBlank()) ? " " : description)
        + SingleValue.SEQ_SEPARATOR_STRING
        + ((mimeType == null || mimeType.isBlank()) ? " " : mimeType)
        + SingleValue.SEQ_SEPARATOR_STRING + location;
  }
  
  /**
   * Returns the representation of this ExternalReference as it is required for
   * the http api in ODS: List{@literal <}String> containing description, mimetype,
   * location
   * 
   * @return
   */
  public List<String> asHttpStrings()
  {
    return Arrays.asList(description, mimeType, location);
  }

  /**
   * @return the description
   */
  public java.lang.String getDescription()
  {
    return description;
  }

  /**
   * @param description
   *        the description to set
   */
  public void setDescription(java.lang.String description)
  {
    if (description != null && !description.isBlank())
    {
      this.description = description.trim();
    }
    else
    {
      this.description = description;
    }
  }

  /**
   * @return the mimeType
   */
  public java.lang.String getMimeType()
  {
    return mimeType;
  }

  /**
   * @param mimeType
   *        the mimeType to set
   */
  public void setMimeType(java.lang.String mimeType)
  {
    if (mimeType != null && !mimeType.isBlank())
    {
      this.mimeType = mimeType.trim();
    }
    else
    {
      this.mimeType = mimeType;
    }
  }

  /**
   * @return the location
   */
  public java.lang.String getLocation()
  {
    return location;
  }

  /**
   * @param location
   *        the location to set
   */
  public void setLocation(java.lang.String location)
  {
    if (location != null && !location.isBlank())
    {
      this.location = location.trim();
    }
    else
    {
      this.location = location;
    }
  }
  
  public static ExternalReference getExtRef(String[] values)
  {
    if (values.length < 1)
    {
      return null;
    }
    
    if (values.length < 3)
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Received list of less than 3 values (" + values.length + ") to load external reference from!");
    }
    
    return new ExternalReference(values[0].trim(), values[1].trim(), values[2].trim());
  }
  
  public static ExternalReference getExtRef(List<String> values)
  {
    if (values.isEmpty())
    {
      return null;
    }
    
    if (values.size() < 3)
    {
        throw new OpenAtfxException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, "Received list of less than 3 values (" + values.size() + ") to load external reference from!");
    }
    
    return new ExternalReference(values.get(0), values.get(1), values.get(2));
  }
  
  public static ExternalReference[] getExtRefs(List<String> values)
  {
    List<ExternalReference> extRefs = new ArrayList<>();
    if (!values.isEmpty())
    {
      for (List<Object> extRefParts : splitIntoPortions(3, values))
      {
        extRefs.add(new ExternalReference(extRefParts));
      }
    }
    return extRefs.toArray(new ExternalReference[0]);
  }
  
  private static List<List<Object>> splitIntoPortions(int length, List<?> values)
  {
    int counter = 0;
    List<List<Object>> mainList = new ArrayList<>();
    List<Object> currentSubList = new ArrayList<>();
    for (Object val : values)
    {
      if (values.isEmpty() || counter >= length)
      {
        mainList.add(currentSubList);
        currentSubList = new ArrayList<>();
        currentSubList.add(val);
        counter = 1;
      }
      else
      {
        currentSubList.add(val);
        counter++;
      }
    }
    mainList.add(currentSubList);
    return mainList;
  }
  
  /**
   * @return
   */
  public boolean isFileLocation(File sourceFile)
  {
    if (location == null || location.isBlank())
    {
      return false;
    }
    
    boolean isValidFileLocation = false;
    try
    {
      Path path = Paths.get(location);
      if (path.toFile().exists())
      {
        isValidFileLocation = true;
      }
      else if (sourceFile != null)
      {
        Path sourcePath = Paths.get(sourceFile.getPath()).getParent();
        path = sourcePath.resolve(location);
        if (path.toFile().exists())
        {
          isValidFileLocation = true;
        }
      }
    }
    catch (Exception ex)
    {
      // ignore any exception, just return false
    }
    return isValidFileLocation;
  }
  
  /**
   * @param toleratedExtRefURLPrefixes
   * @return
   */
  public boolean isURL(Collection<String> toleratedExtRefURLPrefixes, File sourceFile)
  {
    if (location == null || location.isBlank() || isFileLocation(sourceFile))
    {
      return false;
    }
    
    for (String toleratedExtRefURLPrefix : toleratedExtRefURLPrefixes)
    {
      if (location.toLowerCase().startsWith(toleratedExtRefURLPrefix.toLowerCase()))
      {
        return true;
      }
    }
    return false;
  }
}
