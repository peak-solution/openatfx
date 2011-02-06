package de.rechner.openatfx.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rechner.openatfx.util.ODSHelper;


/**
 * Test case for <code>de.rechner.openatfx.io.AtfxExportUtilTest</code>.
 * 
 * @author Christian Rechner
 */
public class AtfxExportUtilTest {

    @Test
    public void testCreateLongLongSeqString() {
        assertEquals("10 11 12 13",
                     AtfxExportUtil.createLongLongSeqString(ODSHelper.asODSLongLong(new long[] { 10, 11, 12, 13 })));
    }

}
