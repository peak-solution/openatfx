package com.peaksolution.openatfx.api;

import java.util.Collection;


public interface BaseModel {

    /**
     * Returns this base model's ASAM ODS version
     * 
     * @return the base model version string.
     */
    String getVersion();

    Collection<EnumerationDefinition> getEnumerations();

    EnumerationDefinition getEnumDef(String enumName);

    BaseElement[] getElements(String pattern);

    BaseElement getElementByType(String type);
}
