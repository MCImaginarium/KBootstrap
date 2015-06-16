package pw.prok.bootstrap;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Sync {
    public static void parseLibraries(File jar, List<LibraryArtifact> artifacts) {
        try {
            ZipFile serverZip = new ZipFile(jar);
            ZipEntry entry = serverZip.getEntry("META-INF/MANIFEST.MF");
            InputStream is = serverZip.getInputStream(entry);
            Manifest manifest = new Manifest(is);
            String cp = manifest.getMainAttributes().getValue("Class-Path");
            if (cp == null) return;
            for (String s : cp.split(" ")) {
                if ("minecraft_server.1.7.10.jar".equals(s)) {
                    artifacts.add(new LibraryArtifact("net.minecraft", "server", "1.7.10", ".", "minecraft_server.1.7.10.jar"));
                    continue;
                }
                boolean library = s.startsWith("libraries/");
                if (library) {
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
                artifacts.add(new LibraryArtifact(group, artifact, version, library ? "libraries/<group>/<artifact>/<version>" : null, filename));
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
