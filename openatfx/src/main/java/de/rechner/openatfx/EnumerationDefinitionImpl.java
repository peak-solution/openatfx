package de.rechner.openatfx;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.EnumerationDefinitionPOA;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;


/**
 * Implementation of <code>org.asam.ods.EnumerationDefinition</code>.
 * 
 * @author Christian Rechner
 */
class EnumerationDefinitionImpl extends EnumerationDefinitionPOA {

    private final ApplicationStructure applicationStructure;
    private final int index;
    private final SortedMap<Integer, String> itemToNameMap;
    private final Map<String, Integer> nameToItemMap;

    private String name;

    /**
     * Constructor.
     * 
     * @param applicationStructure The application structure.
     * @param index The index.
     * @param name The name.
     */
    public EnumerationDefinitionImpl(ApplicationStructure applicationStructure, int index, String name) {
        this.applicationStructure = applicationStructure;
        this.index = index;
        this.name = name;
        this.itemToNameMap = new TreeMap<Integer, String>();
        this.nameToItemMap = new HashMap<String, Integer>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getIndex()
     */
    public int getIndex() throws AoException {
        return this.index;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getName()
     */
    public String getName() throws AoException {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#setName(java.lang.String)
     */
    public void setName(String name) throws AoException {
        // check enum name length
        if (name == null || name.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "name must not be empty");
        }
        // check for name equality
        if (this.name.equals(name)) {
            return;
        }
        // check for existing enum name
        for (String existingEnumDefName : this.applicationStructure.listEnumerations()) {
            if (existingEnumDefName.equals(name)) {
                throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
                                      "EnumerationDefinition with name '" + name + "' already exists");
            }
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#listItemNames()
     */
    public String[] listItemNames() throws AoException {
        return this.itemToNameMap.values().toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getItem(java.lang.String)
     */
    public int getItem(String itemName) throws AoException {
        Integer item = this.nameToItemMap.get(itemName);
        if (item == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Enumeration item '" + itemName
                    + "' not found");
        }
        return item;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getItemName(int)
     */
    public String getItemName(int item) throws AoException {
        String itemName = this.itemToNameMap.get(item);
        if (itemName == null) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Enumeration item '" + item
                    + "' not found");
        }
        return itemName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#addItem(java.lang.String)
     */
    public void addItem(String itemName) throws AoException {
        // check item name length
        if (itemName == null || itemName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "itemName must not be empty");
        }
        // check for existing item name
        if (this.nameToItemMap.containsKey(itemName)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Enumeration item '" + itemName
                    + "' already exists");
        }
        int item = this.itemToNameMap.size();
        this.itemToNameMap.put(item, itemName);
        this.nameToItemMap.put(itemName, item);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#renameItem(java.lang.String, java.lang.String)
     */
    public void renameItem(String oldItemName, String newItemName) throws AoException {
        // check new item name length
        if (newItemName == null || newItemName.length() < 1) {
            throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "newItemName must not be empty");
        }
        // check if old item exists
        if (!this.nameToItemMap.containsKey(oldItemName)) {
            throw new AoException(ErrorCode.AO_NOT_FOUND, SeverityFlag.ERROR, 0, "Enumeration item '" + oldItemName
                    + "' not found");
        }
        // rename
        int item = this.nameToItemMap.get(oldItemName);
        this.nameToItemMap.remove(oldItemName);
        this.itemToNameMap.put(item, newItemName);
        this.nameToItemMap.put(newItemName, item);
    }

}
