package ch.ivyteam.maven.public_api_source;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.facade.internal.AttachedArtifact;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IDependencyMetadata;
import org.eclipse.tycho.p2.resolver.P2MetadataProvider;
import org.eclipse.tycho.source.OsgiSourceMojo;

/**
 * Copied from tycho-source-plugin
 * @see org.eclipse.tycho.source.SourcesP2MetadataProvider 
 */
@Component(role = P2MetadataProvider.class, hint = "SourcesP2MetadataProvider")
public class SourcesP2MetadataProvider implements P2MetadataProvider, Initializable {

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    private DependencyMetadataGenerator sourcesGenerator;

    /**
     * calls {@link PublicApiSourceMojo#isRelevantProjectImpl(MavenProject, BuildPropertiesParser)}
     * instead of {@link OsgiSourceMojo#isRelevantProjectImpl(MavenProject, BuildPropertiesParser)}
     */
    @SuppressWarnings("javadoc")
    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {
        if (PublicApiSourceMojo.isRelevantProjectImpl(project, buildPropertiesParser)) {
            IArtifactFacade sourcesArtifact = new AttachedArtifact(project, project.getBasedir(), "sources");
            return Collections.singletonMap(sourcesArtifact.getClassifier(),
                    sourcesGenerator.generateMetadata(sourcesArtifact, null, OptionalResolutionAction.REQUIRE));
        }
        return null;
    }

    @Override
    public void initialize() throws InitializationException {
        this.sourcesGenerator = equinox.getService(DependencyMetadataGenerator.class, "(role-hint=source-bundle)");
    }
}