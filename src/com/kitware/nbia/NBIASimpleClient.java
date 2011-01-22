/**
 * Copyright 2011 Kitware Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kitware.nbia;

import gov.nih.nci.cagrid.ncia.client.NCIACoreServiceClient;
import gov.nih.nci.ivi.utils.ZipEntryInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import org.cagrid.transfer.context.client.TransferServiceContextClient;
import org.cagrid.transfer.context.client.helper.TransferClientHelper;
import org.cagrid.transfer.context.stubs.types.TransferServiceContextReference;

/**
 * Simple abstraction of the NCIACoreServiceClient and its friends to allow for
 * the easy download of datasets from an NBIA instance.
 * 
 * @author Patrick Reynolds
 */
public class NBIASimpleClient {

  private String gridServiceUrl;
  private String clientDownloadLocation;

  /**
   * Standard constructor
   * 
   * @param gridServiceUrl
   *          - the URL of the NBIA Instance
   * @param clientDownloadLocation
   *          - the target location on disk
   */
  public NBIASimpleClient(String gridServiceUrl, String clientDownloadLocation) {
    this.gridServiceUrl = gridServiceUrl;
    this.clientDownloadLocation = clientDownloadLocation;
  }

  /**
   * Fetch a dataset with the given uuid from the NBIA instance indicated by the
   * Grid Service URL and place it in the Client Download Location
   * 
   * @param uuid
   *          - the series uuid of the dataset requested
   * @param output
   *          - the directory where the files end up.
   * @throws Exception
   */
  public void fetchData(String uuid, String output) throws Exception {
    String finalOutput;
    if (output == "") {
      finalOutput = defaultDownloadLocation();
    } else {
      finalOutput = output;
    }

    NCIACoreServiceClient client = new NCIACoreServiceClient(
        this.gridServiceUrl);

    TransferServiceContextReference tscr = client
        .retrieveDicomDataBySeriesUID(uuid);

    TransferServiceContextClient tclient = new TransferServiceContextClient(
        tscr.getEndpointReference());

    InputStream istream = TransferClientHelper.getData(tclient
        .getDataTransferDescriptor());

    if (istream == null) {
      System.out.println("istream is null");
      return;
    }

    ZipInputStream zis = new ZipInputStream(istream);
    ZipEntryInputStream zeis = null;
    BufferedInputStream bis = null;
    String unzzipedFile = finalOutput;
    while (true) {
      try {
        zeis = new ZipEntryInputStream(zis);
      } catch (EOFException e) {
        break;
      }

      bis = new BufferedInputStream(zeis);

      byte[] data = new byte[8192];
      int bytesRead = 0;
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(
          unzzipedFile + File.separator + zeis.getName()));

      while ((bytesRead = (bis.read(data, 0, data.length))) > 0) {
        bos.write(data, 0, bytesRead);
      }
      bos.flush();
      bos.close();
    }

    zis.close();
    tclient.destroy();
  }
  
  public void query( String query ) {
    // TODO Add implementation
  }

  /**
   * Generate a platform-independent default data directory for fetching new
   * datasets.
   * 
   * @return A string indicating the download location
   */
  private String defaultDownloadLocation() {
    String localClient = System.getProperty("java.io.tmpdir") + File.separator
        + clientDownloadLocation;
    if (!new File(localClient).exists()) {
      new File(localClient).mkdir();
    }
    System.out.println("Local download location: " + localClient);
    return localClient;
  }

}
