package de.rechner.openatfx_mdf;

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


public class AoFactoryMain {

    private static final Log LOG = LogFactory.getLog(AoFactoryMain.class);

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();

            // configure ORB
            ORB orb = ORB.init(new String[0], System.getProperties());
            MDFConverter converter = new MDFConverter();
            AoFactory aoFactory = converter.newAoFactory(orb);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            NameComponent path[] = ncRef.to_name("MDF.ASAM-ODS");
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

}
