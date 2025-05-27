package com.peaksolution.openatfx.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.asam.ods.ErrorCode;


public class AtfxEnumeration implements EnumerationDefinition {

    private final int index;
    private String name;
    private final SortedMap<Long, String> itemToNameMap;
    private final Map<String, Long> nameToItemMap;
    
    private AtfxCache atfxCache;

    /**
     * Constructor.
     * 
     * @param index The index.
     * @param name The name.
     */
    public AtfxEnumeration(int index, String name) {
        this.index = index;
        this.name = name;
        this.itemToNameMap = new TreeMap<>();
        this.nameToItemMap = new HashMap<>();
    }
    
    /**
     * Constructor.
     * 
     * @param index The index.
     * @param name The name.
     * @param atfxCache The AtfxCache.
     */
    public AtfxEnumeration(int index, String name, AtfxCache atfxCache) {
        this.index = index;
        this.name = name;
        this.atfxCache = atfxCache;
        this.itemToNameMap = new TreeMap<>();
        this.nameToItemMap = new HashMap<>();
    }

    @Override
    public void addItem(long item, String name) {
        this.itemToNameMap.put(item, name);
        this.nameToItemMap.put(name, item);
    }
    
    @Override
    public void addItem(String itemName) {
        // check item name length
        if (itemName == null || itemName.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "itemName must not be empty");
        }
        // check for existing item name
        if (nameToItemMap.get(itemName) != null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + itemName + "' already exists");
        }
        int idx = nameToItemMap.size();
        addItem(idx, itemName);
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        // check enum name length
        if (name == null || name.isBlank()) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "name must not be empty");
        }
        // check for name equality
        if (this.name.equals(name)) {
            return;
        }
        // check for existing enum name
        EnumerationDefinition existingEnumDef = atfxCache.getEnumeration(name);
        if (existingEnumDef != null) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                        "Cannot set name, since another enumeration with name '" + name + "' already exists!");
        }
        this.name = name;
    }

    @Override
    public String[] listItemNames() {
        return this.itemToNameMap.values().toArray(new String[0]);
    }

    @Override
    public long getItem(String itemName) {
        return getItem(itemName, true);
    }
    
    @Override
    public long getItem(String itemName, boolean checkCaseSensitive) {
        Long item = null;
        if (checkCaseSensitive) {
            item = this.nameToItemMap.get(itemName);
        } else {
            for (Entry<String, Long> entry : nameToItemMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(itemName)) {
                    item = entry.getValue();
                    break;
                }
            }
        }
        
        if (item == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + itemName
                    + "' not found for enumeration '" + this.name + "' (checked case sensitive=" + checkCaseSensitive + ")");
        }
        return item;
    }

    @Override
    public String getItemName(long item) {
        String itemName = this.itemToNameMap.get(item);
        if (itemName == null) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + item + "' not found");
        }
        return itemName;
    }
    
    @Override
    public void renameItem(String oldItemName, String newItemName) {
        // check new item name length
        if (newItemName == null || newItemName.length() < 1) {
            throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER, "newItemName must not be empty");
        }
        // check if old item exists
        if (!this.nameToItemMap.containsKey(oldItemName)) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, "Enumeration item '" + oldItemName
                    + "' not found");
        }
        // rename
        long item = this.nameToItemMap.get(oldItemName);
        this.nameToItemMap.remove(oldItemName);
        this.itemToNameMap.put(item, newItemName);
        this.nameToItemMap.put(newItemName, item);
    }

    @Override
    public String toString() {
        return "AtfxEnumeration [name=" + name + "]";
    }
}
