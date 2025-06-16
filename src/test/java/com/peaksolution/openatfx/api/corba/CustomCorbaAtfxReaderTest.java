package com.peaksolution.openatfx.api.corba;

import static org.assertj.core.api.Assertions.assertThat;
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
      Path atfxFile = Paths.get("<filePath>");
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
          assertThat(iter.getCount()).isNotZero();
      } catch (AoException e) {
          fail(e.reason);
      }
  }
}
