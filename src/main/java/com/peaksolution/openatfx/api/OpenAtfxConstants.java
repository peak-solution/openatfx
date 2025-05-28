package com.peaksolution.openatfx.api;


public class OpenAtfxConstants {
    // ODS
    public static final String ASAM = "asam";
    public static final int DEF_MODEL_VERSION = 36; // use latest version as default
    public static final String DEF_RELNAME_EMPTY = "AUTOGEN";
    
    public static final String REL_MAX_MANY = "Many";
    public static final String CONTEXT_WILDCARD_ALL = "WILDCARD_ALL";
    public static final String DEF_WILDCARD_ALL = "*";
    public static final String CONTEXT_WILDCARD_ESC = "WILDCARD_ESC";
    public static final String DEF_WILDCARD_ESC = "\\";
    public static final String CONTEXT_WILDCARD_ONE = "WILDCARD_ONE";
    public static final String DEF_WILDCARD_ONE = ".";
    public static final String CONTEXT_USER = "USER";
    public static final String CONTEXT_PASSWORD = "PASSWORD";
    public static final String CONTEXT_ODS_API_VERSION = "ODSVERSION";
    public static final String DEF_ODS_API_VERSION = "5.3.1";
    public static final String CONTEXT_CREATE_COSESSION_ALLOWED = "CREATE_COSESSION_ALLOWED";
    public static final String CONTEXT_FILE_NOTATION = "FILE_NOTATION";
    public static final String DEF_FILE_NOTATION = "UNC_UNIX";
    public static final String CONTEXT_FILE_MODE = "FILE_MODE";
    public static final String DEF_FILE_MODE = "SINGLE_VOLUME";
    public static final String CONTEXT_FILE_ROOT = "FILE_ROOT";
    public static final String CONTEXT_FILE_ROOT_EXTREF = "FILE_ROOT_EXTREF";
    public static final String CONTEXT_VALUEMATRIX_MODE = "VALUEMATRIX_MODE";
    public static final String DEF_VALUEMATRIX_MODE = "CALCULATED";
    public static final String CONTEXT_FILENAME = "FILENAME";
    public static final String CONTEXT_TYPE = "TYPE";
    public static final String DEF_TYPE = "XATF-ASCII";
    public static final String CONTEXT_WRITE_MODE = "write_mode";
    public static final String DEF_WRITE_MODE = "database";
    
    public static final String BE_ANY = "AoAny";
    public static final String BE_SUBMATRIX = "AoSubMatrix";
    public static final String BE_UNIT = "AoUnit";
    
    public static final String BA_ID = "id";
    
    // OpenAtfx
    public static final String CONTEXT_OPENATFX_API_VERSION = "1.0.0";
    public static final String CONTEXT_EXTENDED_COMPATIBILITYMODE = "EXTENDED_COMPATIBILITYMODE";
    public static final String CONTEXT_EXTCOMP_FILENAME_STRIP_STRING = "ETXCOMP_FILENAME_STRIP_STRING";
    public static final String CONTEXT_EXT_COMP_SEGSIZE = "EXT_COMP_SEGSIZE";
    public static final long DEF_EXT_COMP_SEGSIZE = 1024L * 1024 * 500; // 500MB
    public static final String CONTEXT_INDENT_XML = "INDENT_XML";
    public static final String CONTEXT_WRITE_EXTERNALCOMPONENTS = "WRITE_EXTERNALCOMPONENTS";
    public static final String CONTEXT_TRIM_STRING_VALUES = "TRIM_STRING_VALUES";
    
}
