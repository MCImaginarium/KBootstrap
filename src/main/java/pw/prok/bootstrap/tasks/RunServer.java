package pw.prok.bootstrap.tasks;

import java.io.File;

public class RunServer extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        File binDir = getBinDir();
        File serverJar = new File(mMain.cli.getOptionValue(mMain.runServer.getLongOpt()));
        runServer(InstallServer.make(serverDir, binDir, serverJar), serverDir);
    }
}
