package com.peaksolution.openatfx.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.asam.ods.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.IFileHandler;
import com.peaksolution.openatfx.LocalFileHandler;


/**
 * Factory class to create {@link OpenAtfxAPIImplementation}s for new or existing atfx file.
 * 
 * @author Markus Renner
 */
public class ApiFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ApiFactory.class);

    /** The static context variables, these may not be changed */
    static final Map<String, NameValueUnit> STATIC_CONTEXT = new LinkedHashMap<>();
    static {
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_WILDCARD_ALL,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_WILDCARD_ALL, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_WILDCARD_ALL));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_WILDCARD_ESC,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_WILDCARD_ESC, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_WILDCARD_ESC));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_WILDCARD_ONE,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_WILDCARD_ONE, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_WILDCARD_ONE));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_USER,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_USER, DataType.DT_STRING,
                                             System.getProperty("user.name")));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_PASSWORD,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_PASSWORD, DataType.DT_STRING, "***********"));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_ODS_API_VERSION,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_ODS_API_VERSION, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_ODS_API_VERSION));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_CREATE_COSESSION_ALLOWED,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_CREATE_COSESSION_ALLOWED, DataType.DT_STRING,
                                             "FALSE"));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_FILE_NOTATION,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_NOTATION, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_FILE_NOTATION));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_FILE_MODE,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_MODE, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_FILE_MODE));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_FILE_ROOT,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT, DataType.DT_STRING, ""));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_FILE_ROOT_EXTREF,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT_EXTREF, DataType.DT_STRING, ""));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_FILENAME,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_FILENAME, DataType.DT_STRING, ""));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_VALUEMATRIX_MODE,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_VALUEMATRIX_MODE, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_VALUEMATRIX_MODE));
        STATIC_CONTEXT.put(OpenAtfxConstants.CONTEXT_TYPE,
                           new NameValueUnit(OpenAtfxConstants.CONTEXT_TYPE, DataType.DT_STRING,
                                             OpenAtfxConstants.DEF_TYPE));
    }

    /**
     * Reads an existing atfx file and provides OpenAtfxAPIImplementation to access it.
     * 
     * @param fileHandler the IFileHandler implementation for the atfx file
     * @param path The atfx file path.
     * @param properties The OpenAtfx properties to set.
     * @return
     */
    public OpenAtfxAPIImplementation getApiForExistingFile(IFileHandler fileHandler, Path path, Properties properties) {
        try (InputStream in = fileHandler.getFileStream(path)) {
            String fileRoot = fileHandler.getFileRoot(path);
            String fileName = fileHandler.getFileName(path);
            String compatibilityModeString = properties.getProperty(OpenAtfxConstants.CONTEXT_EXTENDED_COMPATIBILITYMODE);
            boolean isExtendedCompatiblityMode = Boolean.parseBoolean(compatibilityModeString);
            String configuredExtCompFilenameStartRemoveString = properties.getProperty(OpenAtfxConstants.CONTEXT_EXTCOMP_FILENAME_STRIP_STRING);

            // open XML file
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            XMLStreamReader rawReader = inputFactory.createXMLStreamReader(in);
            XMLStreamReader reader = inputFactory.createFilteredReader(rawReader, new StartEndElementFilter());

            AtfxReader atfxReader = new AtfxReader(fileHandler, path, isExtendedCompatiblityMode,
                                                   configuredExtCompFilenameStartRemoveString);
            Collection<NameValueUnit> context = prepareContext(properties, fileRoot, fileName);
            return atfxReader.readFile(reader, context);
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_CONNECT_FAILED,
                                        "Error reading data from atfx file: " + e.getMessage());
        } catch (XMLStreamException e) {
            throw new OpenAtfxException(ErrorCode.AO_CONNECT_FAILED,
                                        "Error creating file reader for atfx file: " + e.getMessage());
        }
    }

    /**
     * Creates the new atfx file with given path, initializes ODS base model with given version and provides the
     * connected {@link OpenAtfxAPIImplementation};
     * 
     * @param path The atfx file path.
     * @param properties The OpenAtfx properties to set.
     * @param baseModelVersionNr The number of the ODS base model to use, e.g. 36.
     * @return
     */
    public OpenAtfxAPIImplementation getApiForNewFile(Path path, Properties properties, int baseModelVersionNr) {
        String modelVersion = OpenAtfxConstants.ASAM + OpenAtfxConstants.DEF_MODEL_VERSION;
        if (baseModelVersionNr >= 29 && baseModelVersionNr <= OpenAtfxConstants.DEF_MODEL_VERSION) {
            modelVersion = OpenAtfxConstants.ASAM + baseModelVersionNr;
        } else {
            LOG.info("Received unsupported base model version number '{}', falling back to default version {}",
                     baseModelVersionNr, modelVersion);
        }

        long start = System.currentTimeMillis();
        IFileHandler fileHandler = new LocalFileHandler();
        AtfxBaseModel baseModel = BaseModelFactory.getInstance().getBaseModel(modelVersion);
        OpenAtfxAPIImplementation api = new OpenAtfxAPIImplementation(baseModel);
        try {
            Collection<NameValueUnit> context = prepareContext(properties, fileHandler.getFileRoot(path), fileHandler.getFileName(path));
            api.init(context);
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_CONNECT_FAILED,
                                        "Error initializing new atfx file: " + e.getMessage());
        }
        
        LOG.info("Initiated new ATFX in {}ms", System.currentTimeMillis() - start);
        return api;
    }

    private Collection<NameValueUnit> prepareContext(Properties properties, String fileRoot, String fileName) {
        Map<String, NameValueUnit> contextByName = new HashMap<>();
        
        // static context
        for (Entry<String, NameValueUnit> entry : STATIC_CONTEXT.entrySet()) {
//            api.initContext(entry.getValue());
            contextByName.put(entry.getKey(), entry.getValue());
        }
//        api.initContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT, DataType.DT_STRING, fileRoot));
//        api.initContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT_EXTREF, DataType.DT_STRING, fileRoot));
//        api.initContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_FILENAME, DataType.DT_STRING, fileName));
        contextByName.put(OpenAtfxConstants.CONTEXT_FILE_ROOT, new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT, DataType.DT_STRING, fileRoot));
        contextByName.put(OpenAtfxConstants.CONTEXT_FILE_ROOT_EXTREF, new NameValueUnit(OpenAtfxConstants.CONTEXT_FILE_ROOT_EXTREF, DataType.DT_STRING, fileRoot));
        contextByName.put(OpenAtfxConstants.CONTEXT_FILENAME, new NameValueUnit(OpenAtfxConstants.CONTEXT_FILENAME, DataType.DT_STRING, fileName));

        // context from properties
        for (Entry<Object, Object> entry : properties.entrySet()) {
            Object valueObject = entry.getValue();
            SingleValue value = new SingleValue();
            if (valueObject instanceof String) {
                value.setDiscriminator(DataType.DT_STRING);
                value.setValue(valueObject);
            } else if (valueObject instanceof Integer) {
                value.setDiscriminator(DataType.DT_LONG);
                value.setValue((int) valueObject);
            } else if (valueObject instanceof Long) {
                value.setDiscriminator(DataType.DT_LONGLONG);
                value.setValue((long) valueObject);
            } else if (valueObject instanceof Boolean) {
                value.setDiscriminator(DataType.DT_BOOLEAN);
                value.setValue((boolean) valueObject);
            } else {
                throw new OpenAtfxException(ErrorCode.AO_BAD_PARAMETER,
                                            OpenAtfxException.ERR_UNSUPPORTED_CONTEXT_DATATYPE + entry.getKey() + " ("
                                                    + valueObject.getClass().getSimpleName() + ")");
            }
//            api.setContext(new NameValueUnit(entry.getKey().toString(), value));
            contextByName.put(entry.getKey().toString(), new NameValueUnit(entry.getKey().toString(), value));
        }

        // fixed context
//        api.setContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_WRITE_MODE, DataType.DT_STRING,
//                                         OpenAtfxConstants.DEF_WRITE_MODE));
//        api.setContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_EXT_COMP_SEGSIZE, DataType.DT_LONGLONG,
//                                         OpenAtfxConstants.DEF_EXT_COMP_SEGSIZE));
//        api.setContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_INDENT_XML, DataType.DT_STRING, "TRUE"));
//        api.setContext(new NameValueUnit(OpenAtfxConstants.CONTEXT_WRITE_EXTERNALCOMPONENTS, DataType.DT_STRING,
//                                         "FALSE"));
        contextByName.put(OpenAtfxConstants.CONTEXT_WRITE_MODE,
                          new NameValueUnit(OpenAtfxConstants.CONTEXT_WRITE_MODE, DataType.DT_STRING,
                                            OpenAtfxConstants.DEF_WRITE_MODE));
        contextByName.put(OpenAtfxConstants.CONTEXT_EXT_COMP_SEGSIZE,
                          new NameValueUnit(OpenAtfxConstants.CONTEXT_EXT_COMP_SEGSIZE, DataType.DT_LONGLONG,
                                            OpenAtfxConstants.DEF_EXT_COMP_SEGSIZE));
        contextByName.put(OpenAtfxConstants.CONTEXT_INDENT_XML,
                          new NameValueUnit(OpenAtfxConstants.CONTEXT_INDENT_XML, DataType.DT_STRING, "TRUE"));
        contextByName.put(OpenAtfxConstants.CONTEXT_WRITE_EXTERNALCOMPONENTS,
                          new NameValueUnit(OpenAtfxConstants.CONTEXT_WRITE_EXTERNALCOMPONENTS, DataType.DT_STRING, "FALSE"));
        
        return contextByName.values();
    }
}
