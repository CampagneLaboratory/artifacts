package artifacts;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;

import java.io.File;

/**
 * Main class of the GobyWeb artifact manager.
 */
public class ArtifactManager {

    public static void main(String[] args) throws Exception {
        JSAP jsap = new JSAP(ArtifactManager.class.getResource("ArtifactManager.jsap"));

        JSAPResult config = jsap.parse(args);

        if (!config.success()) {

            // print out the help, then specific error messages describing the problems
            // with the command line, THEN print usage.
            //  This is called "beating the user with a clue stick."

            System.err.println(jsap.getHelp());

            for (java.util.Iterator errs = config.getErrorMessageIterator();
                 errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }

            System.err.println();
            System.err.println("Usage: java "
                    + ArtifactManager.class.getName());
            System.err.println("                "
                    + jsap.getUsage());
            System.err.println();

            System.exit(1);
        }
        File repoDir=config.getFile("repository");
        ArtifactRepo repo=new ArtifactRepo(repoDir);
        try {
        repo.acquireExclusiveLock();
        repo.load(repoDir);
        } finally {
            repo.releaseLock();
        }
        String [] artifacts=config.getStringArray("artifacts");
        for (String a: artifacts) {
            String tokens[]=a.split(":");
            if (tokens.length!=2) {
                System.err.println("Error parsing artifact description, format must be PLUGIN_ID:ARTIFACT_ID, instead got "+a);
                System.exit(1);
            }
            String pluginId=tokens[0];
            String artifactId=tokens[1];
            repo.install(pluginId,artifactId);
        }
    }




}
