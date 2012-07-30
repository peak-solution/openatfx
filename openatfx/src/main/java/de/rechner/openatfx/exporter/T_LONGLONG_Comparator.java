package de.rechner.openatfx.exporter;

import java.util.Comparator;

import org.asam.ods.T_LONGLONG;

import de.rechner.openatfx.util.ODSHelper;


class T_LONGLONG_Comparator implements Comparator<T_LONGLONG> {

    public int compare(T_LONGLONG o1, T_LONGLONG o2) {
        Long l1 = ODSHelper.asJLong(o1);
        Long l2 = ODSHelper.asJLong(o2);
        return l1.compareTo(l2);
    }

}
