package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;
import pw.prok.bootstrap.Utils;

import java.io.File;

public class InstallServer extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        File binDir = getBinDir();
        File serverJar = new File(mMain.cli.getOptionValue(mMain.installServer.getOpt())).getCanonicalFile();
        if (!serverJar.exists()) {
            System.err.println("Server file not exists: " + serverJar);
            return;
        }
        make(serverDir, binDir, serverJar);
    }

    public static File make(File serverDir, File binDir, File serverJar) throws Exception {
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        File targetServerBin = serverDir;
        File targetServerJar;
        Sync.KCauldronInfo info = Sync.getInfo(serverJar);
        if (info == null)
            throw new RuntimeException("Couldn't resolve main jar dependencies. Are you sure this correct server jar?");
        targetServerBin = binDir;
        targetServerJar = new LibraryArtifact(info.group, info.channel, info.version).getTarget(targetServerBin);
        if (!targetServerJar.getCanonicalPath().equals(serverJar.getCanonicalPath())) {
            Utils.copyFile(serverJar, targetServerJar);
        }
        if (!Sync.sync(targetServerJar, targetServerBin, true)) {
            throw new IllegalStateException("Could not install libraries");
        }
        DefaultTask.postInstall(serverDir, targetServerJar);
        return targetServerJar;
    }
}
