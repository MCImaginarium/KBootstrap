package pw.prok.bootstrap.tasks;

import java.io.File;

public class RunKCauldron extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        String artifactNotation = mMain.cli.getOptionValue(mMain.runKCauldron.getLongOpt());
        runServer(InstallKCauldron.make(serverDir, artifactNotation), serverDir);
    }
}
