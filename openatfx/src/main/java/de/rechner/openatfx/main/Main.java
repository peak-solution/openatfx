package de.rechner.openatfx.main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static void main(String[] args) {
        try {
            ORB orb = ORB.init(new String[0], System.getProperties());
            AoFactory aoFactory = AoServiceFactory.getInstance().newAoFactory(orb);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            NameComponent path[] = ncRef.to_name("ATFX");
            ncRef.rebind(path, aoFactory);

            System.out.println("ATFX Server started");
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
        String input = "[transfer.atfx]/[prj]no_project/[tstser]Test_Vorbeifahrt;/[mea]Run_middEng_FINAL_RES;2a2/[dts]Detector_rms A fast - Zusammenfassung";
        Pattern pattern = Pattern.compile("/\\[([^]]*)\\]([^/;]*);?([^/]*)");
        Matcher m = pattern.matcher(input);
        while (m.find()) {
            System.out.println("---------------------------------");
            System.out.println(m.group(0));
            System.out.println("AE: " + m.group(1));
            System.out.println("IE: " + m.group(2));
            System.out.println("VE: " + m.group(3));
        }
    }

}
