package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.Blob;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;


/**
 * Test case for <code>com.peaksolution.openatfx.api.corba.BlobImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class BlobImplTest {

    private static AoSession aoSession;
    private Blob blob;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = BlobImplTest.class.getResource("/com/peaksolution/openatfx/example.atfx");
        aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.blob = aoSession.createBlob();
        this.blob.setHeader("header");
        this.blob.append(new byte[] { 1, 2, 3, 4, 5 });
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#getHeader()}.
     */
    @Test
    void testGetHeader() {
        try {
            assertEquals("header", this.blob.getHeader());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#setHeader(java.lang.String)}.
     */
    @Test
    void testSetHeader() {
        try {
            this.blob.setHeader("newHeader");
            assertEquals("newHeader", this.blob.getHeader());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#getLength()}.
     */
    @Test
    void testGetLength() {
        try {
            assertEquals(5, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#set(byte[])}.
     */
    @Test
    void testSet() {
        try {
            assertEquals(5, this.blob.getLength());
            this.blob.set(new byte[] { 1, 2, 3 });
            assertEquals(3, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#append(byte[])}.
     */
    @Test
    void testAppend() {
        try {
            assertEquals(5, this.blob.getLength());
            this.blob.append(new byte[] { 1, 2, 3 });
            assertEquals(8, this.blob.getLength());
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    /**
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#get(int, int)}.
     */
    @Test
    void testGet() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#compare(org.asam.ods.Blob)}.
     */
    @Test
    void testCompare() {
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
     * Test method for {@link com.peaksolution.openatfx.api.corba.atfx.BlobImpl#destroy()}.
     */
    @Test
    void testDestroy() {
        try {
            this.blob.destroy();
        } catch (AoException e) {
            fail(e.reason);
        }
    }
}
