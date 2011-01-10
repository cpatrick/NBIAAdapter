package com.kitware.nbia;

import gov.nih.nci.cagrid.ncia.client.NCIACoreServiceClient;
import gov.nih.nci.ivi.utils.ZipEntryInputStream;
import jargs.gnu.CmdLineParser;

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

public class NBIAAdapter
{
  public static String clientDownloadLocation;
  public static String gridServiceUrl;
  
  private static Boolean verbose;
  
  private static void printUsage() 
  {
    System.err.println( "Usage: NBIAAdapter [-v,--verbose] [-s,--saveconfig filename]" +
                        "[-l,--loadconfig] [-t,--test]" );
  }
  
  private static void verbosePrint( String out )
  {
    if( verbose )
    {
      System.out.println( out );
    }
  }
  
  private static void runTest() throws Exception
  {
    String uuid = "1.3.6.1.4.1.9328.50.1.8862";
    fetchData( uuid, "" );
  }
  
  private static void fetchData( String uuid, String output ) throws Exception
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
    
    NCIACoreServiceClient client = new NCIACoreServiceClient( gridServiceUrl );
    
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

  public static void main( String args[] ) throws Exception
  {
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option verboseOption = parser.addBooleanOption( 'v', "verbose" );
    CmdLineParser.Option saveConfigOption = parser.addStringOption( 's', "saveconfig" );
    CmdLineParser.Option loadConfigOption = parser.addStringOption( 'l', "loadconfig" );
    CmdLineParser.Option testOption = parser.addBooleanOption( 't', "test" );
    CmdLineParser.Option helpOption = parser.addBooleanOption( 'h', "help" );
    CmdLineParser.Option uuidOption = parser.addStringOption( 'u', "--uuid" );
    CmdLineParser.Option outputOption = parser.addStringOption( 'o', "--output" );
    
    Configurator configurator = new Configurator();
    gridServiceUrl = configurator.getProps().getProperty( "gridServiceUrl" );
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
    
    verbose = (Boolean)parser.getOptionValue( verboseOption, Boolean.FALSE );
    String saveConfig = (String)parser.getOptionValue( saveConfigOption, "" );
    String loadConfig = (String)parser.getOptionValue( loadConfigOption, "" );
    Boolean test = (Boolean)parser.getOptionValue( testOption, Boolean.FALSE );
    Boolean help = (Boolean)parser.getOptionValue( helpOption, Boolean.FALSE );
    String uuid = (String)parser.getOptionValue( uuidOption, "" );
    String output = (String)parser.getOptionValue( outputOption, "" );
    
    // Print usage information
    if( help )
    {
      printUsage();
      System.exit( 0 );
    }
    
    // Load the config file into the configurator
    if( loadConfig != "" )
    {
      verbosePrint("Loading config: " + loadConfig);
      configurator.load( loadConfig );
    }
    
    // Run the testing code from the NBIA wiki if asked to
    if( test )
    {
      verbosePrint("Running Test from NBIA Wiki.");
      runTest();
    }
    
    // Run the application as intended 
    if( uuid != "" )
    {
      fetchData( uuid, output );
    }
    
    
    // Save the configuration
    if( saveConfig != "" )
    {
      verbosePrint("Saving config: " + saveConfig);
      configurator.save( saveConfig );
    }
  }
  
  
  private static String defaultDownloadLocation()
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
