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

import jargs.gnu.CmdLineParser;

public class NBIAAdapter
{
  public static String clientDownloadLocation;
  
  private static void printUsage() 
  {
    System.err.println( "Usage: NBIAAdapter [-v,--verbose] [-s,--saveconfig filename]" +
                        "[-l,--loadconfig]" );
  }

  public static void main( String args[] ) throws Exception
  {
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option verboseOption = parser.addBooleanOption( 'v', "verbose" );
    CmdLineParser.Option saveConfigOption = parser.addStringOption( 's', "saveconfig" );
    CmdLineParser.Option loadConfigOption = parser.addStringOption( 'l', "loadconfig" );
    
    Configurator configurator = new Configurator();
    String gridServiceUrl = configurator.getProps().getProperty( "gridServiceUrl" );
    clientDownloadLocation = configurator.getProps().getProperty( "clientDownloadLocation" );
    
    try
    {
      parser.parse( args );
    }
    catch( CmdLineParser.OptionException e)
    {
      System.err.println(e.getMessage());
      printUsage();
      System.exit( 2 );
    }
    
    Boolean verbose = (Boolean)parser.getOptionValue( verboseOption, Boolean.FALSE );
    String saveConfig = (String)parser.getOptionValue( saveConfigOption, "" );
    String loadConfig = (String)parser.getOptionValue( loadConfigOption, "" );
    
    configurator.save( "foo.txt" );
    
    String seriesInstanceUID = "1.3.6.1.4.1.9328.50.1.8862";
    System.out.println(seriesInstanceUID);
    
    NCIACoreServiceClient client = new NCIACoreServiceClient( gridServiceUrl );
    
    TransferServiceContextReference tscr = 
      client.retrieveDicomDataBySeriesUID( seriesInstanceUID );
    
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
    String unzzipedFile = downloadLocation();
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
  
  
  private static String downloadLocation()
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
