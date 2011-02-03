package de.rechner.openatfx.basestructure;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.asam.ods.AoException;
import org.asam.ods.EnumerationDefinitionPOA;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;


/**
 * Implementation of <code>org.asam.ods.EnumerationDefinition</code>.
 * 
 * @author Christian Rechner
 */
class BaseEnumerationDefinitionImpl extends EnumerationDefinitionPOA {

    private final int index;
    private final String name;
    private final SortedMap<Integer, String> itemToNameMap;
    private final Map<String, Integer> nameToItemMap;

    /**
     * Constructor.
     * 
     * @param index The index.
     * @param name The name.
     */
    public BaseEnumerationDefinitionImpl(int index, String name) {
        this.index = index;
        this.name = name;
        this.itemToNameMap = new TreeMap<Integer, String>();
        this.nameToItemMap = new HashMap<String, Integer>();
    }

    /**
     * Adds an enumeration item.
     * 
     * @param item The item index.
     * @param name The item name.
     */
    public void addItem(int item, String name) {
        this.itemToNameMap.put(item, name);
        this.nameToItemMap.put(name, item);
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
        throw new AoException(ErrorCode.AO_ACCESS_DENIED, SeverityFlag.ERROR, 0,
                              "Changing base enumerations is not allowed");
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
                    + "' not found for enumeration '" + this.name + "'");
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
        throw new AoException(ErrorCode.AO_ACCESS_DENIED, SeverityFlag.ERROR, 0,
                              "Changing base enumerations is not allowed");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#renameItem(java.lang.String, java.lang.String)
     */
    public void renameItem(String oldItemName, String newItemName) throws AoException {
        throw new AoException(ErrorCode.AO_ACCESS_DENIED, SeverityFlag.ERROR, 0,
                              "Changing base enumerations is not allowed");
    }

}
