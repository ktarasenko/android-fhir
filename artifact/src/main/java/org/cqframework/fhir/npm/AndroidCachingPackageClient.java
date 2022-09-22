package org.cqframework.fhir.npm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.PackageClient;
import org.hl7.fhir.utilities.npm.PackageInfo;

/**
 * Implementation of a package client that keeps a local disk cache of downloaded artifacts
 * in order to avoid re-downloading things
 */
public class AndroidCachingPackageClient extends PackageClient {

  private String cacheFolder;


  public AndroidCachingPackageClient(String cacheFolderRoot, String address) {
    super(address);
    try {
      cacheFolder = Utilities.path(cacheFolderRoot, ".fhir", "package-client");
      Utilities.createDirectory(cacheFolder);
    } catch (IOException e) {
    }
  }

  public boolean exists(String id, String ver) throws IOException {
    List<PackageInfo> vl = getVersions(id);
    for (PackageInfo pi : vl) {
      if (ver.equals(pi.getVersion())) {
        return true;
      }
    }
    return false;
  }

  public InputStream fetchCached(String url) throws IOException {
    File cacheFile = new File(Utilities.path(cacheFolder, fn(url)));
    if (cacheFile.exists()) {
      return new FileInputStream(cacheFile);
    }
    InputStream fetchedPackage = super.fetchCached(url);
    TextFile.bytesToFile(TextFile.streamToBytes(fetchedPackage), cacheFile);
    return new FileInputStream(cacheFile);
  }


  public Date getNewPackages(Date lastCalled, List<PackageInfo> updates) {
    return null;
  }



}
