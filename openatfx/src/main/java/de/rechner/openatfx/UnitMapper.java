package de.rechner.openatfx;

/**
 * Interface for implementations that can map the unit name to its unit iid and vice versa.
 * Used for the AtfxCache.
 */
public interface UnitMapper {
    long getUnitId(String unitName);
    String getUnitString(long unitId);
}
