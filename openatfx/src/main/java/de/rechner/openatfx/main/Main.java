package de.rechner.openatfx.main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import de.rechner.openatfx.AoServiceFactory;


public class Main {

    private static final Log LOG = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();

            // configure ORB
            ORB orb = ORB.init(new String[0], System.getProperties());
            AoFactory aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            NameComponent path[] = ncRef.to_name("ATFX");
            ncRef.rebind(path, aoFactory);

            LOG.info("ATFX Server started");
            orb.run();
        } catch (InvalidName e) {
            System.err.println(e.getMessage());
        } catch (NotFound e) {
            System.err.println(e.getMessage());
        } catch (CannotProceed e) {
            System.err.println(e.getMessage());
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
            System.err.println(e.getMessage());
        } catch (AoException e) {
            System.err.println(e.reason);
        }
    }

    public static void main1(String[] args) {
        String input = "/[prj]no_project/[tstser]Test_Vorbeifahrt/[mea]Run_middEng_FINAL_RES/[dts]Detector\\;rms A fast - Zusammenfassung";
        Pattern pattern = Pattern.compile("/\\[([^]]*)\\]([^/;]*);?([^/]*)");
        String lookAround = "(?<!\\\\)/";

        /** /[XXX]YYY */

        Matcher m = pattern.matcher(input);
        while (m.find()) {
            System.out.println("---------------------------------");
            System.out.println(m.group(0));
            System.out.println("AE: " + m.group(1));
            System.out.println("IE: " + m.group(2));
            // System.out.println("VE: " + m.group(3));
        }
    }

    public static void main14(String[] args) {
        String input = "/[prj]no_pr\\/\\\\\\\\/oject/[tstser]Test_Vorbeifahrt;123/[mea]Run_middEng_FINAL_RES/[dts]Detector\\;rms A fast - Zusammenfassung;123";
        Pattern pattern = Pattern.compile("\\[(.*)\\]([^;]*);?(.*)");
        String[] strAr = input.split("(?<!\\\\)/"); // split by '/', check escaping
        for (String str : strAr) {
//            if (str.isEmpty()) {
//                continue;
//            }
            Matcher m = pattern.matcher(str);
            if (m.matches()) {
                String aeName = m.group(1);
                String ieName = m.group(2);
                String version = m.group(3);
                System.out.println("AE: " + aeName);
                System.out.println("IE: " + ieName);
                System.out.println("VE: " + version);
                System.out.println("-");
            }
        }
        System.out.println("-----------------------------------------------");
    }

}
