package de.rechner.openatfx.converter.diadem_dat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
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
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.T_LONGLONG;

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
        MEQ_DATATYPE_MAP.put("REAL32", 3); // DT_FLOAT
        MEQ_DATATYPE_MAP.put("REAL48", null); // not yet supported
        MEQ_DATATYPE_MAP.put("REAL64", 7); // DT_DOUBLE
        MEQ_DATATYPE_MAP.put("MSREAL32", null); // not yet supported
        MEQ_DATATYPE_MAP.put("INT16", null); // not yet supported
        MEQ_DATATYPE_MAP.put("INT32", 6); // DT_LONG
        MEQ_DATATYPE_MAP.put("WORD8", null); // not yet supported
        MEQ_DATATYPE_MAP.put("WORD16", null); // not yet supported
        MEQ_DATATYPE_MAP.put("WORD32", null); // not yet supported
        MEQ_DATATYPE_MAP.put("TWOC12", null); // not yet supported
        MEQ_DATATYPE_MAP.put("TWOC16", null); // not yet supported
        MEQ_DATATYPE_MAP.put("ASCII", null); // not yet supported
    }

    private final Map<File, MappedByteBuffer> sourceFileChannels;
    private final Map<File, FileChannel> targetFileChannels;
    private File atfxFile;

    /**
     * Constructor.
     */
    public AoSessionWriter() {
        this.sourceFileChannels = new HashMap<File, MappedByteBuffer>();
        this.targetFileChannels = new HashMap<File, FileChannel>();
    }

    public synchronized void writeDataToSession(AoSession aoSession, File atfxFile, DatHeader datHeader)
            throws ConvertException, IOException {
        this.atfxFile = atfxFile;
        this.sourceFileChannels.clear();
        this.targetFileChannels.clear();

        try {
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeEnv = as.getElementByName("env");
            ApplicationElement aeTst = as.getElementByName("tst");
            ApplicationRelation relEnvTsts = as.getRelations(aeEnv, aeTst)[0];

            // get "AoEnvironment" instance
            InstanceElement ieEnv = aeEnv.getInstanceById(new T_LONGLONG(0, 1));

            // create "AoTest" instance
            String fileName = datHeader.getSourceFile().getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            InstanceElement ieTst = aeTst.createInstance(fileName);
            ieEnv.createRelation(relEnvTsts, ieTst);

            // write "AoMeasurement" instances
            writeMea(ieTst, datHeader);
        } catch (AoException e) {
            LOG.error(e.reason, e);
            throw new ConvertException(e.reason, e);
        } finally {
            // close buffers
            for (FileChannel targetChannel : this.targetFileChannels.values()) {
                targetChannel.close();
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
        InstanceElement ieMea = aeMea.createInstance("RawData");
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
                throw new ConvertException("Unable to read number of values for channel: " + channelName);
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
                ieMeq.setValue(ODSHelper.createEnumNVU("datatype", 7));
            } else if (type.equals("EXPLICIT")) {
                Integer dtEnum = MEQ_DATATYPE_MAP.get(dt);
                if (dtEnum == null) {
                    throw new ConvertException("Unsupported DAT datatype: " + dt);
                }
                ieMeq.setValue(ODSHelper.createEnumNVU("datatype", dtEnum));
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
        Integer noOfRows = Integer.valueOf(noOfRowsStr.trim());
        InstanceElement ieSm = smMap.get(noOfRows);
        ieSm.createRelation(relSmLc, ieLc);

        // global_flag
        ieLc.setValue(ODSHelper.createShortNVU("global", (short) 15));

        // independent flag
        ieLc.setValue(ODSHelper.createShortNVU("idp", (short) 0));

        // sequence_representation and factor / offset
        String seqRepStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_TYPE);
        String offsetStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_OFFSET);
        String factorStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FACTOR);
        if (seqRepStr == null || seqRepStr.length() < 1) {
            throw new ConvertException("Channel type not found for: " + channelName);
        }
        // implicit linear
        else if (seqRepStr.equals("IMPLICIT")) {
            if (offsetStr == null || offsetStr.length() < 1) {
                throw new ConvertException("Implict channels need an offset: " + channelName);
            }
            double offset = Double.valueOf(offsetStr);
            double factor = Double.valueOf(factorStr);
            ieLc.setValue(ODSHelper.createDoubleSeqNVU("gen_params", new double[] { offset, factor }));
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 2));
            ieMeq.setValue(ODSHelper.createDoubleNVU("min", offset));
            ieMeq.setValue(ODSHelper.createDoubleNVU("max", (offset + (factor * noOfRows))));
            return;
        }
        // raw_linear_external / external component
        else if (offsetStr != null && offsetStr.length() > 0 && factorStr != null && factorStr.length() > 0) {
            double offset = Double.valueOf(offsetStr);
            double factor = Double.valueOf(factorStr);
            ieLc.setValue(ODSHelper.createDoubleSeqNVU("gen_params", new double[] { offset, factor }));
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 8));
        }
        // external_component
        else {
            ieLc.setValue(ODSHelper.createEnumNVU("seq_rep", 7));
        }

        // write AoExternalComponent
        writeEc(ieMea, ieMeq, ieLc, datHeader);
    }

    private void writeEc(InstanceElement ieMea, InstanceElement ieMeq, InstanceElement ieLc, DatHeader datHeader)
            throws AoException, ConvertException {
        ApplicationStructure as = ieLc.getApplicationElement().getApplicationStructure();
        ApplicationElement aeLc = as.getElementByName("lc");
        ApplicationElement aeEc = as.getElementByName("ec");
        ApplicationRelation relLcEc = as.getRelations(aeLc, aeEc)[0];
        String channelName = ieLc.getName();

        // create AoExternalComponenent instance
        InstanceElement ieEc = aeEc.createInstance("ec");
        ieLc.createRelation(relLcEc, ieEc);

        try {
            // open source channel
            String sourceFilename = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FILENAME);
            File sourceDir = datHeader.getSourceFile().getParentFile();
            File sourceFile = new File(sourceDir, sourceFilename);
            MappedByteBuffer sourceMbb = this.sourceFileChannels.get(sourceFile);
            if (sourceMbb == null) {
                FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
                sourceMbb = sourceChannel.map(MapMode.READ_ONLY, 0, sourceFile.length());
                // set byte order
                ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
                String byteOrderStr = datHeader.getGlobalHeaderEntry(DatHeader.KEY_BYTE_ORDER);
                if (byteOrderStr != null && byteOrderStr.equals("Low -> High")) {
                    byteOrder = ByteOrder.BIG_ENDIAN;
                }
                sourceMbb.order(byteOrder);
                this.sourceFileChannels.put(sourceFile, sourceMbb);
            }

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

            // DT_FLOAT
            if (dt.equals("REAL32")) {
                writeEcREAL32(ieMeq, ieEc, datHeader, channelName, sourceMbb, targetFile, targetChannel);
            }
            // DT_DOUBLE
            if (dt.equals("REAL64")) {
                writeEcREAL64(ieMeq, ieEc, datHeader, channelName, sourceMbb, targetFile, targetChannel);
            }
            // DT_LONG
            else if (dt.equals("INT32")) {

            }

        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ConvertException(e.getMessage(), e);
        }
    }

    private void writeEcREAL32(InstanceElement ieMeq, InstanceElement ieEc, DatHeader datHeader, String channelName,
            MappedByteBuffer sourceMbb, File targetFile, FileChannel targetChannel) throws AoException,
            ConvertException, IOException {
        // read meta info from DAT header
        int noOfRows = Integer.valueOf(datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_NO_OF_VALUES).trim());
        int fileOffset = Integer.valueOf(datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FILE_OFFSET).trim());
        String method = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_METHOD);
        long targetStartOffset = targetChannel.position();

        // FloatBuffer sourceFb = sourceMbb.asFloatBuffer();
        ByteBuffer targetBb = ByteBuffer.allocate(noOfRows * 4);
        targetBb.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer targetFb = targetBb.asFloatBuffer();

        float[] data = new float[noOfRows];

        // store method BLOCK
        if (method.equals("BLOCK")) {
            String chOffsetStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_OFFSET);
            int chOffset = Integer.valueOf(chOffsetStr.trim());
            for (int i = 0; i < noOfRows; i++) {
                int idx = (chOffset * i) + fileOffset - 1;
                data[i] = sourceMbb.getFloat(idx);
                targetFb.put(data[i]);
            }
        }
        // store method CHANNEL
        else if (method.equals("CHANNEL")) {
            for (int i = 0; i < noOfRows; i++) {
                int idx = fileOffset + i - 1;
                data[i] = sourceMbb.getFloat(idx * 4);
                targetFb.put(data[i]);
            }
        } else {
            throw new ConvertException("Store method not yet supported: " + method);
        }

        targetChannel.write(targetBb);

        // set external component values
        ieEc.setValue(ODSHelper.createLongNVU("length", noOfRows));
        ieEc.setValue(ODSHelper.createLongLongNVU("start_offset", targetStartOffset));
        ieEc.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEc.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieEc.setValue(ODSHelper.createEnumNVU("type", 5));
        ieEc.setValue(ODSHelper.createLongNVU("blocksize", 4));
        ieEc.setValue(ODSHelper.createStringNVU("filename_url", targetFile.getAbsolutePath()));

        // calculate min/max/avg/dev and update measurement quantity instance
        ieMeq.setValue(ODSHelper.createDoubleNVU("min", calcMin(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("max", calcMax(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("avg", calcAvg(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("stddev", calcStdDev(data)));
    }

    private void writeEcREAL64(InstanceElement ieMeq, InstanceElement ieEc, DatHeader datHeader, String channelName,
            MappedByteBuffer sourceMbb, File targetFile, FileChannel targetChannel) throws AoException,
            ConvertException, IOException {
        // read meta info from DAT header
        int noOfRows = Integer.valueOf(datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_NO_OF_VALUES).trim());
        int fileOffset = Integer.valueOf(datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_FILE_OFFSET).trim());
        String method = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_METHOD);
        long targetStartOffset = targetChannel.position();

        // FloatBuffer sourceFb = sourceMbb.asFloatBuffer();
        ByteBuffer targetBb = ByteBuffer.allocate(noOfRows * 8);
        targetBb.order(ByteOrder.LITTLE_ENDIAN);
        DoubleBuffer targetFb = targetBb.asDoubleBuffer();
        double[] data = new double[noOfRows];

        // store method BLOCK
        if (method.equals("BLOCK")) {
            String chOffsetStr = datHeader.getChannelHeaderEntry(channelName, DatHeader.KEY_CHANNEL_OFFSET);
            int chOffset = Integer.valueOf(chOffsetStr.trim());
            for (int i = 0; i < noOfRows; i++) {
                int idx = (chOffset * i) + fileOffset - 1;
                data[i] = sourceMbb.getDouble(idx);
                targetFb.put(data[i]);
            }
        }
        // store method CHANNEL
        else if (method.equals("CHANNEL")) {
            for (int i = 0; i < noOfRows; i++) {
                int idx = fileOffset + i - 1;
                data[i] = sourceMbb.getDouble(idx * 8);
                targetFb.put(data[i]);
            }
        } else {
            throw new ConvertException("Store method not yet supported: " + method);
        }

        targetChannel.write(targetBb);

        // set external component values
        ieEc.setValue(ODSHelper.createLongNVU("length", noOfRows));
        ieEc.setValue(ODSHelper.createLongLongNVU("start_offset", targetStartOffset));
        ieEc.setValue(ODSHelper.createLongNVU("valperblock", 1));
        ieEc.setValue(ODSHelper.createLongNVU("valoffset", 0));
        ieEc.setValue(ODSHelper.createEnumNVU("type", 6));
        ieEc.setValue(ODSHelper.createLongNVU("blocksize", 4));
        ieEc.setValue(ODSHelper.createStringNVU("filename_url", targetFile.getAbsolutePath()));

        // calculate min/max/avg/dev and update measurement quantity instance
        ieMeq.setValue(ODSHelper.createDoubleNVU("min", calcMin(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("max", calcMax(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("avg", calcAvg(data)));
        ieMeq.setValue(ODSHelper.createDoubleNVU("stddev", calcStdDev(data)));
    }

    /****************************************************************************************************
     * utility methods
     ****************************************************************************************************/

    private String asODSdate(String dateStr, String timeStr) {
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

    private static Double calcMin(float[] numbers) {
        if (numbers.length > 0) {
            double minValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] < minValue) {
                    minValue = numbers[i];
                }
            }
            return minValue;
        }
        return null;
    }

    private static Double calcMin(double[] numbers) {
        if (numbers.length > 0) {
            double minValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] < minValue) {
                    minValue = numbers[i];
                }
            }
            return minValue;
        }
        return null;
    }

    private static Double calcMin(int[] numbers) {
        if (numbers.length > 0) {
            double minValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] < minValue) {
                    minValue = numbers[i];
                }
            }
            return minValue;
        }
        return null;
    }

    private static Double calcMax(float[] numbers) {
        if (numbers.length > 0) {
            double maxValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] > maxValue) {
                    maxValue = numbers[i];
                }
            }
            return maxValue;
        }
        return null;
    }

    private static Double calcMax(double[] numbers) {
        if (numbers.length > 0) {
            double maxValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] > maxValue) {
                    maxValue = numbers[i];
                }
            }
            return maxValue;
        }
        return null;
    }

    private static Double calcMax(int[] numbers) {
        if (numbers.length > 0) {
            double maxValue = numbers[0];
            for (int i = 1; i < numbers.length; i++) {
                if (numbers[i] > maxValue) {
                    maxValue = numbers[i];
                }
            }
            return maxValue;
        }
        return null;
    }

    public static double calcAvg(float[] p) {
        double sum = 0;
        for (int i = 0; i < p.length; i++) {
            sum += p[i];
        }
        return sum / p.length;
    }

    public static double calcAvg(double[] p) {
        double sum = 0;
        for (int i = 0; i < p.length; i++) {
            sum += p[i];
        }
        return sum / p.length;
    }

    public static double calcAvg(int[] p) {
        double sum = 0;
        for (int i = 0; i < p.length; i++) {
            sum += p[i];
        }
        return sum / p.length;
    }

    private static double calcStdDev(float[] data) {
        double mean = 0;
        final int n = data.length;
        if (n < 2) {
            return Double.NaN;
        }
        for (int i = 0; i < n; i++) {
            mean += data[i];
        }
        mean /= n;
        // calculate the sum of squares
        double sum = 0;
        for (int i = 0; i < n; i++) {
            final double v = data[i] - mean;
            sum += v * v;
        }
        // Change to ( n - 1 ) to n if you have complete data instead of a sample.
        return Math.sqrt(sum / (n - 1));
    }

    private static double calcStdDev(double[] data) {
        double mean = 0;
        final int n = data.length;
        if (n < 2) {
            return Double.NaN;
        }
        for (int i = 0; i < n; i++) {
            mean += data[i];
        }
        mean /= n;
        // calculate the sum of squares
        double sum = 0;
        for (int i = 0; i < n; i++) {
            final double v = data[i] - mean;
            sum += v * v;
        }
        // Change to ( n - 1 ) to n if you have complete data instead of a sample.
        return Math.sqrt(sum / (n - 1));
    }

    private static double calcStdDev(int[] data) {
        double mean = 0;
        final int n = data.length;
        if (n < 2) {
            return Double.NaN;
        }
        for (int i = 0; i < n; i++) {
            mean += data[i];
        }
        mean /= n;
        // calculate the sum of squares
        double sum = 0;
        for (int i = 0; i < n; i++) {
            final double v = data[i] - mean;
            sum += v * v;
        }
        // Change to ( n - 1 ) to n if you have complete data instead of a sample.
        return Math.sqrt(sum / (n - 1));
    }

}
