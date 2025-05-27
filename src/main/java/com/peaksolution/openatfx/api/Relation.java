package com.peaksolution.openatfx.api;

import org.asam.ods.RelationType;


public interface Relation {

    int getRelNo();
    
    public Relation getInverseRelation();

    void setBaseRelation(BaseRelation baseRelation);

    public BaseRelation getBaseRelation();

    String getBaseName();
    
    public String getRelationName();

    void setElement1(Element element1);

    public Element getElement1();

    void setElement2(Element element2);

    public Element getElement2();

    public String getInverseRelationName();

    public void setRelationRangeMin(short min);

    public short getRelationRangeMin();

    public void setRelationRangeMax(short max);

    public short getRelationRangeMax();

    public Relationship getRelationship();

    void setRelationType(RelationType relationType);
    
    public RelationType getRelationType();
}
