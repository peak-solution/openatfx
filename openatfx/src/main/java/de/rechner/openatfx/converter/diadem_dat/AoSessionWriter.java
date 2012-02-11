package de.rechner.openatfx.converter.diadem_dat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.DataType;
import org.asam.ods.InstanceElement;

import de.rechner.openatfx.converter.ConvertException;
import de.rechner.openatfx.util.ODSHelper;


/**
 * Main class for writing the DAT file content into an ATFX file
 * 
 * @author Christian Rechner
 */
class AoSessionWriter {

    private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);
    private static final String DATEFORMAT = "dd.MM.yyyy HH:mm:ss";

    // mapping between the DAT data type and the ASAM ODS data type enum value
    private static final Map<String, Integer> MEQ_DATATYPE_MAP = new HashMap<String, Integer>();
    static {
        MEQ_DATATYPE_MAP.put("INT16", 2); // DT_SHORT
        MEQ_DATATYPE_MAP.put("REAL32", 3); // DT_FLOAT
        MEQ_DATATYPE_MAP.put("REAL48", null); // not yet supported
        MEQ_DATATYPE_MAP.put("INT32", 6); // DT_LONG
        MEQ_DATATYPE_MAP.put("REAL64", 7); // DT_DOUBLE
        MEQ_DATATYPE_MAP.put("MSREAL32", null); // not yet supported
        MEQ_DATATYPE_MAP.put("WORD8", null); // not yet supported
        MEQ_DATATYPE_MAP.put("WORD16", null); // not yet supported
        MEQ_DATATYPE_MAP.put("WORD32", null); // not yet supported
        MEQ_DATATYPE_MAP.put("TWOC12", null); // not yet supported
        MEQ_DATATYPE_MAP.put("TWOC16", null); // not yet supported
        MEQ_DATATYPE_MAP.put("ASCII", null); // not yet supported
    }

    // mapping between the DAT data type and the ASAM ODS typespec_enum value
    private static final Map<String, Integer> EC_TYPE_MAP = new HashMap<String, Integer>();
    static {
        EC_TYPE_MAP.put("INT16", 2); // dt_short
        EC_TYPE_MAP.put("INT32", 3); // dt_long
        EC_TYPE_MAP.put("REAL32", 5); // ieeefloat4
        EC_TYPE_MAP.put("REAL64", 6); // ieeefloat8
        EC_TYPE_MAP.put("REAL48", null); // not yet supported
        EC_TYPE_MAP.put("MSREAL32", null); // not yet supported
        EC_TYPE_MAP.put("WORD8", null); // not yet supported
        EC_TYPE_MAP.put("WORD16", null); // not yet supported
        EC_TYPE_MAP.put("WORD32", null); // not yet supported
        EC_TYPE_MAP.put("TWOC12", null); // not yet supported
        EC_TYPE_MAP.put("TWOC16", null); // not yet supported
        EC_TYPE_MAP.put("ASCII", null); // not yet supported
    }

    private final Map<File, FileChannel> targetFileChannels;
    private File atfxFile;

    /**
     * Constructor.
     */
    public AoSessionWriter() {
        this.targetFileChannels = new HashMap<File, FileChannel>();
    }

    /**
     * Appends the data of an ATFX file to
     * 
     * @param iePrj The parent 'AoTest' instance.
     * @param atfxFile The target ATFX file.
     * @param datHeader The source DAT file content.
     * @throws ConvertException Error converting.
     * @throws IOException Error reading file content.
     */
    public synchronized void writeDataToAoTest(InstanceElement iePrj, File atfxFile, DatHeader datHeader)
            throws ConvertException, IOException {
        this.atfxFile = atfxFile;
        this.targetFileChannels.clear();

        try {
            ApplicationStructure as = iePrj.getApplicationElement().getApplicationStructure();
            ApplicationElement aePrj = as.getElementByName("prj");
            ApplicationElement aeTst = as.getElementByName("tst");
            ApplicationRelation relPrjTsts = as.getRelations(aePrj, aeTst)[0];

            // create "AoSubTest" instance
            String fileName = datHeader.getSourceFile().getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            InstanceElement ieTst = aeTst.createInstance(fileName);
            iePrj.createRelation(relPrjTsts, ieTst);

            // write "AoMeasurement" instances
            writeMea(ieTst, datHeader);
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } finally {
            // close buffers
            for (FileChannel targetChannel : this.targetFileChannels.values()) {
                targetChannel.close();
                targetChannel = null;
            }
        }
    }

    /**
     * Write the instance of 'AoMeasurement'.
     * 
     * @param ieTst The parent 'AoTest' instance.
     * @param datHeader The DAT header.
     * @throws AoException Error writing instance.
     * @throws ConvertException
     */
    private void writeMea(InstanceElement ieTst, DatHeader datHeader) throws AoException, ConvertException {
        ApplicationStructure as = ieTst.getApplicationElement().getApplicationStructure();
        ApplicationElement aeTst = as.getElementByName("tst");
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationRelation relTstMea = as.getRelations(aeTst, aeMea)[0];

        // create "AoMeasurement" instance and write descriptive data to instance attributes
        InstanceElement ieMea = aeMea.createInstance(ieTst.getName());
        ieTst.createRelation(relTstMea, ieMea);
        ieMea.setValue(ODSHelper.createStringNVU("origin", datHeader.getGlobalHeaderEntry(DatHeader.KEY_ORIGIN)));
        ieMea.setValue(ODSHelper.createStringNVU("revision", datHeader.getGlobalHeaderEntry(DatHeader.KEY_REVISION)));
        ieMea.setValue(ODSHelper.createStringNVU("description",
                                                 datHeader.getGlobalHeaderEntry(DatHeader.KEY_DESCRIPTION)));
        ieMea.setValue(ODSHelper.createStringNVU("person", datHeader.getGlobalHeaderEntry(DatHeader.KEY_PERSON)));
        // parse date_created,mea_begin,mea_end
        String dateStr = datHeader.getGlobalHeaderEntry(DatHeader.KEY_DATE);
        String timeStr = datHeader.getGlobalHeaderEntry(DatHeader.KEY_TIME);
        ieMea.setValue(ODSHelper.createDateNVU("date_created", asODSdate(dateStr, timeStr)));
        dateStr = datHeader.getGlobalHeaderComment("Startdatum");
        timeStr = datHeader.getGlobalHeaderComment("Startzeit");
        ieMea.setValue(ODSHelper.createDateNVU("mea_begin", asODSdate(dateStr, timeStr)));
        dateStr = datHeader.getGlobalHeaderComment("Enddatum");
        timeStr = datHeader.getGlobalHeaderComment("Endzeit");
        ieMea.setValue(ODSHelper.createDateNVU("mea_end", asODSdate(dateStr, timeStr)));

        for (String commentKey : datHeader.listGlobalHeaderComments()) {
            String commentValue = datHeader.getGlobalHeaderComment(commentKey);
            ieMea.addInstanceAttribute(ODSHelper.createStringNVU(commentKey, commentValue));
        }

        // write "AoSubMatrix" instances
        Map<Integer, InstanceElement> smMap = writeSubMatrices(ieMea, datHeader);

        // write "AoMeasurementQuantity" instances
        writeMeq(ieMea, smMap, datHeader);
    }

    /**
     * Writes the instances of 'AoSubMatrix'.
     * 
     * @param ieMea The 'AoMeasurement' instance.
     * @param datHeader The DAT header.
     * @return Map having the number of values as key and the SubMatrix instance as value.
     * @throws AoException Error writing SubMatrix instances.
     * @throws ConvertException Information not found.
     */
    private Map<Integer, InstanceElement> writeSubMatrices(InstanceElement ieMea, DatHeader datHeader)
            throws AoException, ConvertException {
        ApplicationStructure as = ieMea.getApplicationElement().getApplicationStructure();
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeSm = as.getElementByName("sm");
        ApplicationRelation relMeaSm = as.getRelations(aeMea, aeSm)[0];

        // key=number_of_rows, value=instance
        Map<Integer, InstanceElement> smMap = new HashMap<Integer, InstanceElement>();
        for (String channelName : datHeader.listChannelNames()) {
            String noOfRowsStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_NO_OF_VALUES);
            if (noOfRowsStr == null || noOfRowsStr.length() < 1) {
                LOG.warn("Unable to read number of values for channel: " + channelName);
                noOfRowsStr = "0";
            }
            Integer noOfRows = Integer.valueOf(noOfRowsStr.trim());
            if (!smMap.containsKey(noOfRows)) {
                InstanceElement ieSm = aeSm.createInstance("SubMatrix#" + (smMap.size() + 1));
                ieMea.createRelation(relMeaSm, ieSm);
                ieSm.setValue(ODSHelper.createLongNVU("number_of_rows", noOfRows));
                smMap.put(noOfRows, ieSm);
            }
        }

        return smMap;
    }

    /**
     * Write the instances of 'AoMeasurementQuantity'.
     * 
     * @param ieMea The 'AoMeasurement' instance.
     * @param smMap The SubMatrix map.
     * @param datHeader The DAT header data.
     * @throws AoException Error writing data.
     * @throws ConvertException
     */
    private void writeMeq(InstanceElement ieMea, Map<Integer, InstanceElement> smMap, DatHeader datHeader)
            throws AoException, ConvertException {
        ApplicationStructure as = ieMea.getApplicationElement().getApplicationStructure();
        ApplicationElement aeMea = as.getElementByName("mea");
        ApplicationElement aeMeq = as.getElementByName("meq");
        ApplicationElement aeUnt = as.getElementByName("unt");
        ApplicationRelation relMeaMeq = as.getRelations(aeMea, aeMeq)[0];
        ApplicationRelation relMeqUnt = as.getRelations(aeMeq, aeUnt)[0];

        // iterate over channels
        for (String channelName : datHeader.listChannelNames()) {

            InstanceElement ieMeq = aeMeq.createInstance(channelName);
            ieMea.createRelation(relMeaMeq, ieMeq);

            // description
            String description = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_COMMENT);
            ieMeq.setValue(ODSHelper.createStringNVU("description", description));

            // unit
            String unit = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_UNIT);
            if (unit != null && unit.length() > 0) {
                InstanceElement ieUnt = aeUnt.getInstanceByName(unit);
                if (ieUnt == null) {
                    ieUnt = aeUnt.createInstance(unit);
                    ieUnt.setValue(ODSHelper.createDoubleNVU("factor", 1d));
                    ieUnt.setValue(ODSHelper.createDoubleNVU("offset", 0d));
                }
                ieMeq.createRelation(relMeqUnt, ieUnt);
            }

            // datatype: only for explicit channels
            String type = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_TYPE);
            if (type == null || type.length() < 1) {
                throw new ConvertException("Channel type not found for: " + channelName);
            }
            String dt = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_DATATYPE);
            if (type.equals("IMPLICIT")) {
                ieMeq.setValue(ODSHelper.createEnumNVU("dt", 7)); // always DT_DOUBLE
            } else if (type.equals("EXPLICIT")) {
                Integer dtEnum = MEQ_DATATYPE_MAP.get(dt);
                if (dtEnum == null) {
                    throw new ConvertException("Unsupported DAT datatype: " + dt);
                }
                ieMeq.setValue(ODSHelper.createEnumNVU("dt", dtEnum));
            } else {
                throw new ConvertException("Unknown type: " + channelName);
            }

            // 'AoLocalColumn'
            writeLc(ieMea, ieMeq, smMap, datHeader);
        }
    }

    /**
     * Writes the instance of type 'AoLocalColumn' to the session.
     * 
     * @param ieMea
     * @param ieMeq
     * @param smMap
     * @param datHeader
     * @throws AoException
     * @throws ConvertException
     */
    private void writeLc(InstanceElement ieMea, InstanceElement ieMeq, Map<Integer, InstanceElement> smMap,
            DatHeader datHeader) throws AoException, ConvertException {
        ApplicationStructure as = ieMeq.getApplicationElement().getApplicationStructure();
        ApplicationElement aeMeq = as.getElementByName("meq");
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeSm = as.getElementByName("sm");
        ApplicationRelation relMeqLc = as.getRelations(aeMeq, aeLc)[0];
        ApplicationRelation relSmLc = as.getRelations(aeSm, aeLc)[0];
        String channelName = ieMeq.getName();

        InstanceElement ieLc = aeLc.createInstance(channelName);
        ieMeq.createRelation(relMeqLc, ieLc);

        // relation to SubMatrix
        String noOfRowsStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_NO_OF_VALUES);
        if (noOfRowsStr == null || noOfRowsStr.length() < 1) {
            noOfRowsStr = "0";
        }
        Integer noOfRows = Integer.valueOf(noOfRowsStr.trim());
        InstanceElement ieSm = smMap.get(noOfRows);
        ieSm.createRelation(relSmLc, ieLc);

        // global_flag
        ieLc.setValue(ODSHelper.createShortNVU("global", (short) 15));

        // independent flag
        ieLc.setValue(ODSHelper.createShortNVU("idp", (short) 0));

        // factor and offset
        double offset = 0d;
        double factor = 1d;
        String offsetStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_OFFSET);
        if (offsetStr != null && offsetStr.length() > 0) {
            offset = Double.valueOf(offsetStr);
        }
        String factorStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FACTOR);
        if (factorStr != null && factorStr.length() > 0) {
            factor = Double.valueOf(factorStr);
        }

        // sequence_representation and factor / offset
        String seqRepStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_TYPE);
        if (seqRepStr == null || seqRepStr.length() < 1) {
            throw new ConvertException("Channel type not found for: " + channelName);
        }
        // implicit linear
        else if (seqRepStr.equals("IMPLICIT")) {
            ieLc.setValue(ODSHelper.createDoubleSeqNVU("gen_params", new double[] { offset, factor }));
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 2));
            double min = offset;
            double max = offset + (factor * noOfRows);
            double avg = (min + max) / 2;
            ieMeq.setValue(ODSHelper.createDoubleNVU("min", min));
            ieMeq.setValue(ODSHelper.createDoubleNVU("max", max));
            ieMeq.setValue(ODSHelper.createDoubleNVU("avg", avg));
            return;
        }
        // raw_linear_external / external component
        else if (offset != 0d || factor != 1d) {
            ieLc.setValue(ODSHelper.createDoubleSeqNVU("gen_params", new double[] { offset, factor }));
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 8));
        }
        // external_component
        else {
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 7));
        }

        // write AoExternalComponent
        writeEc(ieMea, ieMeq, ieLc, datHeader, channelName, noOfRows, offset, factor);
    }

    private void writeEc(InstanceElement ieMea, InstanceElement ieMeq, InstanceElement ieLc, DatHeader datHeader,
            String channelName, int noOfRows, double offset, double factor) throws AoException, ConvertException {
        ApplicationStructure as = ieLc.getApplicationElement().getApplicationStructure();
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeEc = as.getElementByName("ec");
        ApplicationRelation relLcEc = as.getRelations(aeLc, aeEc)[0];

        // create AoExternalComponenent instance
        InstanceElement ieEc = aeEc.createInstance("ec");
        ieLc.createRelation(relLcEc, ieEc);

        RandomAccessFile raf = null;
        FileChannel sourceChannel = null;
        MappedByteBuffer sourceMbb = null;
        try {
            // open source channel
            String sourceFilename = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FILENAME);
            File sourceDir = datHeader.getSourceFile().getParentFile();
            File sourceFile = new File(sourceDir, sourceFilename);
            raf = new RandomAccessFile(sourceFile, "r");
            sourceChannel = raf.getChannel();
            sourceMbb = sourceChannel.map(MapMode.READ_ONLY, 0, sourceFile.length());

            // set byte order
            ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
            String byteOrderStr = datHeader.getGlobalHeaderEntry(DatHeader.KEY_BYTE_ORDER);
            if (byteOrderStr != null && byteOrderStr.equals("Low -> High")) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            }
            sourceMbb.order(byteOrder);

            // open target channel
            File targetDir = this.atfxFile.getParentFile();
            File targetFile = new File(targetDir, "binary" + ODSHelper.asJLong(ieMea.getId()) + ".bin");
            FileChannel targetChannel = this.targetFileChannels.get(targetFile);
            if (targetChannel == null) {
                targetChannel = new FileOutputStream(targetFile).getChannel();
                this.targetFileChannels.put(targetFile, targetChannel);
            }

            // read header data
            String dt = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_DATATYPE);

            // DT_SHORT
            if (dt.equals("INT16")) {
                writeEcValues(ieMeq, ieEc, datHeader, channelName, noOfRows, offset, factor, DataType.DT_SHORT, 2,
                              sourceMbb, targetFile, targetChannel);
            }
            // DT_LONG
            else if (dt.equals("INT32")) {
                writeEcValues(ieMeq, ieEc, datHeader, channelName, noOfRows, offset, factor, DataType.DT_LONG, 4,
                              sourceMbb, targetFile, targetChannel);
            }
            // DT_FLOAT
            else if (dt.equals("REAL32")) {
                writeEcValues(ieMeq, ieEc, datHeader, channelName, noOfRows, offset, factor, DataType.DT_FLOAT, 4,
                              sourceMbb, targetFile, targetChannel);
            }
            // DT_DOUBLE
            if (dt.equals("REAL64")) {
                writeEcValues(ieMeq, ieEc, datHeader, channelName, noOfRows, offset, factor, DataType.DT_DOUBLE, 8,
                              sourceMbb, targetFile, targetChannel);
            }

        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        } finally {
            try {
                unmap(sourceChannel, sourceMbb);
                sourceChannel.close();
                sourceChannel = null;
                sourceMbb = null;
                raf.close();
                raf = null;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void writeEcValues(InstanceElement ieMeq, InstanceElement ieEc, DatHeader datHeader, String channelName,
            int noOfRows, double offset, double factor, DataType dt, int blockSize, ByteBuffer sourceMbb,
            File targetFile, FileChannel targetChannel) throws AoException, ConvertException, IOException {
        // read meta info from DAT header
        int fileOffset = Integer.valueOf(datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FILE_OFFSET).trim());
        String datDataType = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_DATATYPE);

        // prepare target data structures
        long targetStartOffset = targetChannel.position();
        ByteBuffer targetBb = ByteBuffer.allocate(noOfRows * blockSize);
        targetBb.order(ByteOrder.LITTLE_ENDIAN);
        Number[] data = new Number[noOfRows];

        // obtain store method
        int methodNo = 0; // 0=BLOCK, 1=CHANNEL
        int chOffset = 0; // only for block
        String method = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_METHOD);

        if (method.equals("BLOCK")) {
            methodNo = 0;
            String chOffsetStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_OFFSET);
            chOffset = Integer.valueOf(chOffsetStr.trim());
        } else if (method.equals("CHANNEL")) {
            methodNo = 1;
        }

        // iterate over file data
        for (int i = 0; i < noOfRows; i++) {

            // calc file idx
            int idx = 0;
            if (methodNo == 0) {
                idx = (fileOffset + (chOffset * i) - 1) * blockSize;
            } else {
                idx = (fileOffset + i - 1) * blockSize;
            }

            // read data from file and store to buffer
            if (dt == DataType.DT_SHORT) {
                data[i] = sourceMbb.getShort(idx);
                targetBb.putShort(i * blockSize, data[i].shortValue());
            } else if (dt == DataType.DT_LONG) {
                data[i] = sourceMbb.getInt(idx);
                targetBb.putInt(i * blockSize, data[i].intValue());
            } else if (dt == DataType.DT_FLOAT) {
                data[i] = sourceMbb.getFloat(idx);
                targetBb.putFloat(i * blockSize, data[i].intValue());
            } else if (dt == DataType.DT_DOUBLE) {
                data[i] = sourceMbb.getDouble(idx);
                targetBb.putDouble(i * blockSize, data[i].intValue());
            }
        }

        // write buffer to file
        targetChannel.write(targetBb);

        // set external component values
        ieEc.setValue(ODSHelper.createLongNVU("length", noOfRows));
        ieEc.setValue(ODSHelper.createLongLongNVU("sofs", targetStartOffset));
        ieEc.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEc.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieEc.setValue(ODSHelper.createEnumNVU("type", EC_TYPE_MAP.get(datDataType)));
        ieEc.setValue(ODSHelper.createLongNVU("blocksize", blockSize));
        ieEc.setValue(ODSHelper.createStringNVU("filename_url", targetFile.getName()));

        // calculate min/max/avg/dev and update measurement quantity instance
        Double min = calcMin(data);
        if (min != null) {
            min = (min * factor) + offset;
        }
        Double max = calcMax(data);
        if (max != null) {
            max = (max * factor) + offset;
        }
        Double avg = calcAvg(data);
        if (avg != null) {
            avg = (avg * factor) + offset;
        }
        Double dev = calcStdDev(data);
        if (dev != null) {
            dev = (dev * factor) + offset;
        }

        ieMeq.setValue(ODSHelper.createDoubleNVU("min", min));
        ieMeq.setValue(ODSHelper.createDoubleNVU("max", max));
        ieMeq.setValue(ODSHelper.createDoubleNVU("avg", avg));
        ieMeq.setValue(ODSHelper.createDoubleNVU("dev", dev));
    }

    /****************************************************************************************************
     * utility methods
     ****************************************************************************************************/

    private static String asODSdate(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.length() < 1 || timeStr == null || timeStr.length() < 1) {
            return "";
        }
        String startDateTime = dateStr + " " + timeStr;
        SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        try {
            Date date = sdf.parse(startDateTime);
            return ODSHelper.asODSDate(date);
        } catch (ParseException e) {
            LOG.warn(e.getMessage());
        }
        return "";
    }

    private static Double calcMin(Number[] numbers) {
        if (numbers.length > 0) {
            double minValue = numbers[0].doubleValue();
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i].doubleValue() < minValue) {
                    minValue = numbers[i].doubleValue();
                }
            }
            return minValue;
        }
        return null;
    }

    private static Double calcMax(Number[] numbers) {
        if (numbers.length > 0) {
            double maxValue = numbers[0].doubleValue();
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i].doubleValue() > maxValue) {
                    maxValue = numbers[i].doubleValue();
                }
            }
            return maxValue;
        }
        return null;
    }

    private static Double calcAvg(Number[] numbers) {
        if (numbers.length > 0) {
            double sum = 0;
            for (int i = 0; i < numbers.length; i++) {
                sum += numbers[i].doubleValue();
            }
            return sum / numbers.length;
        }
        return null;
    }

    private static Double calcStdDev(Number[] numbers) {
        if (numbers.length > 1) {
            double mean = 0;
            final int n = numbers.length;
            for (int i = 0; i < n; i++) {
                mean += numbers[i].doubleValue();
            }
            mean /= n;
            // calculate the sum of squares
            double sum = 0;
            for (int i = 0; i < n; i++) {
                final double v = numbers[i].doubleValue() - mean;
                sum += v * v;
            }
            // Change to ( n - 1 ) to n if you have complete data instead of a sample.
            return Math.sqrt(sum / (n - 1));
        }
        return null;
    }

    private static void unmap(FileChannel fc, MappedByteBuffer bb) throws Exception {
        Class<?> fcClass = fc.getClass();
        java.lang.reflect.Method unmapMethod = fcClass.getDeclaredMethod("unmap",
                                                                         new Class[] { java.nio.MappedByteBuffer.class });
        unmapMethod.setAccessible(true);
        unmapMethod.invoke(null, new Object[] { bb });
    }

}
