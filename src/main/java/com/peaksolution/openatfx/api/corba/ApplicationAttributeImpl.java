package com.peaksolution.openatfx.api.corba;

import org.asam.ods.ACL;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationAttributePOA;
import org.asam.ods.ApplicationElement;
import org.asam.ods.BaseAttribute;
import org.asam.ods.DataType;
import org.asam.ods.EnumerationDefinition;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.RightsSet;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_LONGLONG;

import com.peaksolution.openatfx.api.AtfxAttribute;
import com.peaksolution.openatfx.api.OpenAtfxException;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Implementation of <code>org.asam.ods.ApplicationAttribute</code>.
 * 
 * @author Markus Renner
 */
class ApplicationAttributeImpl extends ApplicationAttributePOA {

//    /**
//     * this map contains the names of all base attributes that are defined as 'non optional' in the ods specification
//     * and their respective base element names. this workaround is necessary, because while the ods spec has two
//     * distinct flags 'optional' and 'mandatory' for each attribute, the ods interface does not. thus this
//     * implementation returns 'true' for isObligatory() whenever one of the attribute names below is encountered.
//     * additionally, it does not permit the obligatory flag to be set to 'false' for those attributes.
//     */
//    private static final Map<String, List<String>> obligatoryAttributes;
//    static {
//        obligatoryAttributes = new HashMap<>();
//        obligatoryAttributes.put("id", null); // all application elements
//        obligatoryAttributes.put("name", null); // all application elements
//        obligatoryAttributes.put("entity_name", Arrays.asList("AoNameMap", "AoAttributeMap"));
//        obligatoryAttributes.put("factor", Arrays.asList("AoUnit"));
//        obligatoryAttributes.put("offset", Arrays.asList("AoUnit"));
//        obligatoryAttributes.put("length_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("mass_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("time_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("current_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("temperature_exp", Arrays.asList("AoPhysicalDimension" ));
//        obligatoryAttributes.put("molar_amount_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("luminous_intensity_exp", Arrays.asList("AoPhysicalDimension"));
//        obligatoryAttributes.put("datatype", Arrays.asList("AoMeasurementQuantity"));
//        obligatoryAttributes.put("number_of_rows", Arrays.asList("AoSubmatrix"));
//        obligatoryAttributes.put("independent", Arrays.asList("AoLocalColumn"));
//        obligatoryAttributes.put("component_length", Arrays.asList("AoExternalComponent"));
//        obligatoryAttributes.put("value_type", Arrays.asList("AoExternalComponent"));
//        obligatoryAttributes.put("password", Arrays.asList("AoUser"));
//        obligatoryAttributes.put("superuser_flag", Arrays.asList("AoUserGroup"));
//        obligatoryAttributes.put("date", Arrays.asList("AoLog"));
//        obligatoryAttributes.put("parameter_datatype", Arrays.asList("AoParameter"));
//        obligatoryAttributes.put("pvalue", Arrays.asList("AoParameter"));
//    }

    private final CorbaAtfxCache corbaCache;
    private final AtfxAttribute delegate;

    private boolean valueFlag;

    /**
     * Constructor.
     * 
     * @param corbaCache The atfx cache.
     * @param atfxAttribute The delegate attribute.
     */
    public ApplicationAttributeImpl(CorbaAtfxCache corbaCache, AtfxAttribute atfxAttribute) {
        this.corbaCache = corbaCache;
        this.delegate = atfxAttribute;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getApplicationElement()
     */
    public ApplicationElement getApplicationElement() throws AoException {
        try {
            return corbaCache.getApplicationElementById(delegate.getAid());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getName()
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
     * @see org.asam.ods.ApplicationAttributeOperations#setName(java.lang.String)
     */
    public void setName(String aaName) throws AoException {
        try {
            corbaCache.renameApplicationAttribute(delegate.getAid(), delegate.getName(), aaName);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getBaseAttribute()
     */
    public BaseAttribute getBaseAttribute() throws AoException {
        return corbaCache.getBaseAttribute(delegate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setBaseAttribute(org.asam.ods.BaseAttribute)
     */
    public void setBaseAttribute(BaseAttribute baseAttr) throws AoException {
        try {
            corbaCache.updateBaseAttribute(delegate.getAid(), getName(), baseAttr.getName());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getDataType()
     */
    public DataType getDataType() throws AoException {
        try {
            return ODSHelper.string2dataType(delegate.getDataType().name());
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setDataType(org.asam.ods.DataType)
     */
    public void setDataType(DataType aaDataType) throws AoException {
        try {
            corbaCache.updateApplicationAttributeDataType(delegate, aaDataType);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getLength()
     */
    public int getLength() throws AoException {
        try {
            return delegate.getLength();
        }  catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setLength(int)
     */
    public void setLength(int aaLength) throws AoException {
        try {
            delegate.setLength(aaLength);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#isUnique()
     */
    public boolean isUnique() throws AoException {
        try {
            return delegate.isUnique();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setIsUnique(boolean)
     */
    public void setIsUnique(boolean aaIsUnique) throws AoException {
        try {
            delegate.setUnique(aaIsUnique);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#isObligatory()
     */
    public boolean isObligatory() throws AoException {
        try {
            return delegate.isObligatory();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setIsObligatory(boolean)
     */
    public void setIsObligatory(boolean aaIsObligatory) throws AoException {
        try {
            delegate.setObligatory(aaIsObligatory);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#isAutogenerated()
     */
    public boolean isAutogenerated() throws AoException {
        try {
            return delegate.isAutogenerated();
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setIsAutogenerated(boolean)
     */
    public void setIsAutogenerated(boolean isAutogenerated) throws AoException {
        try {
            delegate.setAutogenerated(isAutogenerated);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getEnumerationDefinition()
     */
    public EnumerationDefinition getEnumerationDefinition() throws AoException {
        String enumName = null;
        try {
            enumName = delegate.getEnumName();
            if (enumName == null) {
                throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
                                      "Did not find any specified enumeration for attribute '" + getName() + "' at element "
                                              + getApplicationElement().getName() + "'!");
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
        return corbaCache.getEnumerationDefinition(enumName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setEnumerationDefinition(org.asam.ods.EnumerationDefinition)
     */
    public void setEnumerationDefinition(EnumerationDefinition enumDef) throws AoException {
        try {
            if (enumDef != null) {
                delegate.setEnumName(enumDef.getName());
            }
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#hasUnit()
     */
    public boolean hasUnit() throws AoException {
        try {
            return delegate.getUnitId() > 0;
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#withUnit(boolean)
     */
    public void withUnit(boolean withUnit) throws AoException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getUnit()
     */
    public T_LONGLONG getUnit() throws AoException {
        try {
            long unitId = delegate.getUnitId();
            return ODSHelper.asODSLongLong(unitId);
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setUnit(org.asam.ods.T_LONGLONG)
     */
    public void setUnit(T_LONGLONG aaUnit) throws AoException {
        try {
            delegate.setUnitId(ODSHelper.asJLong(aaUnit));
        } catch (OpenAtfxException oae) {
            throw oae.toAoException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#setRights(org.asam.ods.InstanceElement, int,
     *      org.asam.ods.RightsSet)
     */
    public void setRights(InstanceElement usergroup, int rights, RightsSet set) throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'setRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#getRights()
     */
    public ACL[] getRights() throws AoException {
        throw new AoException(ErrorCode.AO_NOT_IMPLEMENTED, SeverityFlag.ERROR, 0, "Method 'getRights' not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#hasValueFlag()
     */
    @Deprecated
    public boolean hasValueFlag() throws AoException {
        return this.valueFlag;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.asam.ods.ApplicationAttributeOperations#withValueFlag(boolean)
     */
    @Deprecated
    public void withValueFlag(boolean withValueFlag) throws AoException {
        this.valueFlag = withValueFlag;
    }

}
