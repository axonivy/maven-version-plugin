package ch.ivyteam.maven;

import java.util.List;

public class IvyArtifactDetector {

  private final List<String> externallyBuiltArtifacts;

  public IvyArtifactDetector(List<String> externallyBuiltArtifacts) {
    this.externallyBuiltArtifacts = externallyBuiltArtifacts;
  }

  public boolean isLocallyBuildIvyArtifact(String artifactId) {
    if (!artifactId.startsWith("ch.ivyteam.")) {
      return false;
    }
    return !externallyBuiltArtifacts.contains(artifactId);
  }
}
