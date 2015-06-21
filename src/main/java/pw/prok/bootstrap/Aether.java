package pw.prok.bootstrap;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Aether {
    private static List<RemoteRepository> sRepositories;
    private static DefaultServiceLocator sLocator;
    private static RepositorySystem sRepositorySystem;
    private static LocalRepository sLocalRepository;

    static {
        sRepositories = new ArrayList<RemoteRepository>();
        sRepositories.add(new RemoteRepository.Builder("prok", "default", "https://repo.prok.pw/").build());
        sRepositories.add(new RemoteRepository.Builder("jcenter", "default", "https://jcenter.bintray.com/").build());

        sLocator = MavenRepositorySystemUtils.newServiceLocator();
        sLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        sLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        sRepositorySystem = sLocator.getService(RepositorySystem.class);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Utils.removeDir(sLocalRepository.getBasedir());
            }
        });
    }

    private static RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_INTERVAL + "-1");
        session.setLocalRepositoryManager(sRepositorySystem.newLocalRepositoryManager(session, sLocalRepository));
        return session;
    }

    public static Artifact resolveArtifact(Artifact artifact) {
        try {
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(artifact);
            request.setRepositories(sRepositories);
            ArtifactResult result = sRepositorySystem.resolveArtifact(newSession(), request);
            return result.isResolved() ? result.getArtifact() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Artifact resolveLatestVersion(String groupId, String artifactId) {
        return resolveLatestVersion(new DefaultArtifact(String.format("%s:%s:[0,)", groupId, artifactId)));
    }

    public static Artifact resolveLatestVersion(Artifact artifact) {
        try {
            VersionRangeRequest request = new VersionRangeRequest();
            request.setArtifact(artifact);
            request.setRepositories(sRepositories);
            VersionRangeResult result = sRepositorySystem.resolveVersionRange(newSession(), request);
            return artifact.setVersion(result.getHighestVersion().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return artifact;
        }

    }

    public static void updateLocalRepo(File dir) {
        sLocalRepository = new LocalRepository(new File(dir, ".local-repo"));
    }
}
