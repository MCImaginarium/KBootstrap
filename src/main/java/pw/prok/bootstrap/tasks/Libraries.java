package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;
import pw.prok.damask.dsl.Builder;

import java.io.File;

public class Libraries extends DefaultTask {
    @Override
    public void make() {
        File serverDir = getBinDir();
        File libraries = new File(serverDir, "libraries");
        for (String library : mMain.cli.getOptionValues(mMain.libraries.getOpt())) {
            Sync.syncArtifact(new LibraryArtifact(Builder.create().parse(library).asArtifact()), libraries, true);
        }
    }
}
