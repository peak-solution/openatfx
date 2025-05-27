package com.peaksolution.openatfx.api;

public interface EnumerationDefinition {
    /**
     * Adds an enumeration item.
     * 
     * @param item The item index.
     * @param name The item name.
     */
    public void addItem(long item, String name);
    
    public void addItem(String itemName);

    public int getIndex();

    public String getName();

    public void setName(String name);

    public String[] listItemNames();

    public long getItem(String itemName);
    
    public long getItem(String itemName, boolean checkCaseSensitive);

    public String getItemName(long item);

    void renameItem(String oldItemName, String newItemName);
}
