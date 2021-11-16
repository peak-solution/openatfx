package de.rechner.openatfx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.RelationRange;
import org.asam.ods.TS_ValueSeq;
import org.asam.ods.T_LONGLONG;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import de.rechner.openatfx.util.ODSHelper;


public class AtfxCacheTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testGetRelatedInstanceIds() throws Exception {
        long aid = 5;
        List<Long> instanceIds = Arrays.asList(6L, 3L, 5L, 4L, 7L);
        
        AtfxCache atfxCache = new AtfxCache(null);
        ApplicationElement applElem = Mockito.mock(ApplicationElement.class);
        Mockito.when(applElem.getId()).thenReturn(new T_LONGLONG(0, 5));
        atfxCache.addApplicationElement(aid, "AoAny", applElem);
        for (Long iid : instanceIds)
        {
            atfxCache.addInstance(aid, iid);
        }
        
        ApplicationRelation applRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(applRel.getRelationName()).thenReturn("testRelation");
        Mockito.when(applRel.getRelationRange()).thenReturn(new RelationRange((short)0, (short)1));
        ApplicationRelation invApplRel = Mockito.mock(ApplicationRelation.class);
        Mockito.when(invApplRel.getRelationName()).thenReturn("invTestRelation");
        Mockito.when(invApplRel.getRelationRange()).thenReturn(new RelationRange((short)0, (short)-1));
        Mockito.when(invApplRel.getElem1()).thenReturn(applElem);
        atfxCache.addApplicationRelation(applRel, invApplRel);
        
        atfxCache.createInstanceRelations(aid, 5L, applRel, Arrays.asList(42L));
        atfxCache.createInstanceRelations(aid, 7L, applRel, Arrays.asList(43L));
        
        List<Long> queryIids = new ArrayList<>();
        queryIids.addAll(instanceIds);
        TS_ValueSeq valueSeq = atfxCache.getRelatedInstanceIds(aid, queryIids, applRel);
        
        assertThat(valueSeq.flag).containsExactly((short)0, (short)0, (short)15, (short)0, (short)15);
        assertThat(ODSHelper.asJLong(valueSeq.u.longlongVal())).containsExactly(0, 0, 42, 0, 43);
    }
}
