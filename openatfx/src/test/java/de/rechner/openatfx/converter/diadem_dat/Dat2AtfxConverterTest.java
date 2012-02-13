package de.rechner.openatfx.converter.diadem_dat;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rechner.openatfx.converter.ConvertException;
import de.rechner.openatfx.converter.ConverterFactory;
import de.rechner.openatfx.converter.IConverter;


/**
 * Test case for <code>de.rechner.openatfx.basestructure.BaseAttributeImpl</code>.
 * 
 * @author Christian Rechner
 */
public class Dat2AtfxConverterTest {

    private static IConverter dat2AtfxConverter;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dat2AtfxConverter = ConverterFactory.getInstance().createConverter("diadem_dat2atfx");
    }

    @Test
    public void testConvertFiles() {
        URL url = Dat2AtfxConverterTest.class.getResource("/de/rechner/openatfx/converter/diadem_dat/testdata.DAT");
        File sourceFile = new File(url.getFile());
        try {
            File targetFile = File.createTempFile("transfer", "atfx");
            dat2AtfxConverter.convertFiles(new File[] { sourceFile }, targetFile, new Properties());
        } catch (ConvertException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    // @Test
    public void testConvertDirectory() {
        URL url = Dat2AtfxConverterTest.class.getResource("/de/rechner/openatfx/converter/diadem_dat");
        File sourceDir = new File(url.getFile());
        File targetFile = new File("D:/PUBLIC/transfer.atfx");
        Properties props = new Properties();
        props.put("attachmentFilenamePattern", "*.erg");
        try {
            dat2AtfxConverter.convertDirectory(sourceDir, targetFile, props);
        } catch (ConvertException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testConvertBulk() {
        File sourceDir = new File("D:/PUBLIC/TestData/dat/hvb/20120207_pHev_Eingangsvermessung_2012028");
        File targetFile = new File("D:/PUBLIC/transfer.atfx");

        Properties props = new Properties();
        props.put("attachmentFilenamePattern", "*.erg");
        try {
            dat2AtfxConverter.convertDirectory(sourceDir, targetFile, props);
        } catch (ConvertException e) {
            fail(e.getMessage());
        }
    }

}
