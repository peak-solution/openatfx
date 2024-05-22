package de.rechner.openatfx.basestructure;

import org.asam.ods.AoException;
import org.asam.ods.BaseStructure;
import org.omg.CORBA.ORB;

public interface BaseModelReader {
    BaseStructure getBaseModel(ORB orb, String baseModelVersion) throws AoException;
}
