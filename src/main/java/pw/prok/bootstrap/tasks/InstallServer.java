package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;
import pw.prok.bootstrap.Utils;

import java.io.File;

public class InstallServer extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        File serverJar = new File(mMain.cli.getOptionValue(mMain.installServer.getOpt())).getCanonicalFile();
        if (!serverJar.exists()) {
            System.err.println("Server file not exists: " + serverJar);
            return;
        }
        make(serverDir, serverJar);
    }

    public static File make(File serverDir, File serverJar) throws Exception {
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        File targetServerBin = serverDir;
        File targetServerJar;
        Sync.KCauldronInfo info = Sync.getInfo(serverJar);
        if (info.legacy) {
            System.out.println("Found legacy server jar");
            targetServerJar = new File(serverDir, serverJar.getName()).getCanonicalFile();
        } else if (info.kcauldron) {
            targetServerBin = Sync.binDir(serverDir);
            targetServerJar = new LibraryArtifact("custom", info.channel, info.version).getTarget(targetServerBin);
        } else {
            throw new IllegalStateException("Found non-legacy and non-kcauldron jar, meh?");
        }
        if (!targetServerJar.getCanonicalPath().equals(serverJar.getCanonicalPath())) {
            Utils.copyFile(serverJar, targetServerJar);
        }
        Sync.sync(targetServerJar, targetServerBin, true);
        DefaultTask.postInstall(serverDir, targetServerJar);
        return targetServerJar;
    }
}
