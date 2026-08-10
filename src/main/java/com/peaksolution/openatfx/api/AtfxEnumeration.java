package com.peaksolution.openatfx.api;
import com.peaksolution.datamodel.DefaultEnumerationDefinition;
import com.peaksolution.datamodel.EnumerationDefinition;
import com.peaksolution.datamodel.ModelException;

import org.asam.ods.ErrorCode;


public class AtfxEnumeration extends DefaultEnumerationDefinition {

    private AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param index The index.
     * @param name The name.
     */
    public AtfxEnumeration(int index, String name) {
        super(index, name);
    }
    
    /**
     * Constructor.
     * 
     * @param index The index.
     * @param name The name.
     * @param atfxCache The AtfxCache.
     */
    public AtfxEnumeration(int index, String name, AtfxCache atfxCache) {
        super(index, name);
        this.atfxCache = atfxCache;
    }

    @Override
    public void addItem(String itemName) {
        // check item name length
        if (itemName == null || itemName.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "itemName must not be empty");
        }
        // check for existing item name
        if (hasItem(itemName)) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + itemName + "' already exists");
        }
        addItem(itemCount(), itemName);
    }

    @Override
    public void setName(String name) {
        // check enum name length
        if (name == null || name.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "name must not be empty");
        }
        // check for name equality
        if (getName().equals(name)) {
            return;
        }
        // check for existing enum name
        EnumerationDefinition existingEnumDef = atfxCache.getEnumeration(name);
        if (existingEnumDef != null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "Cannot set name, since another enumeration with name '" + name + "' already exists!");
        }
        super.setName(name);
    }

    @Override
    public long getItem(String itemName, boolean checkCaseSensitive) {
        try {
            return super.getItem(itemName, checkCaseSensitive);
        } catch (ModelException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + itemName
                    + "' not found for enumeration '" + getName() + "' (checked case sensitive=" + checkCaseSensitive + ")", e);
        }
    }

    @Override
    public String getItemName(long item) {
        try {
            return super.getItemName(item);
        } catch (ModelException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + item + "' not found", e);
        }
    }
    
    @Override
    public void renameItem(String oldItemName, String newItemName) {
        // check new item name length
        if (newItemName == null || newItemName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "newItemName must not be empty");
        }
        // check if old item exists
        if (!hasItem(oldItemName)) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + oldItemName
                    + "' not found");
        }
        super.renameItem(oldItemName, newItemName);
    }

    @Override
    public String toString() {
        return "AtfxEnumeration [name=" + getName() + "]";
    }
}
