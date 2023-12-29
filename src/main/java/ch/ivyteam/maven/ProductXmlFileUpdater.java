package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class ProductXmlFileUpdater extends AbstractXmlFileUpdater {

  private File projectDirectory;
  private UpdateRun update;

  ProductXmlFileUpdater(File projectDirectory, String newVersion, Log log,
          List<String> externalBuiltArtifacts) {
    super(getProductFile(projectDirectory));
    this.projectDirectory = projectDirectory;
    update = new UpdateRun(xmlFile.getName(), newVersion, log, externalBuiltArtifacts);
  }

  private static File getProductFile(File projectDirectory) {
    try {
      var productFiles = Files.list(projectDirectory.toPath())
              .filter(p -> p.toString().endsWith(".product"))
              .toList();
      if (productFiles.isEmpty()) {
        return new File("NoProductFileExists.product");
      }
      return productFiles.get(0).toFile();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void update() throws SAXException, IOException, XPathExpressionException {
    if (xmlFile.exists()) {
      boolean changed = false;
      readXml();
      changed = updateVersion(changed);
      if (changed) {
        saveXml();
      } else {
        update.log.info("Product file " + xmlFile.getAbsolutePath() + " is up to date. Nothing to do.");
      }
    } else {
      update.log.debug("No *.product file found in project " + projectDirectory + ". Nothing to do");
    }
  }

  private boolean updateVersion(boolean changed) throws XPathExpressionException {
    String xPath = "/product";
    Node productNode = findNode(xPath);
    Node versionNode = getVersionAttributeNode(productNode);
    if (versionNeedsUpdate(versionNode, update.versionNoMavenQualifier())) {
      replaceAttributeText(productNode, versionNode, update.versionNoMavenQualifier());
      update.log.info("Replace product version " + versionNode.getTextContent() + " with version "
              + update.versionNoMavenQualifier() + " in product file " + xmlFile.getAbsolutePath());
      return true;
    }
    return changed;
  }

  private Node getVersionAttributeNode(Node node) {
    return node.getAttributes().getNamedItem("version");
  }
}
