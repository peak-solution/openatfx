package de.rechner.openatfx.main;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.BasicConfigurator;

import de.rechner.openatfx.AoFactoryImplTest;
import de.rechner.openatfx.AoServiceFactoryTest;
import de.rechner.openatfx.AoSessionImplTest;
import de.rechner.openatfx.ApplicationAttributeImplTest;
import de.rechner.openatfx.ApplicationElementImplTest;
import de.rechner.openatfx.BlobImplTest;
import de.rechner.openatfx.EnumerationDefinitionImplTest;
import de.rechner.openatfx.InstanceElementImplTest;
import de.rechner.openatfx.NameIteratorImplTest;
import de.rechner.openatfx.NameValueIteratorImplTest;
import de.rechner.openatfx.basestructure.BaseAttributeImplTest;
import de.rechner.openatfx.basestructure.BaseElementImplTest;
import de.rechner.openatfx.basestructure.BaseEnumerationDefinitionImplTest;
import de.rechner.openatfx.basestructure.BaseRelationImplTest;
import de.rechner.openatfx.basestructure.BaseStructureImplTest;
import de.rechner.openatfx.io.AtfxParseUtilTest;
import de.rechner.openatfx.util.ODSHelperTest;


/**
 * Test suite to run all tests.
 * 
 * @author Christian Rechner
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTest(ODSHelperTest.suite());

        // base structure
        suite.addTest(BaseEnumerationDefinitionImplTest.suite());
        suite.addTest(BaseStructureImplTest.suite());
        suite.addTest(BaseElementImplTest.suite());
        suite.addTest(BaseAttributeImplTest.suite());
        suite.addTest(BaseRelationImplTest.suite());

        // atfx
        suite.addTest(AoFactoryImplTest.suite());
        suite.addTest(AoServiceFactoryTest.suite());
        suite.addTest(AoSessionImplTest.suite());
        // suite.addTest(ApplElemAccessImplTest.suite());
        suite.addTest(ApplicationAttributeImplTest.suite());
        suite.addTest(ApplicationElementImplTest.suite());
        // suite.addTest(ApplicationStructureImplTest.suite());
        suite.addTest(AtfxParseUtilTest.suite());
        suite.addTest(BlobImplTest.suite());
        suite.addTest(EnumerationDefinitionImplTest.suite());
        suite.addTest(InstanceElementImplTest.suite());
        suite.addTest(NameIteratorImplTest.suite());
        suite.addTest(NameValueIteratorImplTest.suite());

        return suite;
    }

}
