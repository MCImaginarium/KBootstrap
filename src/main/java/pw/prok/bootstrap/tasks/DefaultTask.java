package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.Aether;
import pw.prok.bootstrap.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class DefaultTask {
    protected Main mMain;

    public void setMain(Main main) {
        mMain = main;
        Aether.updateLocalRepo(getServerDir());
    }

    public File getServerDir() {
        try {
            File dir = new File(mMain.cli.getOptionValue(mMain.serverDir.getOpt(), ".")).getCanonicalFile();
            dir.mkdirs();
            return dir;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void putJvmArgs(List<String> args) {
        String[] z = mMain.cli.getOptionValues(mMain.jvmArgs.getOpt());
        if (z == null) return;
        for (String argRaw : z) {
            for (String arg : argRaw.split(" ")) {
                arg = arg.trim();
                if (arg.length() > 0) {
                    args.add(arg);
                }
            }
        }
    }

    public void runServer(File serverJar, File serverDir) throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaPath = String.format("%s/bin/java", javaHome);

        List<String> args = new ArrayList<>();
        args.add(javaPath);
        putJvmArgs(args);
        args.add("-jar");
        args.add(serverJar.getCanonicalPath());
        args.add("nogui");
        args.addAll(mMain.cli.getArgList());

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(serverDir);
        builder.command(args);
        builder.environment().put("JAVA_HOME", javaHome);
        builder.environment().put("KCAULDRON_HOME", serverDir.getCanonicalPath());
        builder.inheritIO();
        builder.start().waitFor();
    }

    public abstract void make() throws Exception;

    public static void postInstall(File serverDir, File serverJar) throws Exception {
        String[] symlinks = Main.instance.cli.getOptionValues(Main.instance.serverSymlinks.getOpt());
        if (symlinks != null) {
            for (String symlink : symlinks) {
                File symlinkPath = new File(serverDir, symlink);
                Files.deleteIfExists(symlinkPath.toPath());
                Files.createSymbolicLink(symlinkPath.toPath(), serverJar.toPath());
            }
        }
    }
}
