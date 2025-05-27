package com.peaksolution.openatfx.api.corba;

import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElementIterator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.api.OpenAtfxConstants;
import com.peaksolution.openatfx.util.ODSHelper;

@Disabled
@ExtendWith(GlassfishCorbaExtension.class)
public class CustomCorbaAtfxReaderTest {
    private static AoSession aoSession;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
      ORB orb = ORB.init(new String[0], System.getProperties());
      Path atfxFile = Paths.get("C:\\import\\bmw\\BMWMDM-1104\\EEX1_HBP_KE_25000026_EMI_20250124.475.atfx");
      aoSession = AoServiceFactory.getInstance().newAoSession(orb, atfxFile.toFile(), ODSHelper.createBooleanNV(OpenAtfxConstants.CONTEXT_TRIM_STRING_VALUES, false));
  }

  @AfterAll
  public static void tearDownAfterClass() throws Exception {
      if (aoSession != null) {
          aoSession.close();
      }
  }

  @Test
  void test() {
      try {
          ApplicationStructure applicationStructure = aoSession.getApplicationStructure();
          aoSession.getApplicationStructureValue();
          ApplicationElement element = applicationStructure.getElementByName("LocalColumn");
          ApplicationRelation[] rels = element.getRelationsByBaseName("measurement_quantity");
          InstanceElementIterator iter = element.getInstances("*");
          for (int i = 0; i < iter.getCount(); i++) {
              System.out.println("Element name: " + iter.nextOne().getValue("LMS_Actual_sensitivity").value.u.stringVal() + "Ende");
          }
      } catch (AoException e) {
          fail(e.reason);
      }
  }
}
