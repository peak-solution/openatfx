package com.peaksolution.openatfx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.asam.ods.AoException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.api.ExtCompReader;
import com.peaksolution.openatfx.api.SingleValue;
import com.peaksolution.openatfx.api.corba.CorbaAtfxReader;

@ExtendWith(GlassfishCorbaExtension.class)
public class ExtCompReaderAoFileTest {

    private static CorbaAtfxReader atfxReader;

    @BeforeAll
    public static void setUpBeforeClass() throws URISyntaxException {
        ORB orb = ORB.init(new String[0], System.getProperties());
        try {
            URL url = ExtCompReaderAoFileTest.class.getResource("/com/peaksolution/openatfx/external_with_flags_aofile.atfx");
            atfxReader = new CorbaAtfxReader();
            IFileHandler fileHandler = new LocalFileHandler();
            Path path = Paths.get(url.toURI()).toAbsolutePath();
            atfxReader.init(orb, fileHandler, path);
        } catch (AoException e) {
            fail(e.reason);
        }
    }
    
    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (atfxReader.getSession() != null) {
            atfxReader.getSession().close();
        }
    }

    @Test
    void testReadValues() {
        ExtCompReader extCompReader = new ExtCompReader(atfxReader.getApi());
        List<Number> numbers = extCompReader.readNumberValues(1, ByteOrder.LITTLE_ENDIAN); // LC.Name=51900778_1

        // check length
        assertEquals(300004, numbers.size());

        // check first values
        assertEquals(-0.006515602349889372d, (double) numbers.get(0), 0.00000000000000001);
        assertEquals(-0.006515602349889372d, (double) numbers.get(1), 0.00000000000000001);
        assertEquals(Double.NaN, (double) numbers.get(2), 0.00000000000000001);

        // check last values
        assertEquals(-0.006576638437476158d, (double) numbers.get(300001), 0.00000000000000001);
        assertEquals(-0.006576638437476158d, (double) numbers.get(300002), 0.00000000000000001);
        assertEquals(Double.NaN, (double) numbers.get(300003), 0.00000000000000001);
    }

    @Test
    void testReadFlags() {
        ExtCompReader extCompReader = new ExtCompReader(atfxReader.getApi());
        SingleValue value = extCompReader.readFlags(11); // LC.Name=51900778_1
        short[] flags = value.shortSeq();

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
