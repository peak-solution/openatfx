package de.rechner.openatfx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.io.AtfxReader;

public class ExtCompReaderTest {

    private static AoSessionImpl aoSession;
    private static AtfxCache atfxCache;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AtfxReader reader = AtfxReader.getInstance();
        IFileHandler fileHandler = new LocalFileHandler();
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ExtCompReaderTest.class.getResource("/de/rechner/openatfx/example.atfx");
        String path = new File(url.getFile()).getAbsolutePath();
        aoSession = reader.createSessionImplForATFX(orb, fileHandler, path);
        atfxCache = ((AoSessionImpl)aoSession).getAtfxCache();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        aoSession.close();
    }
    
    @Test
    public void testReadNumberValues_signedByte() throws Exception {
        ExtCompReader reader = new ExtCompReader();
        List<Number> numbers = reader.readNumberValues(atfxCache, 116);
        assertThat(numbers).containsExactly((byte)1, (byte)0, (byte)-1, (byte)126, (byte)127, (byte)-127, (byte)-128, (byte)42, (byte)-13, (byte)-111);
    }

    @Test
    public void testReadNumberValues_unsignedByte() throws Exception {
        ExtCompReader reader = new ExtCompReader();
        List<Number> numbers = reader.readNumberValues(atfxCache, 119);
        int[] widenedNumbers = numbers.stream().mapToInt(b -> b.intValue()).toArray();
        assertThat(widenedNumbers).containsExactly(1, 0, 127, 128, 129, 254, 255, 42, 13, 111);
    }
}
