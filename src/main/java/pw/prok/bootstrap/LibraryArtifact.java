package pw.prok.bootstrap;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.io.File;

public final class LibraryArtifact {
    private Artifact mArtifact;
    private final String mLocation;
    private final String mFilename;

    public LibraryArtifact(String group, String name, String version, String location, String filename) {
        this(new DefaultArtifact(group, name, "jar", version), location, filename);
    }

    public LibraryArtifact(String group, String name, String version) {
        this(new DefaultArtifact(group, name, "jar", version), null, null);
    }

    public LibraryArtifact(Artifact artifact) {
        this(artifact, null, null);
    }

    public LibraryArtifact(Artifact artifact, String location, String filename) {
        mArtifact = artifact;
        mLocation = location;
        mFilename = filename;
    }

    public Artifact getArtifact() {
        return mArtifact;
    }

    public boolean hasLocation() {
        return mLocation != null;
    }

    public String getLocation() {
        return mLocation;
    }

    public String getRealLocation() {
        if (mLocation != null) {
            return compute(mLocation);
        }
        String groupId = mArtifact.getGroupId().replace('.', '/');
        String artifactId = mArtifact.getArtifactId();
        String version = mArtifact.getVersion();
        return String.format("%s/%s/%s", groupId, artifactId, version);
    }

    public boolean hasFilename() {
        return mFilename != null;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getRealFilename() {
        if (mFilename != null) {
            return compute(mFilename);
        }
        String artifactId = mArtifact.getArtifactId();
        String version = mArtifact.getVersion();
        String classifier = mArtifact.getClassifier();
        String extension = mArtifact.getExtension();
        classifier = classifier != null ? '-' + classifier : "";
        return String.format("%s-%s%s.%s", artifactId, version, classifier, extension);
    }

    public String compute(String s) {
        s = s.replace("<group>", mArtifact.getGroupId().replace('.', '/'));
        s = s.replace("<artifact>", mArtifact.getArtifactId());
        s = s.replace("<version>", mArtifact.getVersion());
        s = s.replace("<classifier>", mArtifact.getClassifier());
        s = s.replace("<extension>", mArtifact.getExtension());
        return s;
    }

    @Override
    public String toString() {
        return String.valueOf(mArtifact);
    }

    public void setArtifact(Artifact artifact) {
        mArtifact = artifact;
    }

    public File getTarget(File rootDir) {
        return new File(new File(rootDir, getRealLocation()), getRealFilename());
    }
}
