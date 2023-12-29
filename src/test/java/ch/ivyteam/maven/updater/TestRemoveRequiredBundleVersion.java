package ch.ivyteam.maven.updater;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ivyteam.maven.BundleManifestFileUpdater;
import ch.ivyteam.maven.InMemoryLog;

class TestRemoveRequiredBundleVersion {

  @TempDir
  Path tempDir;

  @Test
  void canHandleRequiredBundleRanges() throws IOException {
    var manifest = tempDir.resolve(BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    new BundleManifestFileUpdater(tempDir.toFile(), "6.1.0.XXX", new InMemoryLog(), List.of(), false).update();
    var updatedManifest = Files.readString(manifest);
    var expectedManifest = new String(streamOf("MANIFEST.MF_expected").readAllBytes(), StandardCharsets.UTF_8);
    assertThat(updatedManifest).isEqualTo(expectedManifest);
  }

  @Test
  void canRemoveBundleRanges() throws IOException {
    var manifest = tempDir.resolve(BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    new BundleManifestFileUpdater(tempDir.toFile(), "6.1.0.XXX", new InMemoryLog(), List.of(), true).update();
    var updatedManifest = Files.readString(manifest);
    var expectedManifest = new String(streamOf("MANIFEST.MF_expectedNoRanges").readAllBytes(), StandardCharsets.UTF_8);
    assertThat(updatedManifest).isEqualToIgnoringWhitespace(expectedManifest);
  }

  private static void createFile(Path file, InputStream content) throws IOException {
    Files.createDirectories(file.getParent());
    Files.write(file, content.readAllBytes(), StandardOpenOption.CREATE);
  }

  private static InputStream streamOf(String resouce) {
    return TestRemoveRequiredBundleVersion.class.getResourceAsStream(resouce);
  }
}
