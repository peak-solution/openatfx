package de.rechner.openatfx.io;

/**
 * Abstract class holding the constants for the XML element and attribute names in an ATFX file.
 * 
 * @author Christian Rechner
 */
class AtfxTagConstants {

    public static final String VERSION = "0.7.1";

    public static final String ATFX_FILE = "atfx_file";

    public static final String DOCUMENTATION = "documentation";
    public static final String EXPORTED_BY = "exported_by";
    public static final String EXPORTER = "exporter";
    public static final String EXPORT_DATETIME = "export_date_time";
    public static final String EXPORTER_VERSION = "exporter_version";
    public static final String SHORT_DESCRIPTION = "short_description";
    public static final String LONG_DESCRIPTION = "long description";
    public static final String ESCAPE_SPECIAL_CHARS = "escape_special_chars";

    public static final String BASE_MODEL_VERSION = "base_model_version";

    public static final String FILES = "files";
    public static final String COMPONENT = "component";
    public static final String COMPONENT_IDENTIFIER = "identifier";
    public static final String COMPONENT_FILENAME = "filename";
    public static final String COMPONENT_DATATYPE = "datatype";
    public static final String COMPONENT_LENGTH = "length";
    public static final String COMPONENT_DESCRIPTION = "description";
    public static final String COMPONENT_INIOFFSET = "inioffset";
    public static final String COMPONENT_BLOCKSIZE = "blocksize";
    public static final String COMPONENT_VALPERBLOCK = "valperblock";
    public static final String COMPONENT_VALOFFSETS = "valoffsets";
    public static final String COMPONENT_BITCOUNT = "bitcount";
    public static final String COMPONENT_BITOFFSET = "bitoffset";

    public static final String APPL_MODEL = "application_model";

    public static final String APPL_ENUM = "application_enumeration";
    public static final String APPL_ENUM_NAME = "name";
    public static final String APPL_ENUM_ITEM = "item";
    public static final String APPL_ENUM_ITEM_NAME = "name";
    public static final String APPL_ENUM_ITEM_VALUE = "value";

    public static final String APPL_ELEM = "application_element";
    public static final String APPL_ELEM_NAME = "name";
    public static final String APPL_ELEM_BASETYPE = "basetype";
    public static final String APPL_ELEM_INHERITSFROM = "inherits_from";

    public static final String APPL_ATTR = "application_attribute";
    public static final String APPL_ATTR_NAME = "name";
    public static final String APPL_ATTR_BASEATTR = "base_attribute";
    public static final String APPL_ATTR_DATATYPE = "datatype";
    public static final String APPL_ATTR_LENGTH = "length";
    public static final String APPL_ATTR_OBLIGATORY = "obligatory";
    public static final String APPL_ATTR_UNIQUE = "unique";
    public static final String APPL_ATTR_AUTOGENERATE = "autogenerate";
    public static final String APPL_ATTR_ENUMTYPE = "enumeration_type";
    public static final String APPL_ATTR_UNIT = "unit";

    public static final String APPL_REL = "relation_attribute";
    public static final String APPL_REL_REFTO = "ref_to";
    public static final String APPL_REL_NAME = "name";
    public static final String APPL_REL_INVNAME = "inverse_name";
    public static final String APPL_REL_BASEREL = "base_relation";
    public static final String APPL_REL_MIN = "min_occurs";
    public static final String APPL_REL_MAX = "max_occurs";

    public static final String INSTANCE_DATA = "instance_data";

    public static final String INST_ATTR = "instance_attributes";
    public static final String INST_ATTR_NAME = "name";
    public static final String INST_ATTR_ASCIISTRING = "inst_attr_asciistring";
    public static final String INST_ATTR_FLOAT32 = "inst_attr_float32";
    public static final String INST_ATTR_FLOAT64 = "inst_attr_float64";
    public static final String INST_ATTR_INT8 = "inst_attr_int8";
    public static final String INST_ATTR_INT16 = "inst_attr_int16";
    public static final String INST_ATTR_INT32 = "inst_attr_int32";
    public static final String INST_ATTR_INT64 = "inst_attr_int64";
    public static final String INST_ATTR_TIME = "inst_attr_time";

    public static final String VALUES_ATTR_UTF8STRING = "A_UTF8STRING";
    public static final String VALUES_ATTR_INT8 = "A_INT8";
    public static final String VALUES_ATTR_INT16 = "A_INT16";
    public static final String VALUES_ATTR_INT32 = "A_INT32";
    public static final String VALUES_ATTR_INT64 = "A_INT64";
    public static final String VALUES_ATTR_FLOAT32 = "A_FLOAT32";
    public static final String VALUES_ATTR_FLOAT64 = "A_FLOAT64";
    public static final String VALUES_ATTR_BOOLEAN = "A_BOOLEAN";
    public static final String VALUES_ATTR_COMPLEX32 = "A_COMPLEX32";
    public static final String VALUES_ATTR_COMPLEX64 = "A_COMPLEX64";
    public static final String VALUES_ATTR_TIMESTRING = "A_TIMESTRING";
    public static final String VALUES_ATTR_BYTEFIELD = "A_BYTEFIELD";
    public static final String VALUES_ATTR_BLOB = "A_BLOB";
    public static final String VALUES_ATTR_EXTERNALREFERENCE = "A_EXTERNALREFERENCE";

    public static final String EXTREF = "external_reference";
    public static final String EXTREF_DESCRIPTION = "description";
    public static final String EXTREF_MIMETYPE = "mimetype";
    public static final String EXTREF_LOCATION = "location";

    public static final String BYTESTR_LENGTH = "length";
    public static final String BYTESTR_SEQUENCE = "sequence";
    
    public static final String BLOB_TEXT = "text";
    public static final String BLOB_BYTEFIELD = "bytefield";
    public static final String BLOB_LENGTH = "length";
    public static final String BLOB_SEQUENCE = "sequence";

    public static final String STRING_SEQ = "s";

    public static final String SECURITY_ACLA = "ACLA";
    public static final String SECURITY_ACLI = "ACLI";
    public static final String SECURITY_ACL_AENAME = "appl_element_name";
    public static final String SECURITY_ACL_ATTRNAME = "attribute_name";
    public static final String SECURITY_ACL_RIGHTS = "rights";

}
