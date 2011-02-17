package de.rechner.openatfx.main;

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
import de.rechner.openatfx.util.PatternUtil;


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
        String input = "/[p\\]\\[rj]no_project;123/[tstser]Test_V\\[\\]\\;\\/orbeifahrt/[mea]Run_middEng_FINAL_RES/[dts]Detectorrms A fast - Zusammenfassung";
        // split by '/' considering escaping
        for (String p : input.split("(?<!\\\\)/")) {

            // split by ']' considering escaping
            String[] xAr = p.split("(?<!\\\\)]");
            if (xAr.length != 2) {
                continue;
            }
            System.out.println("-------------------");
            System.out.println("> " + p);
            String aeName = PatternUtil.unEscapeNameForASAMPath(xAr[0].substring(1, xAr[0].length()));

            // split by ';' considering escaping
            String[] yAr = xAr[1].split("(?<!\\\\);");
            if (yAr.length < 1) {
                continue;
            }
            String ieName = PatternUtil.unEscapeNameForASAMPath(yAr[0]);
            String version = yAr.length == 2 ? PatternUtil.unEscapeNameForASAMPath(yAr[1]) : "";

            System.out.println("AE: " + aeName);
            System.out.println("IE: " + ieName);
            System.out.println("VR: " + version);

        }
    }

}
