package de.rechner.openatfx.converter;

import java.io.File;
import java.io.FileFilter;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;

import de.rechner.openatfx.converter.diadem_dat.Dat2AtfxConverter;


public class Main {

    public static void main(String[] args) throws ConvertException {
        BasicConfigurator.configure();

        File sourceDir = new File("D:/PUBLIC/TestData/dat/hvb/Batterie1");
        File[] datFiles = sourceDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".dat");
            }
        });

        IConverter converter = new Dat2AtfxConverter();
        converter.convert(datFiles, new File("D:/PUBLIC/target.atfx"), new Properties());
    }

}
