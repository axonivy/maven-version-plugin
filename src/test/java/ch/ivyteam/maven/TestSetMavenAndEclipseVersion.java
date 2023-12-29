package ch.ivyteam.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestSetMavenAndEclipseVersion {

  private static final File POM_FILE = new File("testProject/pom.xml");

  private InMemoryLog log;
  private SetMavenAndEclipseVersion testee = new SetMavenAndEclipseVersion();

  @TempDir
  Path tempDir;

  @BeforeEach
  void before() throws IOException {
    log = new InMemoryLog();
    var testProjectFs = new FileSet();
    testProjectFs.setDirectory(tempDir.toAbsolutePath().toString());
    testProjectFs.setIncludes(List.of("pom.xml"));
    testee.eclipseArtifactPoms = new FileSet[] {testProjectFs};
    testee.setLog(log);
    testee.version = "5.1.14-SNAPSHOT";
    testee.externalBuiltArtifacts = List.of(
            "ch.ivyteam.ulc.base",
            "ch.ivyteam.ulc.extension",
            "ch.ivyteam.ivy.richdialog.components",
            "ch.ivyteam.ivy.designer.cm.ui",
            "ch.ivyteam.vn.feature",
            "ch.ivyteam.ulc.base.source",
            "ch.ivyteam.ulc.extension.source",
            "ch.ivyteam.ivy.richdialog.components.source");
    PathUtils.copyFolder(Path.of("src/test/projects/originalProject"), tempDir);
  }

  @Test
  void bundleManifestVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareManifest();
  }

  @Test
  void pomVersion() throws MojoExecutionException, IOException {
    testee.execute();
    comparePom();
  }

  @Test
  void featureVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareFeature();
  }

  @Test
  void productVersion() throws MojoExecutionException, IOException {
    testee.execute();
    compareProduct();
  }

  @Test
  void log() throws MojoExecutionException, IOException {
    testee.execute();
    compareLog();
  }

  private void compareFeature() throws IOException {
    var testeeManifest = Files.readString(tempDir.resolve("feature.xml"));
    var referenceManifest = Files.readString(new File("src/test/projects/referenceProject/feature.xml").toPath());
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void comparePom() throws IOException {
    var testeeManifest = Files.readString(POM_FILE.toPath());
    var referenceManifest = Files.readString(new File("src/test/projects/referenceProject/pom.xml").toPath());
    assertThat(testeeManifest).isEqualTo(referenceManifest);
  }

  private void compareManifest() throws IOException {
    var testeeManifest = Files.readString(tempDir.resolve("META-INF/MANIFEST.MF"));
    var referenceManifest = Files.readString(new File("src/test/projects/referenceProject/META-INF/MANIFEST.MF").toPath());
    assertThat(testeeManifest).isEqualToIgnoringWhitespace(referenceManifest);
  }

  private void compareLog() throws IOException {
    var referenceLog = Files.readAllLines(new File("src/test/projects/referenceProject/log.txt").toPath());
    var cleanedReferenceLog = new ArrayList<>();
    for (String line : referenceLog) {
      line = StringUtils.replace(line, "C:\\dev\\maven-plugin\\maven-plugin\\testProject\\",
              tempDir.toAbsolutePath() + "\\");
      line = StringUtils.replace(line, "\\", File.separator);
      cleanedReferenceLog.add(line);
    }
    assertThat(log.log).containsOnly(cleanedReferenceLog.toArray(String[]::new));
  }

  private void compareProduct() throws IOException {
    var testeeProduct = Files.readString(tempDir.resolve("Designer.product"));
    var referenceProduct = Files.readString(new File("src/test/projects/referenceProject/Designer.product").toPath());
    assertThat(testeeProduct).isEqualTo(referenceProduct);
  }
}
