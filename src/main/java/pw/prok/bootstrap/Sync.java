package pw.prok.bootstrap;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Sync {
    public static File binDir(File serverDir) {
        return new File(serverDir, "bin");
    }

    public static class KCauldronInfo {
        public final boolean kcauldron;
        public final boolean legacy;
        public final String channel;
        public final String version;
        public final String[] classpath;

        public KCauldronInfo(boolean kcauldron, boolean legacy, String channel, String version, String[] classpath) {
            this.kcauldron = kcauldron;
            this.legacy = legacy;
            this.channel = channel;
            this.version = version;
            this.classpath = classpath;
        }
    }

    public static KCauldronInfo getInfo(File jar) {
        boolean kcauldron = false;
        boolean legacy = true;
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
            if (attributes.containsKey("KCauldron-Version")) {
                kcauldron = true;
                legacy = Boolean.parseBoolean(attributes.getValue("KCauldron-Legacy"));
                version = attributes.getValue("KCauldron-Version");
                channel = attributes.getValue("KCauldron-Channel");
            } else {
                version = attributes.getValue("Implementation-Version");
                channel = "unknown";
            }
            classpath = attributes.getValue("Class-Path").split(" ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new KCauldronInfo(kcauldron, legacy, channel, version, classpath);
    }

    public static void parseLibraries(File jar, List<LibraryArtifact> artifacts) {
        try {
            ZipFile serverZip = new ZipFile(jar);
            ZipEntry entry = serverZip.getEntry("META-INF/MANIFEST.MF");
            InputStream is = serverZip.getInputStream(entry);
            Manifest manifest = new Manifest(is);
            is.close();
            serverZip.close();
            String cp = manifest.getMainAttributes().getValue("Class-Path");
            if (cp == null) return;
            for (String s : cp.split(" ")) {
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
                String group = s.replace('/', '.');
                artifacts.add(new LibraryArtifact(group, artifact, version, legacy ? "libraries/<group>/<artifact>/<version>" : null, legacy ? filename : null));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                syncArtifact(artifact, rootDir, recursive);
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
        File artifactFile = new File(new File(rootDir, artifact.getRealLocation()), artifact.getRealFilename());
        if (!artifactFile.exists() || !checksum(artifactFile)) {
            System.out.print("Downloading " + artifact + "... ");
            try {
                artifact.setArtifact(Aether.resolveArtifact(artifact.getArtifact()));
                artifactFile.getParentFile().mkdirs();
                if (artifactFile.exists()) {
                    artifactFile.delete();
                }
                artifact.getArtifact().getFile().renameTo(artifactFile);
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
            sync(artifacts, rootDir, true);
        }
        return artifactFile;
    }

    private static final String[] ALGORITHMS = {"sha-1", "md5"};
}
