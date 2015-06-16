package pw.prok.bootstrap.tasks;

import org.eclipse.aether.artifact.DefaultArtifact;
import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;

import java.io.File;

public class Libraries extends DefaultTask {
    @Override
    public void make() {
        File serverDir = getServerDir();
        File libraries = new File(serverDir, "libraries");
        for (String library : mMain.cli.getOptionValues(mMain.libraries.getOpt())) {
            Sync.syncArtifact(new LibraryArtifact(new DefaultArtifact(library)), libraries, true);
        }
    }
}
