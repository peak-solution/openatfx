package de.rechner.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import junit.framework.JUnit4TestAdapter;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.Blob;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;


/**
 * Test case for <code>de.rechner.openatfx.util.atfx.BlobImpl</code>.
 * 
 * @author Christian Rechner
 */
public class BlobImplTest {

    private static AoSession aoSession;
    private Blob blob;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = BlobImplTest.class.getResource("/de/rechner/openatfx/example_atfx.xml");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }

    @Before
    public void setUp() throws Exception {
        this.blob = aoSession.createBlob();
        this.blob.setHeader("header");
        this.blob.append(new byte[] { 1, 2, 3, 4, 5 });
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#getHeader()}.
     */
    @Test
    public void testGetHeader() {
        try {
            assertEquals("header", this.blob.getHeader());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#setHeader(java.lang.String)}.
     */
    @Test
    public void testSetHeader() {
        try {
            this.blob.setHeader("newHeader");
            assertEquals("newHeader", this.blob.getHeader());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#getLength()}.
     */
    @Test
    public void testGetLength() {
        try {
            assertEquals(5, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#set(byte[])}.
     */
    @Test
    public void testSet() {
        try {
            assertEquals(5, this.blob.getLength());
            this.blob.set(new byte[] { 1, 2, 3 });
            assertEquals(3, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#append(byte[])}.
     */
    @Test
    public void testAppend() {
        try {
            assertEquals(5, this.blob.getLength());
            this.blob.append(new byte[] { 1, 2, 3 });
            assertEquals(8, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#get(int, int)}.
     */
    @Test
    public void testGet() {
        try {
            assertEquals(true, Arrays.equals(new byte[] {}, this.blob.get(0, 0)));
            assertEquals(true, Arrays.equals(new byte[] { 1, 2, 3, 4, 5 }, this.blob.get(0, 5)));
            assertEquals(true, Arrays.equals(new byte[] { 3 }, this.blob.get(2, 1)));
            assertEquals(true, Arrays.equals(new byte[] { 5 }, this.blob.get(4, 1)));
            assertEquals(5, this.blob.get(0, 5).length);
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            this.blob.get(4, 2);
            fail("AoException expected");
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#compare(org.asam.ods.Blob)}.
     */
    @Test
    public void testCompare() {
        try {
            Blob comp = aoSession.createBlob();
            comp.setHeader("header");
            comp.append(new byte[] { 1, 2, 3, 4, 5 });
            assertEquals(true, this.blob.compare(comp));
            comp.setHeader("headerNew");
            assertEquals(true, this.blob.compare(comp));
            comp.setHeader("header");
            comp.append(new byte[] { 1, 2, 3, 4, 5 });
            assertEquals(false, this.blob.compare(comp));
        } catch (AoException e) {
        }
    }

    /**
     * Test method for {@link de.rechner.openatfx.util.atfx.BlobImpl#destroy()}.
     */
    @Test
    public void testDestroy() {
        try {
            this.blob.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
        try {
            this.blob.getHeader();
            fail("Throwable expected");
        } catch (SystemException e) {
        } catch (AoException e) {
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(BlobImplTest.class);
    }

}
