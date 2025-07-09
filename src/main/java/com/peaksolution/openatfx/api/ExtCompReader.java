package com.peaksolution.openatfx.api;

import com.peaksolution.openatfx.util.BufferedRandomAccessFile;
import com.peaksolution.openatfx.util.ODSHelper;
import org.asam.ods.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Utility class for reading values from external component files.
 * 
 * @author Christian Rechner, Markus Renner
 */
public class ExtCompReader {

    private static final String FILENAME_URL = "filename_url";
    private static final String START_OFFSET = "start_offset";
    private static final String COMPONENT_LENGTH = "component_length";
    private static final String VALUE_TYPE = "value_type";
    private static final String AOEXTERNALCOMPONENT = "aoexternalcomponent";
    private static final Logger LOG = LoggerFactory.getLogger(ExtCompReader.class);
    private static final int BUFFER_SIZE = 32768;

    private final OpenAtfxAPIImplementation api;

    public ExtCompReader(OpenAtfxAPIImplementation api) {
        this.api = api;
    }

    public SingleValue readValues(long iidLc, DataType targetDataType) throws OpenAtfxException {
        // read external component instances
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");
        Element ecElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
        Relation relExtComps = api.getRelationByBaseName(lcElement.getId(), "external_component");
        List<Long> iidExtComps = api.getRelatedInstanceIds(lcElement.getId(), iidLc, relExtComps);
        if (!iidExtComps.isEmpty()) {
            Collections.sort(iidExtComps, new ExternalComponentComparator(ecElement.getId()));
        }

        // get raw data type
        DataType rawDataType = targetDataType;
        Instance lcInstance = api.getInstanceById(lcElement.getId(), iidLc);
        if (lcInstance != null) {
            NameValueUnit valRawDatatype = lcInstance.getValue("raw_datatype");
            if (valRawDatatype != null && valRawDatatype.hasValidValue()) {
                int val = valRawDatatype.getValue().enumVal();
                if (val == 1) { // DT_STRING
                    rawDataType = DataType.DS_STRING;
                } else if (val == 2) { // DT_SHORT
                    rawDataType = DataType.DS_SHORT;
                } else if (val == 3) { // DT_FLOAT
                    rawDataType = DataType.DS_FLOAT;
                } else if (val == 4) { // DT_BOOLEAN
                    rawDataType = DataType.DS_BOOLEAN;
                } else if (val == 5) { // DT_BYTE
                    rawDataType = DataType.DS_BYTE;
                } else if (val == 6) { // DT_LONG
                    rawDataType = DataType.DS_LONG;
                } else if (val == 7) { // DT_DOUBLE
                    rawDataType = DataType.DS_DOUBLE;
                } else if (val == 8) { // DT_LONGLONG
                    rawDataType = DataType.DS_LONGLONG;
                } else if (val == 10) { // DT_DATE
                    rawDataType = DataType.DS_DATE;
                } else if (val == 11) { // DT_BYTESTR
                    rawDataType = DataType.DS_BYTESTR;
                } else if (val == 13) { // DT_COMPLEX
                    rawDataType = DataType.DS_COMPLEX;
                } else if (val == 14) { // DT_DCOMPLEX
                    rawDataType = DataType.DS_DCOMPLEX;
                } else if (val == 28) { // DT_EXTERNALREFERENCE
                    rawDataType = DataType.DS_EXTERNALREFERENCE;
                } else if (val == 30) { // DT_ENUM
                    rawDataType = DataType.DS_ENUM;
                }
            }
        }

        SingleValue tsValue = new SingleValue(rawDataType);
        // DS_STRING, DS_DATE, DS_BYTESTR
        if (rawDataType == DataType.DS_STRING || rawDataType == DataType.DS_DATE || rawDataType == DataType.DS_BYTESTR) {
            List<String> list = new ArrayList<>();
            for (long iidExtComp : iidExtComps) {
                list.addAll(readStringValues(iidExtComp));
            }
            tsValue.setValue(list.toArray(new String[0]));
        }
        // DS_NUMBER
        else {
            List<Number> list = new ArrayList<>();
            for (long iidExtComp : iidExtComps) {
                ByteOrder valuesByteOrder = api.getByteOrder(ecElement.getId(), iidExtComp);
                list.addAll(readNumberValues(iidExtComp, valuesByteOrder));
            }
            // DS_BOOLEAN
            if (rawDataType == DataType.DS_BOOLEAN) {
                boolean[] ar = new boolean[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).byteValue() != 0;
                }
                tsValue.setValue(ar);
            }
            // DS_BYTE
            else if (rawDataType == DataType.DS_BYTE) {
                byte[] ar = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).byteValue();
                }
                tsValue.setValue(ar);
            }
            // DS_SHORT
            else if (rawDataType == DataType.DS_SHORT) {
                short[] ar = new short[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).shortValue();
                }
                tsValue.setValue(ar);
            }
            // DS_LONG
            else if (rawDataType == DataType.DS_LONG) {
                int[] ar = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).intValue();
                }
                tsValue.setValue(ar);
            }
            // DS_DOUBLE
            else if (rawDataType == DataType.DS_DOUBLE) {
                double[] ar = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).doubleValue();
                }
                tsValue.setValue(ar);
            }
            // DS_LONGLONG
            else if (rawDataType == DataType.DS_LONGLONG) {
                long[] ar = new long[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).longValue();
                }
                tsValue.setValue(ar);
            }
            // DS_FLOAT
            else if (rawDataType == DataType.DS_FLOAT) {
                float[] ar = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ar[i] = list.get(i).floatValue();
                }
                tsValue.setValue(ar);
            }
            // DS_COMPLEX
            else if (rawDataType == DataType.DS_COMPLEX) {
                int size = list.size() / 2;
                Complex[] ar = new Complex[size];
                for (int i = 0; i < size; i++) {
                    ar[i] = new Complex();
                    ar[i].setR(list.get(i * 2).floatValue());
                    ar[i].setI(list.get(i * 2 + 1).floatValue());
                }
                tsValue.setValue(ar);
            }
            // DS_DCOMPLEX
            else if (rawDataType == DataType.DS_DCOMPLEX) {
                int size = list.size() / 2;
                DoubleComplex[] ar = new DoubleComplex[size];
                for (int i = 0; i < size; i++) {
                    ar[i] = new DoubleComplex();
                    ar[i].setR(list.get(i * 2).doubleValue());
                    ar[i].setI(list.get(i * 2 + 1).doubleValue());
                }
                tsValue.setValue(ar);
            }
            // unsupported
            else {
                throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                            "Reading values from external component not yet supported for datatype: "
                                                    + targetDataType);
            }
        }
        return tsValue;
    }

    public List<Number> readNumberValues(long iidExtComp, ByteOrder byteOrder) {
        long start = System.currentTimeMillis();

        List<Number> list = new ArrayList<>();
        Element ecElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
        long aidExtComp = ecElement.getId();
        Instance extCompInstance = api.getInstanceById(aidExtComp, iidExtComp);

        // get filename
        Path extCompFile = getExtCompFile(extCompInstance, false);

        // get value type
        extCompInstance.doesAttributeExist(null, VALUE_TYPE, true);
        NameValueUnit valueTypeNvu = extCompInstance.getValueByBaseName(VALUE_TYPE);
        int valueType = valueTypeNvu.getValue().enumVal();

        // read length
        extCompInstance.doesAttributeExist(null, COMPONENT_LENGTH, true);
        NameValueUnit compLengthNvu = extCompInstance.getValueByBaseName(COMPONENT_LENGTH);
        int componentLength = compLengthNvu.getValue().longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        extCompInstance.doesAttributeExist(null, START_OFFSET, true);
        long startOffset = 0;
        NameValueUnit startOffsetNvu = extCompInstance.getValueByBaseName(START_OFFSET);
        if (startOffsetNvu.getValue().discriminator() == DataType.DT_LONG) {
            startOffset = startOffsetNvu.getValue().longVal();
        } else if (startOffsetNvu.getValue().discriminator() == DataType.DT_LONGLONG) {
            startOffset = startOffsetNvu.getValue().longlongVal();
        }

        // read value offset
        extCompInstance.doesAttributeExist(null, "value_offset", true);
        NameValueUnit valueOffsetNvu = extCompInstance.getValueByBaseName("value_offset");
        int valueOffset = valueOffsetNvu.getValue().longVal();

        // read bit count
        short bitCount = 0;
        boolean bitCountExists = extCompInstance.doesAttributeExist(null, "ao_bit_count", false);
        if (bitCountExists) {
            NameValueUnit bitCountNvu = extCompInstance.getValueByBaseName("ao_bit_count");
            if (bitCountNvu != null) {
                bitCount = bitCountNvu.getValue().shortVal();
            }
        }

        // read bit offset
        short bitOffset = 0;
        boolean bitOffsetExists = extCompInstance.doesAttributeExist(null, "ao_bit_offset", false);
        if (bitOffsetExists) {
            NameValueUnit bitOffsetNvu = extCompInstance.getValueByBaseName("ao_bit_offset");
            if (bitOffsetNvu != null) {
                bitOffset = bitOffsetNvu.getValue().shortVal();
            }
        }

        // block_size
        extCompInstance.doesAttributeExist(null, "block_size", true);
        NameValueUnit blockSizeNvu = extCompInstance.getValueByBaseName("block_size");
        int blockSize = blockSizeNvu.getValue().longVal();

        // valuesperblock
        extCompInstance.doesAttributeExist(null, "valuesperblock", true);
        NameValueUnit valuesPerBlockNvu = extCompInstance.getValueByBaseName("valuesperblock");
        int valuesperblock = valuesPerBlockNvu.getValue().longVal();

        // read values
        try (RandomAccessFile raf = new BufferedRandomAccessFile(extCompFile.toFile(), "r", BUFFER_SIZE)) {
            raf.seek(startOffset);

            // initialize buffer
            ByteBuffer sourceMbb = ByteBuffer.allocate(blockSize);
            sourceMbb.order(byteOrder);

            // loop over blocks
            for (int i = 0; i < componentLength; i += valuesperblock) {

                // read whole block into memory
                byte[] block = new byte[blockSize];
                raf.read(block, 0, block.length);

                // make buildable with both java8 and java9
                Buffer.class.cast(sourceMbb).clear();
                sourceMbb.put(block);
                sourceMbb.position(valueOffset);

                // sub blocks are consecutive, puhh!
                for (int j = 0; j < valuesperblock; j++) {

                    // 1=dt_byte
                    if (valueType == 1) {
                        list.add(sourceMbb.get() & 0xff);
                    }
                    // 19=dt_sbyte
                    else if (valueType == 19) {
                        list.add(sourceMbb.get());
                    }
                    // 2=dt_short, 7=dt_short_beo
                    else if ((valueType == 2) || (valueType == 7)) {
                        list.add(sourceMbb.getShort());
                    }
                    // 3=dt_long, 8=dt_long_beo
                    else if ((valueType == 3) || (valueType == 8)) {
                        list.add(sourceMbb.getInt());
                    }
                    // 4=dt_longlong, 9=dt_longlong_beo
                    else if ((valueType == 4) || (valueType == 9)) {
                        list.add(sourceMbb.getLong());
                    }
                    // 5=ieeefloat4, 10=ieeefloat4_beo
                    else if ((valueType == 5) || (valueType == 10)) {
                        list.add(sourceMbb.getFloat());
                    }
                    // 6=ieeefloat8, 11=ieeefloat8_beo
                    else if ((valueType == 6) || (valueType == 11)) {
                        list.add(sourceMbb.getDouble());
                    }
                    // 21=dt_ushort, 22=dt_ushort_beo
                    else if ((valueType == 21) || (valueType == 22)) {
                        int val = sourceMbb.getShort() & 0xffff;
                        list.add(val);
                    }
                    // 23=dt_ulong, 24=dt_ulong_beo
                    else if ((valueType == 23) || (valueType == 24)) {
                        long val = sourceMbb.getInt() & 0xffffffffL;
                        list.add(val);
                    }
                    // 27=dt_bit_int, 28=dt_bit_int_beo
                    // 29=dt_bit_uint, 30=dt_bit_uint_beo
                    else if ((valueType == 27) || (valueType == 28) || (valueType == 29) || (valueType == 30)) {

                        // Read required number of bytes from the byte position within the file:
                        int bytesToRead = ((bitCount + bitOffset - 1) / 8) + 1;
                        byte[] tmp = new byte[bytesToRead];
                        sourceMbb.get(tmp);

                        list.add(ODSHelper.getBitShiftedIntegerValue(tmp, valueType, bitCount, bitOffset));
                    }
                    // unsupported data type
                    else {
                        throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED, "Unsupported 'value_type': "
                                + ODSHelper.valueType2String(valueType));
                    }
                }
            }

            extCompInstance.doesAttributeExist(null, FILENAME_URL, true);
            NameValueUnit fileNameUrlNvu = extCompInstance.getValueByBaseName(FILENAME_URL);
            String filenameUrl = fileNameUrlNvu.getValue().stringVal();
            if (LOG.isInfoEnabled()) {
                LOG.info("Read {} numeric values from component file '{}' in {}ms [value_type={}]", list.size(),
                         filenameUrl, System.currentTimeMillis() - start, ODSHelper.valueType2String(valueType));
            }
            return list;
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, e.getMessage());
        }
    }

    private Collection<String> readStringValues(long iidExtComp) {
        long start = System.currentTimeMillis();
        Element ecElement = api.getUniqueElementByBaseType(AOEXTERNALCOMPONENT);
        long aidExtComp = ecElement.getId();
        Instance extComp = api.getInstanceById(aidExtComp, iidExtComp);

        // get values File
        Path extCompFile = getExtCompFile(extComp, false);

        // get datatype
        NameValueUnit vtNvu = extComp.getValueByBaseName(VALUE_TYPE);
        int valueType = vtNvu.getValue().enumVal();
        if (valueType != 12 && valueType != 25) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_IMPLEMENTED,
                                        "Unsupported 'value_type' for data type DT_STRING or DT_DATE: " + valueType);
        }

        // read length
        NameValueUnit clNvu = extComp.getValueByBaseName(COMPONENT_LENGTH);
        int componentLength = clNvu.getValue().longVal();

        // read start offset, may be DT_LONG or DT_LONGLONG
        long startOffset = 0;
        NameValueUnit soNvu = extComp.getValueByBaseName(START_OFFSET);
        if (soNvu.getValue().discriminator() == DataType.DT_LONG) {
            startOffset = soNvu.getValue().longVal();
        } else if (soNvu.getValue().discriminator() == DataType.DT_LONGLONG) {
            startOffset = soNvu.getValue().longlongVal();
        }

        // value_offset is irrelevant according ODS Standard 3.42, page 3-51

        // read values
        byte[] backingBuffer = new byte[componentLength];
        List<String> list = new ArrayList<>();
        Charset charset = valueType == 12 ? ISO_8859_1 : UTF_8;
        try (RandomAccessFile raf = new BufferedRandomAccessFile(extCompFile.toFile(), "r", BUFFER_SIZE)) {
            raf.seek(startOffset);
            raf.read(backingBuffer, 0, backingBuffer.length);

            int startPosition = 0;
            for (int position = 0; position < componentLength; position++) {
                if (backingBuffer[position] == 0) {
                    list.add(new String(backingBuffer, startPosition, position - startPosition, charset));
                    startPosition = position + 1;
                }
            }

            NameValueUnit furlNvu = extComp.getValueByBaseName(FILENAME_URL);
            String filenameUrl = furlNvu.getValue().stringVal();
            if (LOG.isInfoEnabled()) {
                LOG.info("Read {} string values from component file '{}' in {}ms [value_type={}]", list.size(),
                         filenameUrl, System.currentTimeMillis() - start, ODSHelper.valueType2String(valueType));
            }
            return list;
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, e.getMessage());
        }
    }

    public SingleValue readFlags(long iidLc) throws OpenAtfxException {
        long start = System.currentTimeMillis();
        Element lcElement = api.getUniqueElementByBaseType("aolocalcolumn");

        // read external component instances
        long aidLc = lcElement.getId();
        Relation relExtComps = api.getRelationByBaseName(aidLc, "external_component");
        Collection<Element> ecElements = api.getElementsByBaseType(AOEXTERNALCOMPONENT);
        if (ecElements.isEmpty()) {
            return null;
        }
        long aidExtComp = ecElements.iterator().next().getId();

        Collection<Long> iidExtComps = api.getRelatedInstanceIds(aidLc, iidLc, relExtComps);
        if (iidExtComps.isEmpty()) {
            return null;
        }

        Collection<Instance> extComps = api.getInstances(aidExtComp, iidExtComps);

        // count the number of flags in all the external components
        int overallNrOfFlags = 0;
        for (Instance currentEc : extComps) {
            NameValueUnit clNvu = currentEc.getValueByBaseName(COMPONENT_LENGTH);
            if (clNvu == null) {
                return null;
            }
            overallNrOfFlags += clNvu.getValue().longVal();
        }
        short[] overallFlags = new short[overallNrOfFlags];

        // collect all flags from all external components
        Collection<String> flagsFileNames = new HashSet<>();
        int flagIndex = 0;
        for (Instance currentEc : extComps) {
            long iidExtComp = currentEc.getIid();
            // get flags file
            Path flagsFile = getExtCompFile(currentEc, true);
            if (flagsFile == null) {
                return null;
            }
            flagsFileNames.add(flagsFile.getFileName().toString());

            // read start offset, may be DT_LONG or DT_LONGLONG
            long flagsStartOffset = 0;
            NameValueUnit fsoNvu = currentEc.getValueByBaseName("flags_start_offset");
            if (fsoNvu == null) {
                throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND,
                                            "Attribute derived from base attribute 'flags_start_offset' not found");
            }
            if (fsoNvu.getValue().discriminator() == DataType.DT_LONG) {
                flagsStartOffset = fsoNvu.getValue().longVal();
            } else if (fsoNvu.getValue().discriminator() == DataType.DT_LONGLONG) {
                flagsStartOffset = fsoNvu.getValue().longlongVal();
            }

            // read and store flag values of current external component
            ByteOrder flagsByteOrder = api.getByteOrder(aidExtComp, iidExtComp);
            NameValueUnit clNvu = currentEc.getValueByBaseName(COMPONENT_LENGTH);
            int componentLength = clNvu.getValue().longVal();
            for (Short flag : readFlagsFromFile(flagsFile, flagsStartOffset, componentLength, flagsByteOrder)) {
                overallFlags[flagIndex++] = flag;
            }
        }

        // prepare return value
        if (LOG.isInfoEnabled()) {
            LOG.info("Read {} flags for LocalColumn {} from component file(s) '{}' in {}ms", overallNrOfFlags, iidLc,
                     flagsFileNames.stream().collect(Collectors.joining(",")), System.currentTimeMillis() - start);
        }
        return new SingleValue(DataType.DS_SHORT, overallFlags);
    }

    private List<Short> readFlagsFromFile(Path flagsFile, long flagsStartOffset, int componentLength,
            ByteOrder byteOrder) throws OpenAtfxException {
        List<Short> flags = new ArrayList<>();

        byte[] backingBuffer = new byte[2];
        // open source channel and read flag values
        try (RandomAccessFile raf = new BufferedRandomAccessFile(flagsFile.toFile(), "r", BUFFER_SIZE)) {
            raf.seek(flagsStartOffset);
            for (int i = 0; i < componentLength; i++) {
                raf.read(backingBuffer, 0, backingBuffer.length);
                ByteBuffer sourceMbb = ByteBuffer.wrap(backingBuffer);
                sourceMbb.order(byteOrder);
                flags.add(sourceMbb.getShort());
            }
        } catch (IOException e) {
            throw new OpenAtfxException(ErrorCode.AO_NOT_FOUND, e.getMessage());
        }

        return flags;
    }

    /**
     * Identifies the requested values or flags file of specified External Component. Tries to identify the file by the
     * respective reference attribute first and, if no valid value was found there, tries to find the AoFile instance
     * probably related to the External Component instead.
     * 
     * @param extComp
     * @param requestFlags if true, the ExtComp's flags file is requested, the values file otherwise
     * @return the defined File instance identified at the given external component, null if file could not be
     *         identified
     * @throws OpenAtfxException
     */
    private Path getExtCompFile(Instance extComp, boolean requestFlags) {
        String attrName = null;
        String fileRelBaseName = null;
        if (requestFlags) {
            attrName = "flags_filename_url";
            fileRelBaseName = "ao_flags_file";
        } else {
            attrName = FILENAME_URL;
            fileRelBaseName = "ao_values_file";
        }

        NameValueUnit fileNvu = extComp.getValueByBaseName(attrName);
        Path extCompFile = null;
        String location = null;
        if (fileNvu == null || !fileNvu.hasValidValue()) {
            // if no valid file reference is found, find the related AoFile if one exists
            Relation relFile = api.getRelationByBaseName(extComp.getAid(), fileRelBaseName);
            if (relFile == null) {
                return null;
            }

            Element fileElement = relFile.getElement2();
            List<Long> iidFiles = api.getRelatedInstanceIds(extComp.getAid(), extComp.getIid(), relFile);
            if (iidFiles.isEmpty()) {
                return null;
            }

            long iidFile = iidFiles.iterator().next();
            Instance relatedFile = api.getInstanceById(fileElement.getId(), iidFile);
            NameValueUnit locNvu = relatedFile.getValueByBaseName("ao_location");
            location = locNvu.getValue().stringVal();
        } else {
            location = fileNvu.getValue().stringVal();
        }
        if (location.isBlank()) {
            // handle empty value, for example because no flags file is set
            return null;
        }
        
        extCompFile = Paths.get(location);
        if (!extCompFile.toFile().exists()) {
            extCompFile = Paths.get(api.getContext().get(OpenAtfxConstants.CONTEXT_FILE_ROOT).getValue().stringVal(), location);
        }
        return extCompFile;
    }

    /**
     * Custom comparator so sort multiple instances of 'AoExternalComponent' for the same 'AoLocalColumn' instance by
     * the value of the base attribute 'ordinal_number'.
     */
    private class ExternalComponentComparator implements Comparator<Long> {

        private static final String ORDINAL_NUMBER = "ordinal_number";
        private final long ecAid;

        public ExternalComponentComparator(long ecAid) {
            this.ecAid = ecAid;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Long iidExtComp1, Long iidExtComp2) {
            try {
                Integer ordExtComp1 = 0;
                Integer ordExtComp2 = 0;

                Instance inst1 = api.getInstanceById(ecAid, iidExtComp1);
                Instance inst2 = api.getInstanceById(ecAid, iidExtComp2);
                if (!inst1.doesAttributeExist(null, ORDINAL_NUMBER, false)) {
                    LOG.warn("Attribute derived from '{}' not found at {}", ORDINAL_NUMBER, inst1);
                    return 0;
                } else if (!inst2.doesAttributeExist(null, ORDINAL_NUMBER, false)) {
                    LOG.warn("Attribute derived from '{}' not found at {}", ORDINAL_NUMBER, inst2);
                    return 0;
                }

                NameValueUnit on1 = inst1.getValueByBaseName(ORDINAL_NUMBER);
                ordExtComp1 = on1.getValue().longVal();
                NameValueUnit on2 = inst2.getValueByBaseName(ORDINAL_NUMBER);
                ordExtComp2 = on2.getValue().longVal();

                return ordExtComp1.compareTo(ordExtComp2);
            } catch (OpenAtfxException e) {
                LOG.warn(e.getMessage(), e);
                return 0;
            }
        }
    }
}
