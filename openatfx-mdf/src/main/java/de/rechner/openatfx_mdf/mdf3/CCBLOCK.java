package de.rechner.openatfx_mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;


/**
 * <p>
 * Channel conversion block: Description of a conversion formula for a channel
 * </p>
 * The data records are used to store implementation values. The CCBLOCK serves to specify a conversion formula that can
 * be used to convert these values into physical values with physical units.
 * 
 * @author Christian Rechner
 */
class CCBLOCK extends BLOCK {

    public static String BLOCK_ID = "CC";

    // BOOL 1 Value range – known physical value
    private boolean knownPhysValue;

    // REAL 1 Value range – minimum physical value
    private double minPhysValue;

    // REAL 1 Value range – maximum physical value
    private double maxPhysValue;

    // CHAR 20 Physical unit
    private String physUnit;

    // UINT16 1 Conversion formula identifier
    // 0 = parametric, linear
    // 1 = tabular with interpolation
    // 2 = tabular
    // 6 = polynomial function
    // 7 = exponential function
    // 8 = logarithmic function
    // 9 = ASAP2 Rational conversion formula
    // 10 = ASAM-MCD2 Text formula
    // 11 = ASAM-MCD2 Text Table, (COMPU_VTAB)
    // 12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)
    // 132 = Date (Based on 7 Byte Date data structure)
    // 133 = time (Based on 6 Byte Time data structure)
    // 65535 = 1:1 conversion formula (Int = Phys)
    private int formulaIdent;

    // UINT16 1 Number of value pairs for conversion formulas 1, 2, 11 and 12 or number of
    // parameters
    private int noOfValuePairsForFormula;

    // ... Parameter (for type 0,6,7,8,9) or table (for type 1, 2, 11, or 12) or text (for type
    // 10), depending on the conversion formula identifier. See formula-specific block
    // supplement.
    private double[] valuePairsForFormula; // formula = 0,6,7,8,9

    private double[] keysForTextTable; // formula = 11
    private String[] valuesForTextTable; // formula = 11

    private String defaultTextForTextRangeTable; // formula = 12
    private double[] lowerRangeKeysForTextRangeTable; // formula = 12
    private double[] upperRangeKeysForTextRangeTable; // formula = 12
    private String[] valuesForTextRangeTable; // formula = 12

    /**
     * Constructor.
     * 
     * @param sbc The byte channel pointing to the MDF file.
     * @param pos The position of the block within the MDF file.
     */
    private CCBLOCK(SeekableByteChannel sbc, long pos) {
        super(sbc, pos);
    }

    public boolean isKnownPhysValue() {
        return knownPhysValue;
    }

    private void setKnownPhysValue(boolean knownPhysValue) {
        this.knownPhysValue = knownPhysValue;
    }

    public double getMinPhysValue() {
        return minPhysValue;
    }

    private void setMinPhysValue(double minPhysValue) {
        this.minPhysValue = minPhysValue;
    }

    public double getMaxPhysValue() {
        return maxPhysValue;
    }

    private void setMaxPhysValue(double maxPhysValue) {
        this.maxPhysValue = maxPhysValue;
    }

    public String getPhysUnit() {
        return physUnit;
    }

    private void setPhysUnit(String physUnit) {
        this.physUnit = physUnit;
    }

    public int getFormulaIdent() {
        return formulaIdent;
    }

    private void setFormulaIdent(int formulaIdent) {
        this.formulaIdent = formulaIdent;
    }

    public int getNoOfValuePairsForFormula() {
        return noOfValuePairsForFormula;
    }

    private void setNoOfValuePairsForFormula(int noOfValuePairsForFormula) {
        this.noOfValuePairsForFormula = noOfValuePairsForFormula;
    }

    public double[] getValuePairsForFormula() {
        return valuePairsForFormula;
    }

    private void setValuePairsForFormula(double[] valuePairsForFormula) {
        this.valuePairsForFormula = valuePairsForFormula;
    }

    public double[] getKeysForTextTable() {
        return keysForTextTable;
    }

    private void setKeysForTextTable(double[] keysForTextTable) {
        this.keysForTextTable = keysForTextTable;
    }

    public String[] getValuesForTextTable() {
        return valuesForTextTable;
    }

    private void setValuesForTextTable(String[] valuesForTextTable) {
        this.valuesForTextTable = valuesForTextTable;
    }

    public String getDefaultTextForTextRangeTable() {
        return defaultTextForTextRangeTable;
    }

    private void setDefaultTextForTextRangeTable(String defaultTextForTextRangeTable) {
        this.defaultTextForTextRangeTable = defaultTextForTextRangeTable;
    }

    public double[] getLowerRangeKeysForTextRangeTable() {
        return lowerRangeKeysForTextRangeTable;
    }

    private void setLowerRangeKeysForTextRangeTable(double[] lowerRangeKeysForTextRangeTable) {
        this.lowerRangeKeysForTextRangeTable = lowerRangeKeysForTextRangeTable;
    }

    public double[] getUpperRangeKeysForTextRangeTable() {
        return upperRangeKeysForTextRangeTable;
    }

    private void setUpperRangeKeysForTextRangeTable(double[] upperRangeKeysForTextRangeTable) {
        this.upperRangeKeysForTextRangeTable = upperRangeKeysForTextRangeTable;
    }

    public String[] getValuesForTextRangeTable() {
        return valuesForTextRangeTable;
    }

    private void setValuesForTextRangeTable(String[] valuesForTextRangeTable) {
        this.valuesForTextRangeTable = valuesForTextRangeTable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rechner.openatfx_mdf4.mdf3.BLOCK#toString()
     */
    @Override
    public String toString() {
        return "CCBLOCK [knownPhysValue=" + knownPhysValue + ", minPhysValue=" + minPhysValue + ", maxPhysValue="
                + maxPhysValue + ", physUnit=" + physUnit + ", formulaIdent=" + formulaIdent
                + ", noOfValuePairsForFormula=" + noOfValuePairsForFormula + ", valuePairsForFormula="
                + Arrays.toString(valuePairsForFormula) + ", keysForTextTable=" + Arrays.toString(keysForTextTable)
                + ", valuesForTextTable=" + Arrays.toString(valuesForTextTable) + ", defaultTextForTextRangeTable="
                + defaultTextForTextRangeTable + ", lowerRangeKeysForTextRangeTable="
                + Arrays.toString(lowerRangeKeysForTextRangeTable) + ", upperRangeKeysForTextRangeTable="
                + Arrays.toString(upperRangeKeysForTextRangeTable) + ", valuesForTextRangeTable="
                + Arrays.toString(valuesForTextRangeTable) + "]";
    }

    /**
     * Reads a CCBLOCK from the channel starting at pos
     * 
     * @param sbc The channel to read from.
     * @param pos The position to start reading.
     * @return The block data.
     * @throws IOException The exception.
     */
    public static CCBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
        CCBLOCK block = new CCBLOCK(sbc, pos);

        // read block
        ByteBuffer bb = ByteBuffer.allocate(46);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        sbc.position(pos);
        sbc.read(bb);
        bb.rewind();

        // CHAR 2 Block type identifier
        block.setId(Mdf3Util.readChars(bb, 2));
        if (!block.getId().equals(BLOCK_ID)) {
            throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
        }

        // UINT16 1 Block size of this block in bytes
        block.setLength(Mdf3Util.readUInt16(bb));

        // BOOL 1 Value range – known physical value
        block.setKnownPhysValue(Mdf3Util.readBool(bb));

        // REAL 1 Value range – minimum physical value
        block.setMinPhysValue(Mdf3Util.readReal(bb));

        // REAL 1 Value range – maximum physical value
        block.setMaxPhysValue(Mdf3Util.readReal(bb));

        // CHAR 20 Physical unit
        block.setPhysUnit(Mdf3Util.readChars(bb, 20));

        // UINT16 1 Conversion formula identifier
        // 0 = parametric, linear
        // 1 = tabular with interpolation
        // 2 = tabular
        // 6 = polynomial function
        // 7 = exponential function
        // 8 = logarithmic function
        // 9 = ASAP2 Rational conversion formula
        // 10 = ASAM-MCD2 Text formula
        // 11 = ASAM-MCD2 Text Table, (COMPU_VTAB)
        // 12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)
        // 132 = Date (Based on 7 Byte Date data structure)
        // 133 = time (Based on 6 Byte Time data structure)
        // 65535 = 1:1 conversion formula (Int = Phys)
        block.setFormulaIdent(Mdf3Util.readUInt16(bb));

        // UINT16 1 Number of value pairs for conversion formulas 1, 2, 11 and 12 or number of parameters
        block.setNoOfValuePairsForFormula(Mdf3Util.readUInt16(bb));

        int formula = block.getFormulaIdent();
        if ((formula == 0) || formula == 1 || (formula == 6) || (formula == 7) || (formula == 8) || (formula == 9)) {

            // read block
            bb = ByteBuffer.allocate(block.getNoOfValuePairsForFormula() * 8);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            sbc.position(pos + 46);
            sbc.read(bb);
            bb.rewind();

            double[] d = new double[block.getNoOfValuePairsForFormula()];
            for (int i = 0; i < block.getNoOfValuePairsForFormula(); i++) {
                d[i] = Mdf3Util.readReal(bb);
            }
            block.setValuePairsForFormula(d);
        }

        else if (formula == 11) {

            // read block
            bb = ByteBuffer.allocate(block.getNoOfValuePairsForFormula() * 40);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            sbc.position(pos + 46);
            sbc.read(bb);
            bb.rewind();

            double[] keys = new double[block.getNoOfValuePairsForFormula()];
            String[] values = new String[block.getNoOfValuePairsForFormula()];
            for (int i = 0; i < block.getNoOfValuePairsForFormula(); i++) {
                keys[i] = Mdf3Util.readReal(bb);
                values[i] = Mdf3Util.readChars(bb, 32);
            }

            block.setKeysForTextTable(keys);
            block.setValuesForTextTable(values);
        }

        else if (formula == 12) {

            // read block
            bb = ByteBuffer.allocate(block.getNoOfValuePairsForFormula() * 20);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            sbc.position(pos + 46);
            sbc.read(bb);
            bb.rewind();

            // REAL 1 Undefined (to be ignored)
            Mdf3Util.readReal(bb);
            // REAL 1 Undefined (to be ignored)
            Mdf3Util.readReal(bb);
            // LINK 1 Pointer to TXBLOCK that contains the DEFAULT text
            long lnkTxBlock = Mdf3Util.readLink(bb);
            if (lnkTxBlock > 0) {
                block.setDefaultTextForTextRangeTable(TXBLOCK.read(sbc, lnkTxBlock).getText());
            }
            double[] lowerRangeKeysForTextRangeTable = new double[block.getNoOfValuePairsForFormula() - 1];
            double[] upperRangeKeysForTextRangeTable = new double[block.getNoOfValuePairsForFormula() - 1];
            String[] valuesForTextRangeTable = new String[block.getNoOfValuePairsForFormula() - 1];
            for (int i = 0; i < block.getNoOfValuePairsForFormula() - 1; i++) {
                lowerRangeKeysForTextRangeTable[i] = Mdf3Util.readReal(bb);
                upperRangeKeysForTextRangeTable[i] = Mdf3Util.readReal(bb);
                long lnkValue = Mdf3Util.readLink(bb);
                if (lnkValue > 0) {
                    valuesForTextRangeTable[i] = TXBLOCK.read(sbc, lnkValue).getText();
                }
            }
            block.setLowerRangeKeysForTextRangeTable(lowerRangeKeysForTextRangeTable);
            block.setUpperRangeKeysForTextRangeTable(upperRangeKeysForTextRangeTable);
            block.setValuesForTextRangeTable(valuesForTextRangeTable);
        }

        return block;
    }

}
