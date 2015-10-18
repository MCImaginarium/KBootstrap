package pw.prok.bootstrap;


import pw.prok.damask.dsl.Builder;
import pw.prok.damask.dsl.IArtifact;

import java.io.File;

public final class LibraryArtifact {
    private IArtifact mArtifact;
    private final String mLocation;
    private final String mFilename;
    private File mTarget;

    public LibraryArtifact(String group, String name, String version, String location, String filename) {
        this(Builder.create().group(group).name(name).version(version).asArtifact(), location, filename);

    }

    public LibraryArtifact(String group, String name, String version) {
        this(Builder.create().group(group).name(name).version(version).asArtifact(), null, null);
    }

    public LibraryArtifact(IArtifact artifact) {
        this(artifact, null, null);
    }

    public LibraryArtifact(IArtifact artifact, String location, String filename) {
        mArtifact = artifact;
        mLocation = location;
        mFilename = filename;
    }

    public LibraryArtifact(IArtifact artifact, File target) {
        this(artifact, null, null);
        mTarget = target;
    }

    public IArtifact getArtifact() {
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
        String groupId = mArtifact.getGroup().replace('.', '/');
        String artifactId = mArtifact.getName();
        String version = mArtifact.getVersion().toRawString();
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
        String artifactId = mArtifact.getName();
        String version = mArtifact.getVersion().toRawString();
        String classifier = mArtifact.getClassifier();
        String extension = mArtifact.getExtension();
        classifier = classifier != null && classifier.length() > 0 ? ('-' + classifier) : "";
        return String.format("%s-%s%s.%s", artifactId, version, classifier, extension);
    }

    public String compute(String s) {
        s = s.replace("<group>", mArtifact.getGroup().replace('.', '/'));
        s = s.replace("<artifact>", mArtifact.getName());
        s = s.replace("<version>", mArtifact.getVersion().toRawString());
        s = s.replace("<classifier>", mArtifact.getClassifier());
        s = s.replace("<extension>", mArtifact.getExtension());
        return s;
    }

    @Override
    public String toString() {
        return String.valueOf(mArtifact);
    }

    public void setArtifact(IArtifact artifact) {
        mArtifact = artifact;
    }

    public File getTarget(File rootDir) {
        if (mTarget != null) return mTarget;
        return new File(new File(rootDir, getRealLocation()), getRealFilename());
    }

    public void setTarget(File target) {
        mTarget = target;
    }
}
