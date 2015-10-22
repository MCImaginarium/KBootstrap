package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;
import pw.prok.damask.Damask;
import pw.prok.damask.dsl.Builder;
import pw.prok.damask.dsl.IArtifact;
import pw.prok.damask.dsl.Version;

import java.io.File;
import java.io.FileFilter;

public class InstallKCauldron extends DefaultTask {
    private static final FileFilter VERSION_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        File binDir = getBinDir();
        String artifactNotation = mMain.cli.getOptionValue(mMain.installKCauldron.getLongOpt());
        make(serverDir, binDir, artifactNotation);
    }

    public static File make(File serverDir, File binDir, String artifactNotation) throws Exception {
        artifactNotation = shorthand(artifactNotation);
        IArtifact artifact = Builder.create().parse(artifactNotation).asArtifact();
        LibraryArtifact jar = null;
        System.out.print("Resolve KCauldron version... ");
        try {
            artifact = Builder.create().fromArtifact(artifact).fromModuleVersion(Damask.get().versionList(artifact).getLatestVersion()).asArtifact();
            jar = findJar(binDir, artifact);
            System.out.println("SUCCESS: " + artifact.getVersion());
        } catch (Exception ignored) {
            System.out.print("FAILED\nTrying to find latest local version... ");
            jar = findJar(binDir, artifact);
            if (jar != null) {
                System.out.println("FOUND: " + jar.getArtifact().getVersion().toRawString() + "\nSo we're found something, attempting to launch...");
                artifact = jar.getArtifact();
            } else {
                System.out.println("FAILED\nNothing to launch ;( Goodbye!");
            }
        }
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        if (jar == null) {
            jar = new LibraryArtifact(artifact, new File(binDir, Builder.asPath(artifact, true, true)));
        }
        File file = Sync.syncArtifact(jar, binDir, true);
        if (file == null) {
            throw new IllegalStateException("Could not install libraries");
        }
        DefaultTask.postInstall(serverDir, file);
        return file;
    }

    private static LibraryArtifact findJar(File binDir, IArtifact artifact) {
        if (artifact.getVersion().isSpecified()) {
            File f = new File(binDir, Builder.asPath(artifact, true, true));
            return f.exists() ? new LibraryArtifact(artifact, f) : null;
        }
        File dir = new File(binDir, Builder.asPath(artifact, false, false));
        if (!dir.exists()) return null;
        File[] versionDirs = dir.listFiles(VERSION_FILTER);
        if (versionDirs == null || versionDirs.length == 0) return null;
        Version maxVersion = null;
        for (File file : versionDirs) {
            Version version = new Version(file.getName());
            if (maxVersion == null || version.compareTo(maxVersion) > 0) {
                maxVersion = version;
            }
        }
        if (maxVersion != null) {
            artifact = Builder.create().fromArtifact(artifact).version(maxVersion).asArtifact();
            File f = new File(binDir, Builder.asPath(artifact, true, true));
            return f.exists() ? new LibraryArtifact(artifact, f) : null;
        }
        return null;
    }

    private static String shorthand(String s) {
        if (s == null || "latest".equals(s)) {
            return "pw.prok:KCauldron:0+";
        }
        if (s.startsWith("backport-")) {
            return String.format("pw.prok:KCauldron-Backport-%s:0+", s.substring("backport-".length()));
        }
        return s;
    }
}
