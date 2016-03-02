package de.rechner.openatfx_mdf;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.BasicConfigurator;
import org.omg.CORBA.ORB;


public class ConvertMain {

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            ORB orb = ORB.init(new String[0], System.getProperties());
            Path mdfFile = Paths.get("D:/PUBLIC/test/Wimmerversuch.mdf");

            MDFConverter converter = new MDFConverter();
            converter.writeATFXHeader(orb, mdfFile);
        } catch (ConvertException e) {
            System.err.println(e.getMessage());
        }
    }

}
