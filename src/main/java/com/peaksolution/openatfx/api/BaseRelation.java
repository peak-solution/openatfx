package com.peaksolution.openatfx.api;

import java.util.Collection;

import org.asam.ods.RelationRange;
import org.asam.ods.RelationType;


public interface BaseRelation {
    public BaseElement getElem1();

    public Collection<BaseElement> getElem2();

    public String getName();

    public String getInverseName();
    
    public String getInverseName(String relationTargetBaseName);
    
    boolean isMandatory();

    public RelationRange getRelationRange();

    public RelationRange getInverseRelationRange();

    public Relationship getRelationship();

    public Relationship getInverseRelationship();

    public RelationType getRelationType();
}
