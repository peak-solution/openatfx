package com.peaksolution.openatfx.api.corba;

import org.asam.ods.AoException;
import org.asam.ods.EnumerationDefinitionPOA;

import com.peaksolution.openatfx.api.EnumerationDefinition;
import com.peaksolution.openatfx.api.OpenAtfxException;


/**
 * Implementation of <code>org.asam.ods.EnumerationDefinition</code>.
 * 
 * @author Christian Rechner, Markus Renner
 */
class EnumerationDefinitionImpl extends EnumerationDefinitionPOA {

    private final EnumerationDefinition delegate;

    /**
     * Constructor.
     * 
     * @param newEnum The OpenAtfx enumeration delegate.
     */
    public EnumerationDefinitionImpl(EnumerationDefinition newEnum) {
        this.delegate = newEnum;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getIndex()
     */
    public int getIndex() throws AoException {
        try {
            return delegate.getIndex();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getName()
     */
    public String getName() throws AoException {
        try {
            return delegate.getName();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#setName(java.lang.String)
     */
    public void setName(String name) throws AoException {
        try {
            delegate.setName(name);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#listItemNames()
     */
    public String[] listItemNames() throws AoException {
        try {
            return delegate.listItemNames();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getItem(java.lang.String)
     */
    public int getItem(String itemName) throws AoException {
        try {
            long item = delegate.getItem(itemName);
            return Math.toIntExact(item);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#getItemName(int)
     */
    public String getItemName(int item) throws AoException {
        try {
            return delegate.getItemName(item);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#addItem(java.lang.String)
     */
    public void addItem(String itemName) throws AoException {
        try {
            delegate.addItem(itemName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.EnumerationDefinitionOperations#renameItem(java.lang.String, java.lang.String)
     */
    public void renameItem(String oldItemName, String newItemName) throws AoException {
        try {
            delegate.renameItem(oldItemName, newItemName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

}
