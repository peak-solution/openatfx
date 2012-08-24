package de.rechner.openatfx.converter.digatron_csv;

import java.io.File;
import java.io.FileFilter;

import de.rechner.openatfx.util.PatternUtil;


class PatternFileFilter implements FileFilter {

    private final String pattern;

    public PatternFileFilter(String pattern) {
        this.pattern = pattern;
    }

    public boolean accept(File pathname) {
        return PatternUtil.nameFilterMatchCI(pathname.getName(), pattern);
    }

}
