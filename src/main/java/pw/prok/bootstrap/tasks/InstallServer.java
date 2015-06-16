package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.Sync;
import pw.prok.bootstrap.Utils;

import java.io.File;

public class InstallServer extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        File serverJar = new File(mMain.installServer.getValue()).getCanonicalFile();
        if (!serverJar.exists()) {
            System.err.println("Server file not exists: " + serverJar);
            return;
        }
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        File targetServerJar = new File(serverDir, serverJar.getName()).getCanonicalFile();
        if (!targetServerJar.getCanonicalPath().equals(serverJar.getCanonicalPath())) {
            Utils.copyFile(serverJar, targetServerJar);
        }
        Sync.sync(targetServerJar, serverDir, true);
    }
}
