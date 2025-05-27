package com.peaksolution.openatfx.api.corba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplElem;
import org.asam.ods.ApplRel;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.ApplicationStructureValue;
import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;
import org.asam.ods.Relationship;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.CORBA.ORB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peaksolution.openatfx.AoServiceFactory;
import com.peaksolution.openatfx.GlassfishCorbaExtension;
import com.peaksolution.openatfx.util.ODSHelper;


/**
 * Test case for <code>com.peaksolution.openatfx.ApplicationRelationImpl</code>.
 * 
 * @author Christian Rechner
 */
@ExtendWith(GlassfishCorbaExtension.class)
public class ApplicationRelationImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRelationImplTest.class);
    
    private static AoSession aoSession;
    private static ApplicationRelation baseRel;
    private static ApplicationRelation infoRel;
    private static ApplicationRelation m2nRel;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        ORB orb = ORB.init(new String[0], System.getProperties());
        URL url = ApplicationAttributeImpl.class.getResource("/com/peaksolution/openatfx/example.atfx");
        try {
            aoSession = AoServiceFactory.getInstance().newAoFactory(orb).newSession("FILENAME=" + new File(url.getFile()));
    
            ApplicationStructure as = aoSession.getApplicationStructure();
            ApplicationElement aeGeo = as.getElementByName("geometry");
            ApplicationElement aeMea = as.getElementByName("mea");
            ApplicationElement aeDts = as.getElementByName("dts");
            ApplicationElement aePas = as.getElementByName("pas");
            baseRel = as.getRelations(aeMea, aeDts)[0];
            infoRel = as.getRelations(aeGeo, aeDts)[0];
            m2nRel = as.getRelations(aeDts, aePas)[0];
        } catch (AoException ex) {
            LOG.error(ex.reason, ex);
            throw ex;
        }
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        if (aoSession != null) {
            aoSession.close();
        }
    }
    
    @Test
    void testInverseBaseRelationNameForSubTestMeasurementRelation() throws AoException {
        long meaId = 0;
        long dtsId = 0;
        ApplicationStructureValue asv = aoSession.getApplicationStructureValue();
        for (ApplElem elem : asv.applElems) {
            if (elem.aeName.equals("mea")) {
                meaId = ODSHelper.asJLong(elem.aid);
            } else if (elem.aeName.equals("dts")) {
                dtsId = ODSHelper.asJLong(elem.aid);
            }
        }
        
        for (ApplRel rel : asv.applRels) {
            if (meaId == ODSHelper.asJLong(rel.elem1) && dtsId == ODSHelper.asJLong(rel.elem2)) {
                assertThat(rel.brName).isEqualTo("children");
                assertThat(rel.invBrName).isEqualTo("test");
            }
        }
    }

    @Test
    void testGetBaseRelation() {
        try {
            assertThat(baseRel.getBaseRelation().getRelationName()).isEqualTo("children");
            assertThat(infoRel.getBaseRelation()).isNull();
            assertThat(m2nRel.getBaseRelation()).isNull();
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetElem1() {
        try {
            assertThat(baseRel.getElem1().getName()).isEqualTo("mea");
            assertThat(infoRel.getElem1().getName()).isEqualTo("geometry");
            assertThat(m2nRel.getElem1().getName()).isEqualTo("dts");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetElem2() {
        try {
            assertThat(baseRel.getElem2().getName()).isEqualTo("dts");
            assertThat(infoRel.getElem2().getName()).isEqualTo("dts");
            assertThat(m2nRel.getElem2().getName()).isEqualTo("pas");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetRelationName() {
        try {
            assertThat(baseRel.getRelationName()).isEqualTo("dts_iid");
            assertThat(infoRel.getRelationName()).isEqualTo("measurements_on_geometry");
            assertThat(m2nRel.getRelationName()).isEqualTo("pas_iid");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetInverseRelationName() {
        try {
            assertThat(baseRel.getInverseRelationName()).isEqualTo("mea_iid");
            assertThat(infoRel.getInverseRelationName()).isEqualTo("geometry");
            assertThat(m2nRel.getInverseRelationName()).isEqualTo("dts_iid");
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetRelationRange() {
        try {
            RelationRange baseRelRange = baseRel.getRelationRange();
            assertThat(baseRelRange.min).isZero();
            assertThat(baseRelRange.max).isEqualTo((short)-1);

            RelationRange infoRelRange = infoRel.getRelationRange();
            assertThat(infoRelRange.min).isZero();
            assertThat(infoRelRange.max).isEqualTo((short)-1);

            RelationRange m2nRelRange = m2nRel.getRelationRange();
            assertThat(m2nRelRange.min).isZero();
            assertThat(m2nRelRange.max).isEqualTo((short)-1);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetInverseRelationRange() {
        try {
            RelationRange baseRelRange = baseRel.getInverseRelationRange();
            assertThat(baseRelRange.min).isEqualTo((short)1);
            assertThat(baseRelRange.max);

            RelationRange infoRelRange = infoRel.getInverseRelationRange();
            assertThat(infoRelRange.min).isZero();
            assertThat(infoRelRange.max).isEqualTo((short)1);

            RelationRange m2nRelRange = m2nRel.getInverseRelationRange();
            assertThat(m2nRelRange.min).isZero();
            assertThat(m2nRelRange.max).isEqualTo((short)-1);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetRelationship() {
        try {
            assertThat(baseRel.getRelationship()).isEqualTo(Relationship.CHILD);
            assertThat(ODSHelper.relationship2string(infoRel.getRelationship())).isEqualTo("INFO_FROM");
            assertThat(m2nRel.getRelationship()).isEqualTo(Relationship.INFO_REL);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetInverseRelationship() {
        try {
            assertThat(baseRel.getInverseRelationship()).isEqualTo(Relationship.FATHER);
            assertThat(infoRel.getInverseRelationship()).isEqualTo(Relationship.INFO_TO);
            assertThat(m2nRel.getInverseRelationship()).isEqualTo(Relationship.INFO_REL);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testGetRelationType() {
        try {
            assertThat(baseRel.getRelationType()).isEqualTo(RelationType.FATHER_CHILD);
            assertThat(infoRel.getRelationType()).isEqualTo(RelationType.INFO);
            assertThat(m2nRel.getRelationType()).isEqualTo(RelationType.INFO);
        } catch (AoException e) {
            fail(e.reason);
        }
    }

    @Test
    void testSetBaseRelation() {
    }

    @Test
    void testSetElem1() {
    }

    @Test
    void testSetElem2() {
    }

    @Test
    void testSetRelationName() {
    }

    @Test
    void testSetInverseRelationName() {
    }

    @Test
    void testSetRelationRange() {
    }

    @Test
    void testSetInverseRelationRange() {
    }

    @Test
    void testSetRelationType() {
    }
}
