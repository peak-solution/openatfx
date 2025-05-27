package com.peaksolution.openatfx.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AtfxModelCache {
    private final Map<String, Set<Long>> beToAidMap = new HashMap<>();
    private final Map<Long, String> aidToAeNameMap = new HashMap<>();
    private final Map<Long, String> aidToBaseTypeMap = new HashMap<>();
    private final Map<String, Long> aeNameToAidMap = new HashMap<>();
    
    private final Map<Long, Map<String, Integer>> aaNameToAttrNoMap = new HashMap<>();
    private final Map<Long, Map<String, Integer>> baNameToAttrNoMap = new HashMap<>();
}
