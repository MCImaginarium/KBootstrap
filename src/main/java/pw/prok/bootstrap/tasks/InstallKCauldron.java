package pw.prok.bootstrap.tasks;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import pw.prok.bootstrap.Aether;
import pw.prok.bootstrap.LibraryArtifact;
import pw.prok.bootstrap.Sync;

import java.io.File;

public class InstallKCauldron extends DefaultTask {
    @Override
    public void make() throws Exception {
        File serverDir = getServerDir();
        String artifactNotation = mMain.cli.getOptionValue(mMain.installKCauldron.getLongOpt());
        make(serverDir, artifactNotation);
    }

    public static File make(File serverDir, String artifactNotation) throws Exception {
        artifactNotation = shorthand(artifactNotation);
        Artifact artifact = new DefaultArtifact(artifactNotation);
        System.out.print("Resolve KCauldron version... ");
        artifact = Aether.resolveLatestVersion(artifact);
        System.out.println(artifact.getVersion());
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        Artifact baseArtifact = artifact;
        artifact = Aether.resolveArtifact(artifact);
        if (artifact == null) {
            // Attempt to download legacy server file
            artifact = new DefaultArtifact(baseArtifact.getGroupId(), baseArtifact.getArtifactId(), "server", "jar", baseArtifact.getVersion());
            artifact = Aether.resolveArtifact(artifact);
        }
        if (artifact == null) {
            throw new IllegalStateException("Could not resolve server jar");
        }
        boolean legacy = Sync.getInfo(artifact.getFile()).legacy;
        if (legacy) {
            System.out.println("Found legacy server jar");
        }
        File file = Sync.syncArtifact(new LibraryArtifact(artifact, legacy ? "." : null, null), legacy ? serverDir : Sync.binDir(serverDir), true);
        if (file == null) {
            throw new IllegalStateException("Could not install libraries");
        }
        DefaultTask.postInstall(serverDir, file);
        return file;
    }

    private static String shorthand(String s) {
        if (s == null || "latest".equals(s)) {
            return "pw.prok:KCauldron:jar:server:[0,)";
        }
        if (s.startsWith("backport-")) {
            return String.format("pw.prok:KCauldron-Backport-%s:jar:server:[0,)", s.substring("backport-".length()));
        }
        return s;
    }
}
