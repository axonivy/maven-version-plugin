package ch.ivyteam.ivy.maven;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.regex.Pattern;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Downloads the ivy Engine from the NET if does not yet exists in the correct version.
 * 
 * @author Reguel Wermelinger
 * @since 18.09.2014
 */
@Mojo(name="ensureInstalledEngine")
public class EnsureInstalledEngineMojo extends AbstractEngineMojo
{
  /**
   * URL where the ivy Engine can be downloaded.
   */
  @Parameter(defaultValue="http://developer.axonivy.com/download/${ivyVersion}/AxonIvyDesigner${ivyVersion}.46995_Windows_x64.zip") 
  URL engineDownloadUrl;
  
  /** 
   * URL where a link to the ivy Engine in the expected {@link #ivyVersion} exists. 
   * The URL will be used to download the required engine if it does not yet exist.
   * The URL should point to a site providing HTML content with a link to the engine <br>e.g.
   * <code>&lt;a href="http://developer.axonivy.com/download/5.1.0/AxonIvyEngine5.1.0.46949_Windows_x86.zip"&gt; the engine&lt;/a&gt;</code>
   */
  @Parameter(defaultValue="http://developer.axonivy.com/download/product.php")
  URL engineListPageUrl;
  
  /** 
   * Engine type that will be downloaded if {@link #autoInstallEngine} is set and the engine must be 
   * retrieved from the {@link #engineListPageUrl}.
   * Possible values are:
   * <ul>
   *    <li>Linux_x64</li>
   *    <li>Linux_x86</li>
   *    <li>Windows_x64</li>
   *    <li>Windows_x86</li>
   * </ul>
   */
  @Parameter(defaultValue="Windows_x64")
  String osArchitecture;
  
  /** 
   * Enables the automatic installation of an ivy Engine in the {@link #engineDirectory}.
   * If there is yet no engine installed, or the {@link #ivyVersion} does not match, the
   * engine will be downloaded from the {@link #engineDownloadUrl} and unpacked into the
   * {@link #engineDirectory}.
   */
  @Parameter(defaultValue="false") 
  boolean autoInstallEngine;

  @Override
  public void execute() throws MojoExecutionException
  {
    getLog().info("Compiling project for ivy version " + ivyVersion);
    ensureEngineIsInstalled();
  }

  private void ensureEngineIsInstalled() throws MojoExecutionException
  {
    if (engineDirectoryIsEmpty())
    {
      engineDirectory.mkdirs();
    }
    
    String installedEngineVersion = getInstalledEngineVersion();
    if (!ivyVersion.equals(installedEngineVersion))
    {
      handleWrongIvyVersion(installedEngineVersion);
    }
  }

  private void handleWrongIvyVersion(String installedEngineVersion) throws MojoExecutionException
  {
    getLog().info("Installed engine has version '"+installedEngineVersion+"' instead of expected '"+ivyVersion+"'");
    if (autoInstallEngine)
    {
      File downloadZip = new EngineDownloader().downloadEngine();
      if (installedEngineVersion != null)
      {
        removeOldEngineContent();
      }
      unpackEngine(downloadZip);
      downloadZip.delete();
      
      installedEngineVersion = getInstalledEngineVersion();
      if (!ivyVersion.equals(installedEngineVersion))
      {
        throw new MojoExecutionException("Automatic installation of an ivyEngine failed. "
                + "Downloaded version is '"+installedEngineVersion+"' but expecting '"+ivyVersion+"'.");
      }
    }
    else
    {
      throw new MojoExecutionException("Aborting class generation as no valid ivy Engine is available! "
              + "Use the 'autoInstallEngine' parameter for an automatic installation.");
    }
  }

  private void removeOldEngineContent() throws MojoExecutionException
  {
    try
    {
      FileUtils.cleanDirectory(engineDirectory);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to clean outdated ivy Engine directory '"+engineDirectory+"'.", ex);
    }
  }

  private String getInstalledEngineVersion()
  {
    File ivyLibs = new File(engineDirectory, "lib/ivy");
    if (ivyLibs.exists())
    {
      String[] libraryNames = ivyLibs.list();
      if (!ArrayUtils.isEmpty(libraryNames))
      {
        String firstLibrary = libraryNames[0];
        String version = StringUtils.substringBetween(firstLibrary, "-", "-");
        return version;
      }
    }
    return null;
  }

  private boolean engineDirectoryIsEmpty()
  {
    return !engineDirectory.isDirectory() || ArrayUtils.isEmpty(engineDirectory.listFiles());
  }

  private void unpackEngine(File downloadZip) throws MojoExecutionException
  {
    try
    {
      String targetLocation = downloadZip.getParent();
      getLog().info("Unpacking engine "+downloadZip.getName()+" to "+targetLocation);
      ZipFile engineZip = new ZipFile(downloadZip);
      engineZip.extractAll(targetLocation);
    }
    catch (ZipException ex)
    {
      throw new MojoExecutionException("Failed to unpack downloaded engine '"+ downloadZip + "'.", ex);
    }
  }

  class EngineDownloader
  {
    
    private File downloadEngine() throws MojoExecutionException
    {
      try
      {
        return downloadEngineFromUrl(engineDownloadUrl);
      }
      catch(MojoExecutionException ex)
      {
        getLog().warn("Failed to download engine from "+engineDownloadUrl);
        return downloadEngineFromUrl(findEngineDownloadUrlFromListPage());
      }
    }
  
    private URL findEngineDownloadUrlFromListPage() throws MojoExecutionException
    {
      try(InputStream pageStream = engineListPageUrl.openStream())
      {
        return findEngineDownloadUrl(pageStream);
      }
      catch (IOException ex)
      {
        throw new MojoExecutionException("Failed to find engine download link in list page "+engineListPageUrl, ex);
      }
    }
    
    URL findEngineDownloadUrl(InputStream htmlStream) throws MojoExecutionException, MalformedURLException
    {
      Pattern enginePattern = Pattern.compile("href=[\"|'][^\"']*?AxonIvyEngine"+ivyVersion+"\\.[0-9]+_"+osArchitecture+"\\.zip");
      try(Scanner scanner = new Scanner(htmlStream))
      {
        String engineLinkMatch = scanner.findWithinHorizon(enginePattern, 0);
        String engineLink = StringUtils.substring(engineLinkMatch, "href='".length());
        if (engineLink == null)
        {
          throw new MojoExecutionException("Could not find a link to engine in version '"+ivyVersion+"' on site '"+engineListPageUrl+"'");
        }
        return toAbsoluteLink(engineListPageUrl, engineLink);
      }
    }
  
    private URL toAbsoluteLink(URL baseUrl, String parsedEngineArchivLink) throws MalformedURLException
    {
      boolean isAbsoluteLink = StringUtils.startsWith(parsedEngineArchivLink, "http://");
      if (isAbsoluteLink)
      {
        return new URL(parsedEngineArchivLink);
      }
      return new URL(baseUrl, parsedEngineArchivLink);
    }
  
    private File downloadEngineFromUrl(URL engineUrl) throws MojoExecutionException
    {
      try
      {
        String zipFileName = StringUtils.substringAfterLast(engineUrl.toExternalForm(), "/");
        File downloadZip = new File(engineDirectory, zipFileName);
        getLog().info("Starting engine download from "+engineUrl);
        Files.copy(engineUrl.openStream(), downloadZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return downloadZip;
      }
      catch (IOException ex)
      {
        throw new MojoExecutionException("Failed to download engine from '" + engineUrl + "' to '"
                + engineDirectory + "'", ex);
      }
    }
  }

}