package de.rechner.openatfx.basestructure;

import org.asam.ods.AoException;
import org.asam.ods.BaseAttributePOA;
import org.asam.ods.BaseElement;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;


/**
 * Implementation of <code>org.asam.ods.BaseAttribute</code>.
 * 
 * @author Christian Rechner
 */
class BaseAttributeImpl extends BaseAttributePOA {

    private final String name;
    private final DataType dataType;
    private final boolean obligatory;
    private final boolean unique;
    private final BaseElement baseElement;
    private final EnumerationDefinition enumerationDefinition;

    /**
     * Constructor.
     * 
     * @param name The name.
     * @param dataType The data type.
     * @param obligatory The obligatory flag.
     * @param unique The unique flag.
     * @param baseElementImpl The base element.
     * @param enumerationDefinition The enumeration definition.
     */
    public BaseAttributeImpl(String name, DataType dataType, boolean obligatory, boolean unique,
            BaseElement baseElement, EnumerationDefinition enumerationDefinition) {
        this.name = name;
        this.dataType = dataType;
        this.obligatory = obligatory;
        this.unique = unique;
        this.baseElement = baseElement;
        this.enumerationDefinition = enumerationDefinition;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#getName()
     */
    public String getName() throws AoException {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#getDataType()
     */
    public DataType getDataType() throws AoException {
        return this.dataType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#isObligatory()
     */
    public boolean isObligatory() throws AoException {
        return this.obligatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#isUnique()
     */
    public boolean isUnique() throws AoException {
        return this.unique;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#getBaseElement()
     */
    public BaseElement getBaseElement() throws AoException {
        return this.baseElement;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.BaseAttributeOperations#getEnumerationDefinition()
     */
    public EnumerationDefinition getEnumerationDefinition() throws AoException {
        if (this.enumerationDefinition == null) {
            throw new AoException(ErrorCode.AO_INVALID_DATATYPE, SeverityFlag.ERROR, 0, "Invalid datatype");
        }
        return this.enumerationDefinition;
    }

}
