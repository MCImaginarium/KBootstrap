package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.Main;
import pw.prok.damask.Damask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class DefaultTask {
    protected Main mMain;

    public void setMain(Main main) {
        mMain = main;
        Damask.get().addRepository("prok", "https://repo.prok.pw");
        Damask.get().addRepository("mavencentral", "http://repo1.maven.org/maven2");
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

    public File getBinDir() {
        String bin = mMain.cli.getOptionValue(mMain.binDir.getOpt());
        File dir;
        if (bin != null) {
            dir = new File(bin);
            if (!dir.isAbsolute()) {
                dir = new File(getServerDir(), bin);
            }
        } else {
            dir = new File(getServerDir(), "bin");
        }
        dir.mkdirs();
        return dir;
    }

    public File getPidFile() {
        String pid = mMain.cli.getOptionValue(mMain.pidFile.getOpt());
        File file;
        if (pid != null) {
            file = new File(pid);
            if (!file.isAbsolute()) {
                file = new File(getServerDir(), pid);
            }
        } else {
            file = new File(getServerDir(), "server.pid");
        }
        file.getParentFile().mkdirs();
        return file;
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

        List<String> args = new ArrayList<String>();
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
        builder.environment().put("KBOOTSTRAP_ACTIVE", "true");
        builder.inheritIO();
        Process process = builder.start();
        int pid = getPid(process);
        if (pid > 0) writePid(pid);
        process.waitFor();
    }

    private void writePid(int pid) {
        try {
            File pidFile = getPidFile();
            pidFile.deleteOnExit();
            Writer writer = new FileWriter(pidFile);
            writer.write(String.format("%d\n", pid));
            writer.close();
        } catch (Exception e) {
            new IllegalStateException("Failed to write pid file, ignoring...", e).printStackTrace();
        }
    }

    public static int getPid(Process process) {
        try {
            Class<?> processClass = process.getClass();
            Field field = processClass.getDeclaredField("pid");
            field.setAccessible(true);
            return field.getInt(process);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (IllegalArgumentException ignored) {
        }
        return -1;
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
