package de.rechner.openatfx.converter.digatron_csv;

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
 * Test case for <code>de.rechner.openatfx.converter.maccor_csv.MaccorCSV2AtfxConverter</code>.
 * 
 * @author Christian Rechner
 */
public class DigatronCSV2AtfxConverterTest {

    private static IConverter converter;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        converter = ConverterFactory.getInstance().createConverter("digatron_csv2atfx");
    }

    @Test
    public void testConvertFiles() {
        URL url = DigatronCSV2AtfxConverterTest.class.getResource("/de/rechner/openatfx/converter/digatron_csv/digatron_testdata.csv");
        File sourceFile = new File(url.getFile());
        try {
            File targetFile = File.createTempFile("transfer", "atfx");
            converter.convertFiles(new File[] { sourceFile }, targetFile, new Properties());
        } catch (ConvertException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
