package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Main;
import pw.prok.bootstrap.Sync;
import pw.prok.damask.Damask;
import pw.prok.damask.dsl.Builder;
import pw.prok.damask.dsl.IArtifact;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

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
        if (mMain.cli.hasOption(mMain.warmRoast.getOpt())) runWarmroast(pid, javaHome, javaPath);
        process.waitFor();
    }

    private void runWarmroast(int pid, String javaHome, String javaPath) {
        try {
            IArtifact artifact = Builder.create().group("pw.prok").name("WarmRoastMappings").version("1.7.10").extension("zip").asArtifact();
            LibraryArtifact libraryArtifact = new LibraryArtifact(artifact);
            File mappings = Sync.syncArtifact(libraryArtifact, getBinDir(), false);
            File mappingsDir = unzip(mappings);

            List<String> classpath = new ArrayList<>();
            classpath.add(new File(javaHome + "/../lib/tools.jar").getCanonicalPath());
            classpath.addAll(Arrays.stream(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()).map(URL::getFile).collect(Collectors.toList()));

            List<String> args = new ArrayList<String>();
            args.add(javaPath);
            args.add("-Djava.library.path=" + javaHome + "/bin");
            args.add("-cp");
            args.add(classpath.stream().collect(Collectors.joining(File.pathSeparator)));
            args.add(WarmRoastExecutor.class.getName());
            args.add("0.0.0.0");
            args.add("23000");
            args.add(String.valueOf(pid));
            args.add(mappingsDir.getCanonicalPath());
            System.out.println(args);

            ProcessBuilder builder = new ProcessBuilder();
            builder.command(args);
            builder.environment().put("JAVA_HOME", javaHome);
            builder.inheritIO();
            Process process = builder.start();

            Thread.sleep(3000); // Wait until warmroast starting

            if (!openWebpage(new URL("http://127.0.0.1:23000"))) {
                System.err.println("Failed to launch browser, please visit manually http://127.0.0.1:23000");
            }
        } catch (Exception e) {
            new RuntimeException("Failed to run warmroast", e).printStackTrace();
        }
    }

    private File unzip(File file) throws IOException {
        File targetDir = new File(file.getCanonicalFile() + ".unzipped");
        targetDir.mkdirs();
        ZipFile zipFile = new ZipFile(file);
        zipFile.stream().forEach((entry) -> {
            File targetFile = new File(targetDir, entry.getName());
            if (entry.isDirectory()) targetFile.mkdirs();
            else if (!targetFile.exists()) {
                try {
                    Files.copy(zipFile.getInputStream(entry), targetFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return targetDir;
    }

    public static boolean openWebpage(URL url) {
        try {
            return openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
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
