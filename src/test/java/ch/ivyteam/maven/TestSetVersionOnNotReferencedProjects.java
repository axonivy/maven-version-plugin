package ch.ivyteam.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestSetVersionOnNotReferencedProjects {

  private InMemoryLog log;
  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();

  @TempDir
  Path tempDir;

  @BeforeEach
  void before() throws IOException {
    log = new InMemoryLog();
    FileSet testProjectFs = new FileSet();
    testProjectFs.setDirectory(tempDir.toAbsolutePath().toString());
    testProjectFs.setIncludes(List.of("**/pom.xml"));
    testProjectFs.setExcludes(List.of(
            "**/ch.ivyteam.ivy.another.feature/pom.xml",
            "**/ch.ivyteam.ivy.build.maven/*"));
    testee.eclipseArtifactPoms = new FileSet[] {testProjectFs};
    FileSet testPoms = new FileSet();
    testPoms.setDirectory(tempDir.toAbsolutePath().toString());
    testPoms.setIncludes(Arrays.asList("development/ch.ivyteam.ivy.build.maven/**/pom.xml"));
    testee.pomsToUpdate = new FileSet[] {testPoms};
    testee.setLog(log);
    testee.version = "5.1.14-SNAPSHOT";
    testee.externalBuiltArtifacts = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension",
            "ch.ivyteam.ivy.richdialog.components",
            "ch.ivyteam.ivy.designer.cm.ui", "ch.ivyteam.vn.feature", "ch.ivyteam.ulc.base.source",
            "ch.ivyteam.ulc.extension.source", "ch.ivyteam.ivy.richdialog.components.source");
    PathUtils.copyFolder(Path.of("src/test/projects/originalIvy"), tempDir);
  }

  @Test
  void pomVersion() throws MojoExecutionException, IOException {
    testee.execute();
    comparePom();
  }

  @Test
  void configAndModulesPomVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareMavenConfigPom();
    compareMavenModulesPom();
  }

  @Test
  void notReferencedTestPomAndFeature() throws MojoExecutionException, IOException {
    testee.execute();
    comparePom("development/features/ch.ivyteam.ivy.another.feature/pom.xml");
    compareFeature("development/features/ch.ivyteam.ivy.another.feature/feature.xml");
    comparePom("development/features/ch.ivyteam.ivy.test.feature/pom.xml");
    compareFeature("development/features/ch.ivyteam.ivy.test.feature/feature.xml");
    comparePom("development/updatesites/ch.ivyteam.ivy.test.p2/pom.xml");
    compareCategory("development/updatesites/ch.ivyteam.ivy.test.p2/category.xml");
  }

  @Test
  void log() throws MojoExecutionException, IOException {
    testee.execute();
    compareLog();
  }

  private void compareMavenModulesPom() throws IOException {
    comparePom("development/ch.ivyteam.ivy.build.maven/pom.xml");
  }

  private void compareMavenConfigPom() throws IOException {
    comparePom("development/ch.ivyteam.ivy.build.maven/config/pom.xml");
  }

  private void comparePom() throws IOException {
    comparePom("pom.xml");
  }

  private void comparePom(String relativePomPath) throws IOException {
    String testeePom = Files.readString(tempDir.resolve(relativePomPath));
    String referencePom = Files.readString(new File("src/test/projects/referenceIvy", relativePomPath).toPath());
    assertThat(testeePom).as("Content of '" + relativePomPath + "' is wrong").isEqualTo(referencePom);
  }

  private void compareFeature(String relativeFeatureXmlPath) throws IOException {
    String testeeManifest = Files.readString(tempDir.resolve(relativeFeatureXmlPath));
    String referenceManifest = Files.readString(new File("src/test/projects/referenceIvy", relativeFeatureXmlPath).toPath());
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void compareCategory(String relativeCategoryXmlPath) throws IOException {
    String testeeCatInfo = Files.readString(tempDir.resolve(relativeCategoryXmlPath));
    String referenceCatInfo = Files.readString(new File("src/test/projects/referenceIvy", relativeCategoryXmlPath).toPath());
    assertThat(testeeCatInfo).isEqualTo(referenceCatInfo);
  }

  private void compareLog() throws IOException {
    List<String> referenceLog = Files.readAllLines(new File("src/test/projects/referenceIvy/log.txt").toPath());
    List<String> cleanedReferenceLog = new ArrayList<>();
    for (String line : referenceLog) {
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testIvy\\",
              testee.eclipseArtifactPoms[0].getDirectory() + "\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(cleanedReferenceLog).containsOnly(log.log.toArray(new String[log.log.size()]));
  }
}
