package pw.prok.bootstrap;

import pw.prok.damask.Damask;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Sync {
    public static class KCauldronInfo {
        public final boolean kcauldron;
        public final String group;
        public final String channel;
        public final String version;
        public final String[] classpath;

        public KCauldronInfo(boolean kcauldron, String group, String channel, String version, String[] classpath) {
            this.kcauldron = kcauldron;
            this.group = group;
            this.channel = channel;
            this.version = version;
            this.classpath = classpath;
        }
    }

    public static KCauldronInfo getInfo(File jar) {
        boolean kcauldron = false;
        String group = null;
        String channel = null;
        String version = null;
        String[] classpath = null;
        try {
            ZipFile serverZip = new ZipFile(jar);
            ZipEntry entry = serverZip.getEntry("META-INF/MANIFEST.MF");
            InputStream is = serverZip.getInputStream(entry);
            Manifest manifest = new Manifest(is);
            is.close();
            serverZip.close();
            Attributes attributes = manifest.getMainAttributes();
            if (attributes.getValue("KCauldron-Version") != null) {
                kcauldron = true;
                version = attributes.getValue("KCauldron-Version");
                channel = attributes.getValue("KCauldron-Channel");
                group = attributes.getValue("KCauldron-Group");
                if (group == null) group = "pw.prok";
            } else {
                version = attributes.getValue("Implementation-Version");
                group = "unknown";
                channel = "unknown";
            }
            String cp = attributes.getValue("Class-Path");
            classpath = cp == null ? new String[0] : cp.split(" ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KCauldronInfo(kcauldron, group, channel, version, classpath);
    }

    public static void parseLibraries(File jar, List<LibraryArtifact> artifacts) {
        String[] cp = getInfo(jar).classpath;
        if (cp == null) return;
        for (String s : cp) {
            if ("minecraft_server.1.7.10.jar".equals(s)) {
                artifacts.add(new LibraryArtifact("net.minecraft", "server", "1.7.10", ".", "minecraft_server.1.7.10.jar"));
                continue;
            }
            boolean legacy = s.startsWith("libraries/");
            if (legacy) {
                s = s.substring("libraries/".length());
            }
            int c = s.lastIndexOf('/');
            String filename = s.substring(c + 1);
            s = s.substring(0, c);
            String version = s.substring((c = s.lastIndexOf('/')) + 1).trim();
            s = s.substring(0, c);
            String artifact = s.substring((c = s.lastIndexOf('/')) + 1).trim();
            s = s.substring(0, c);
            String group = s.replace("../", "").replace('/', '.');
            artifacts.add(new LibraryArtifact(group, artifact, version, legacy ? "libraries/<group>/<artifact>/<version>" : null, legacy ? filename : null));
        }
    }

    public static boolean sync(File serverFile, File rootDir, boolean recursive) {
        List<LibraryArtifact> artifacts = new ArrayList<LibraryArtifact>();
        parseLibraries(serverFile, artifacts);
        return sync(artifacts, rootDir, recursive);
    }

    public static boolean sync(List<LibraryArtifact> artifacts, File rootDir, boolean recursive) {
        try {
            for (LibraryArtifact artifact : artifacts) {
                if (syncArtifact(artifact, rootDir, recursive) == null) {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean checksum(File artifactFile) {
        for (String algorithm : ALGORITHMS) {
            try {
                String digest = Utils.digest(algorithm, artifactFile);
                String checksum = Utils.readChecksum(algorithm, artifactFile);
                if (digest == null || checksum == null || !digest.equalsIgnoreCase(checksum)) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static File syncArtifact(LibraryArtifact artifact, File rootDir, boolean recursive) {
        File artifactFile = artifact.getTarget(rootDir);
        if (!artifactFile.exists() || !checksum(artifactFile)) {
            System.out.print("Downloading " + artifact + "... ");
            try {
                artifactFile.getParentFile().mkdirs();
                Damask.get().artifactResolve(artifact.getArtifact(), artifactFile, false);
                for (String algorithm : ALGORITHMS) {
                    Utils.writeChecksum(algorithm, artifactFile);
                }
                System.out.println("DONE!");
            } catch (Exception e) {
                System.out.println("ERROR!");
                e.printStackTrace();
                return null;
            }
        }
        if (recursive) {
            List<LibraryArtifact> artifacts = new ArrayList<LibraryArtifact>();
            parseLibraries(artifactFile, artifacts);
            if (!sync(artifacts, rootDir, true)) {
                return null;
            }
        }
        return artifactFile;
    }

    public static void resolveLatestVersion(File basedir, LibraryArtifact lib) {

    }

    private static final String[] ALGORITHMS = {"sha-1", "md5"};
}
