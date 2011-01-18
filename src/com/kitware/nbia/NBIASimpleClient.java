/**
 * 
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
 * @author Patrick Reynolds
 *
 */
public class NBIASimpleClient {


	private String gridServiceUrl;
	private String clientDownloadLocation;

	/**
	 * 
	 */
	public NBIASimpleClient( String gridServiceUrl, String clientDownloadLocation ) 
	{
	  this.gridServiceUrl = gridServiceUrl;
	  this.clientDownloadLocation = clientDownloadLocation;
	}

	public void fetchData( String uuid, String output ) throws Exception
	{
		String finalOutput;
		if( output == "" )
		{
			finalOutput = defaultDownloadLocation();
		}
		else
		{
			finalOutput = output;
		}

		NCIACoreServiceClient client = new NCIACoreServiceClient( this.gridServiceUrl );

		TransferServiceContextReference tscr = 
			client.retrieveDicomDataBySeriesUID( uuid );

		TransferServiceContextClient tclient = 
			new TransferServiceContextClient( tscr.getEndpointReference() );

		InputStream istream = 
			TransferClientHelper.getData( tclient.getDataTransferDescriptor() );

		if( istream == null ) 
		{
			System.out.println( "istream is null" );
			return;
		}

		ZipInputStream zis = new ZipInputStream(istream);
		ZipEntryInputStream zeis = null;
		BufferedInputStream bis = null;
		String unzzipedFile = finalOutput;
		while(true) 
		{
			try 
			{
				zeis = new ZipEntryInputStream( zis );
			} 
			catch( EOFException e )
			{
				break;
			}

			bis = new BufferedInputStream(zeis);

			byte[] data = new byte[8192];
			int bytesRead = 0;
			BufferedOutputStream bos = 
				new BufferedOutputStream( new FileOutputStream( unzzipedFile + 
						File.separator + 
						zeis.getName() ) );

			while( ( bytesRead = ( bis.read( data, 0, data.length ) ) ) > 0 )
			{
				bos.write( data, 0, bytesRead );
			}
			bos.flush();
			bos.close();
		}

		zis.close();
		tclient.destroy();
	}
	
	private String defaultDownloadLocation()
	  {
	    String localClient= System.getProperty( "java.io.tmpdir" ) + 
	      File.separator + clientDownloadLocation;
	    if( !new File( localClient ).exists() )
	      {
	      new File( localClient ).mkdir();
	      }
	    System.out.println( "Local download location: " + localClient );
	    return localClient;
	  }

}
