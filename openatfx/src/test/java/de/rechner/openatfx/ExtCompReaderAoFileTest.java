package de.rechner.openatfx;


import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.io.AtfxReader;
import org.asam.ods.AoException;
import org.asam.ods.TS_Value;
import static org.junit.Assert.assertEquals;

public class ExtCompReaderAoFileTest {

    private static AoSessionImpl aoSession;
    private static AtfxCache atfxCache;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AtfxReader reader = AtfxReader.getInstance();
        IFileHandler fileHandler = new LocalFileHandler();
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ExtCompReaderTest.class.getResource("/de/rechner/openatfx/external_with_flags_aofile.atfx");
        String path = new File(url.getFile()).getAbsolutePath();
        aoSession = reader.createSessionImplForATFX(orb, fileHandler, path);
        atfxCache = ((AoSessionImpl) aoSession).getAtfxCache();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }
    
    @Test
    public void testReadValues() throws AoException {
        ExtCompReader reader = new ExtCompReader();
        List<Number> numbers= reader.readNumberValues(atfxCache, 1); // LC.Name=51900778_1
        
        // check length
        assertEquals(300004, numbers.size());

        // check first values
        assertEquals(-0.006515602349889372d, (double)numbers.get(0), 0.00000000000000001);
        assertEquals(-0.006515602349889372d, (double)numbers.get(1), 0.00000000000000001);
        assertEquals(Double.NaN, (double)numbers.get(2), 0.00000000000000001);

        // check last values
        assertEquals(-0.006576638437476158d, (double)numbers.get(300001), 0.00000000000000001);
        assertEquals(-0.006576638437476158d, (double)numbers.get(300002), 0.00000000000000001);
        assertEquals(Double.NaN, (double)numbers.get(300003), 0.00000000000000001);
    }

    @Test
    public void testReadFlags() throws AoException {
        ExtCompReader reader = new ExtCompReader();
        TS_Value value = reader.readFlags(atfxCache, 11); // LC.Name=51900778_1
        short[] flags = value.u.shortSeq();
        
        // check length
        assertEquals(300004, flags.length);

        // check first flags
        assertEquals((short) 15, flags[0]);
        assertEquals((short) 15, flags[1]);
        assertEquals((short) 0, flags[2]);
        assertEquals((short) 15, flags[3]);

        // check last flags
        assertEquals((short) 0, flags[300000]);
        assertEquals((short) 15, flags[300001]);
        assertEquals((short) 15, flags[300002]);
        assertEquals((short) 0, flags[300003]);
    }
}
