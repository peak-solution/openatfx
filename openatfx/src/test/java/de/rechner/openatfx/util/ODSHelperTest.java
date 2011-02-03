package de.rechner.openatfx.util;

import static org.junit.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

import org.asam.ods.DataType;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.asam.ods.T_LONGLONG;
import org.junit.Test;


/**
 * Test case for <code>de.rechner.openatfx.util.ODSHelper</code>.
 * 
 * @author Christian Rechner
 */
public class ODSHelperTest {

    @Test
    public void testAsODSLongLongLong() {
        T_LONGLONG l1 = ODSHelper.asODSLongLong(Long.MIN_VALUE);
        assertEquals(0, l1.low);
        assertEquals(-2147483648, l1.high);

        l1 = ODSHelper.asODSLongLong(Integer.MIN_VALUE);
        assertEquals(-2147483648, l1.low);
        assertEquals(-1, l1.high);

        l1 = ODSHelper.asODSLongLong(Short.MIN_VALUE);
        assertEquals(-32768, l1.low);
        assertEquals(-1, l1.high);

        l1 = ODSHelper.asODSLongLong(0);
        assertEquals(0, l1.low);
        assertEquals(0, l1.high);

        l1 = ODSHelper.asODSLongLong(Short.MAX_VALUE);
        assertEquals(32767, l1.low);
        assertEquals(0, l1.high);

        l1 = ODSHelper.asODSLongLong(Integer.MAX_VALUE);
        assertEquals(2147483647, l1.low);
        assertEquals(0, l1.high);

        l1 = ODSHelper.asODSLongLong(Long.MAX_VALUE);
        assertEquals(-1, l1.low);
        assertEquals(2147483647, l1.high);

        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Long.MIN_VALUE)), Long.MIN_VALUE);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Integer.MIN_VALUE)), Integer.MIN_VALUE);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Short.MIN_VALUE)), Short.MIN_VALUE);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(0)), 0);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Short.MAX_VALUE)), Short.MAX_VALUE);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Integer.MAX_VALUE)), Integer.MAX_VALUE);
        assertEquals(ODSHelper.asJLong(ODSHelper.asODSLongLong(Long.MAX_VALUE)), Long.MAX_VALUE);
    }

    @Test
    public void testDataType2String() {
        assertEquals("DT_BLOB", ODSHelper.dataType2String(DataType.DT_BLOB));
        assertEquals("DT_BOOLEAN", ODSHelper.dataType2String(DataType.DT_BOOLEAN));
        assertEquals("DT_BYTE", ODSHelper.dataType2String(DataType.DT_BYTE));
        assertEquals("DT_BYTESTR", ODSHelper.dataType2String(DataType.DT_BYTESTR));
        assertEquals("DT_COMPLEX", ODSHelper.dataType2String(DataType.DT_COMPLEX));
        assertEquals("DT_DCOMPLEX", ODSHelper.dataType2String(DataType.DT_DCOMPLEX));
        assertEquals("DT_DOUBLE", ODSHelper.dataType2String(DataType.DT_DOUBLE));
        assertEquals("DT_ENUM", ODSHelper.dataType2String(DataType.DT_ENUM));
        assertEquals("DT_EXTERNALREFERENCE", ODSHelper.dataType2String(DataType.DT_EXTERNALREFERENCE));
        assertEquals("DT_ID", ODSHelper.dataType2String(DataType.DT_ID));
        assertEquals("DT_LONG", ODSHelper.dataType2String(DataType.DT_LONG));
        assertEquals("DT_SHORT", ODSHelper.dataType2String(DataType.DT_SHORT));
        assertEquals("DT_STRING", ODSHelper.dataType2String(DataType.DT_STRING));
        assertEquals("DT_UNKNOWN", ODSHelper.dataType2String(DataType.DT_UNKNOWN));

        assertEquals("DS_BOOLEAN", ODSHelper.dataType2String(DataType.DS_BOOLEAN));
        assertEquals("DS_BYTE", ODSHelper.dataType2String(DataType.DS_BYTE));
        assertEquals("DS_BYTESTR", ODSHelper.dataType2String(DataType.DS_BYTESTR));
        assertEquals("DS_COMPLEX", ODSHelper.dataType2String(DataType.DS_COMPLEX));
        assertEquals("DS_DATE", ODSHelper.dataType2String(DataType.DS_DATE));
        assertEquals("DS_DCOMPLEX", ODSHelper.dataType2String(DataType.DS_DCOMPLEX));
        assertEquals("DS_DOUBLE", ODSHelper.dataType2String(DataType.DS_DOUBLE));
        assertEquals("DS_ENUM", ODSHelper.dataType2String(DataType.DS_ENUM));
        assertEquals("DS_EXTERNALREFERENCE", ODSHelper.dataType2String(DataType.DS_EXTERNALREFERENCE));
        assertEquals("DS_FLOAT", ODSHelper.dataType2String(DataType.DS_FLOAT));
        assertEquals("DS_ID", ODSHelper.dataType2String(DataType.DS_ID));
        assertEquals("DS_LONG", ODSHelper.dataType2String(DataType.DS_LONG));
        assertEquals("DS_LONGLONG", ODSHelper.dataType2String(DataType.DS_LONGLONG));
        assertEquals("DS_SHORT", ODSHelper.dataType2String(DataType.DS_SHORT));
        assertEquals("DS_STRING", ODSHelper.dataType2String(DataType.DS_STRING));
    }

    @Test
    public void testString2dataType() {
        assertEquals(DataType.DT_BLOB, ODSHelper.string2dataType("DT_BLOB"));
        assertEquals(DataType.DT_BOOLEAN, ODSHelper.string2dataType("DT_BOOLEAN"));
        assertEquals(DataType.DT_BYTE, ODSHelper.string2dataType("DT_BYTE"));
        assertEquals(DataType.DT_BYTESTR, ODSHelper.string2dataType("DT_BYTESTR"));
        assertEquals(DataType.DT_COMPLEX, ODSHelper.string2dataType("DT_COMPLEX"));
        assertEquals(DataType.DT_DATE, ODSHelper.string2dataType("DT_DATE"));
        assertEquals(DataType.DT_DCOMPLEX, ODSHelper.string2dataType("DT_DCOMPLEX"));
        assertEquals(DataType.DT_DOUBLE, ODSHelper.string2dataType("DT_DOUBLE"));
        assertEquals(DataType.DT_ENUM, ODSHelper.string2dataType("DT_ENUM"));
        assertEquals(DataType.DT_EXTERNALREFERENCE, ODSHelper.string2dataType("DT_EXTERNALREFERENCE"));
        assertEquals(DataType.DT_FLOAT, ODSHelper.string2dataType("DT_FLOAT"));
        assertEquals(DataType.DT_ID, ODSHelper.string2dataType("DT_ID"));
        assertEquals(DataType.DT_LONG, ODSHelper.string2dataType("DT_LONG"));
        assertEquals(DataType.DT_LONGLONG, ODSHelper.string2dataType("DT_LONGLONG"));
        assertEquals(DataType.DT_SHORT, ODSHelper.string2dataType("DT_SHORT"));
        assertEquals(DataType.DT_STRING, ODSHelper.string2dataType("DT_STRING"));
        assertEquals(DataType.DT_UNKNOWN, ODSHelper.string2dataType("DT_UNKNOWN"));

        assertEquals(DataType.DS_BOOLEAN, ODSHelper.string2dataType("DS_BOOLEAN"));
        assertEquals(DataType.DS_BYTE, ODSHelper.string2dataType("DS_BYTE"));
        assertEquals(DataType.DS_BYTESTR, ODSHelper.string2dataType("DS_BYTESTR"));
        assertEquals(DataType.DS_COMPLEX, ODSHelper.string2dataType("DS_COMPLEX"));
        assertEquals(DataType.DS_DATE, ODSHelper.string2dataType("DS_DATE"));
        assertEquals(DataType.DS_DCOMPLEX, ODSHelper.string2dataType("DS_DCOMPLEX"));
        assertEquals(DataType.DS_DOUBLE, ODSHelper.string2dataType("DS_DOUBLE"));
        assertEquals(DataType.DS_ENUM, ODSHelper.string2dataType("DS_ENUM"));
        assertEquals(DataType.DS_EXTERNALREFERENCE, ODSHelper.string2dataType("DS_EXTERNALREFERENCE"));
        assertEquals(DataType.DS_FLOAT, ODSHelper.string2dataType("DS_FLOAT"));
        assertEquals(DataType.DS_ID, ODSHelper.string2dataType("DS_ID"));
        assertEquals(DataType.DS_LONG, ODSHelper.string2dataType("DS_LONG"));
        assertEquals(DataType.DS_LONGLONG, ODSHelper.string2dataType("DS_LONGLONG"));
        assertEquals(DataType.DS_SHORT, ODSHelper.string2dataType("DS_SHORT"));
        assertEquals(DataType.DS_STRING, ODSHelper.string2dataType("DS_STRING"));
    }

    @Test
    public void testRelRange2string() {
        assertEquals("0", ODSHelper.relRange2string((short) 0));
        assertEquals("1", ODSHelper.relRange2string((short) 1));
        assertEquals("Many", ODSHelper.relRange2string((short) -1));
    }

    @Test
    public void testString2relRange() {
        assertEquals((short) 0, ODSHelper.string2relRange("0"));
        assertEquals((short) 1, ODSHelper.string2relRange("1"));
        assertEquals((short) -1, ODSHelper.string2relRange("Many"));
    }

    @Test
    public void testRelationship2string() {
        assertEquals("ALL_REL", ODSHelper.relationship2string(Relationship.ALL_REL));
        assertEquals("CHILD", ODSHelper.relationship2string(Relationship.CHILD));
        assertEquals("FATHER", ODSHelper.relationship2string(Relationship.FATHER));
        assertEquals("INFO_FROM", ODSHelper.relationship2string(Relationship.INFO_FROM));
        assertEquals("INFO_REL", ODSHelper.relationship2string(Relationship.INFO_REL));
        assertEquals("INFO_TO", ODSHelper.relationship2string(Relationship.INFO_TO));
        assertEquals("SUBTYPE", ODSHelper.relationship2string(Relationship.SUBTYPE));
        assertEquals("SUPERTYPE", ODSHelper.relationship2string(Relationship.SUPERTYPE));
    }

    @Test
    public void testString2relationship() {
        assertEquals(Relationship.ALL_REL, ODSHelper.string2relationship("ALL_REL"));
        assertEquals(Relationship.CHILD, ODSHelper.string2relationship("CHILD"));
        assertEquals(Relationship.FATHER, ODSHelper.string2relationship("FATHER"));
        assertEquals(Relationship.INFO_FROM, ODSHelper.string2relationship("INFO_FROM"));
        assertEquals(Relationship.INFO_REL, ODSHelper.string2relationship("INFO_REL"));
        assertEquals(Relationship.INFO_TO, ODSHelper.string2relationship("INFO_TO"));
        assertEquals(Relationship.SUBTYPE, ODSHelper.string2relationship("SUBTYPE"));
        assertEquals(Relationship.SUPERTYPE, ODSHelper.string2relationship("SUPERTYPE"));
    }

    @Test
    public void testRelationType2string() {
        assertEquals("FATHER_CHILD", ODSHelper.relationType2string(RelationType.FATHER_CHILD));
        assertEquals("INFO", ODSHelper.relationType2string(RelationType.INFO));
        assertEquals("INHERITANCE", ODSHelper.relationType2string(RelationType.INHERITANCE));
    }

    @Test
    public void testString2relationType() {
        assertEquals(RelationType.FATHER_CHILD, ODSHelper.string2relationType("FATHER_CHILD"));
        assertEquals(RelationType.INFO, ODSHelper.string2relationType("INFO"));
        assertEquals(RelationType.INHERITANCE, ODSHelper.string2relationType("INHERITANCE"));
    }

    @Test
    public void testByteToHex() {
        assertEquals("12", ODSHelper.byteToHex((byte) 18));
        assertEquals("2A", ODSHelper.byteToHex((byte) 42));
        assertEquals("34", ODSHelper.byteToHex((byte) 52));
        assertEquals("DE", ODSHelper.byteToHex((byte) 222));
    }

    @Test
    public void testHexToByte() {
        assertEquals((byte) 18, ODSHelper.hexToByte("12"));
        assertEquals((byte) 42, ODSHelper.hexToByte("2A"));
        assertEquals((byte) 52, ODSHelper.hexToByte("34"));
        assertEquals((byte) 222, ODSHelper.hexToByte("DE"));
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ODSHelperTest.class);
    }

}
