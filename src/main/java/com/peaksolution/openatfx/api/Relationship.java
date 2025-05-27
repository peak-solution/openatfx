package com.peaksolution.openatfx.api;

public enum Relationship {
    FATHER,
    CHILD,
    INFO_TO,
    INFO_FROM,
    INFO_REL,
    SUPERTYPE,
    SUBTYPE,
    ALL_REL,
    UNKNOWN
    ;
    
    public static Relationship fromString(String key) {
        for (Relationship current : Relationship.values()) {
            if (current.toString().equalsIgnoreCase(key)) {
                return current;
            }
        }
        return UNKNOWN;
    }
}
