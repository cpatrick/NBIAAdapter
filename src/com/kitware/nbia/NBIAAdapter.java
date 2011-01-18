package com.kitware.nbia;

import jargs.gnu.CmdLineParser;

public class NBIAAdapter
{
  public static NBIASimpleClient nbiaClient;
  private static Boolean verbose;
  
  public static void main( String args[] ) throws Exception
  {
    AutoHelpParser parser = new AutoHelpParser();
    parser.setExeName( "NBIAAdapter" );
    CmdLineParser.Option verboseOption = parser.addHelp( 
      parser.addBooleanOption( 'v', "verbose" ),
      "Print additional information at each step." );
    CmdLineParser.Option saveConfigOption = parser.addHelp( 
      parser.addStringOption( 's', "saveconfig" ),
      "Save parameters to the specified config file." );
    CmdLineParser.Option loadConfigOption = parser.addHelp(  
      parser.addStringOption( 'l', "loadconfig" ),
      "Load parameters from the specified config file." );
    CmdLineParser.Option testOption = parser.addHelp(  
      parser.addBooleanOption( 't', "test" ),
      "Run the standard test." );
    CmdLineParser.Option helpOption = parser.addHelp(  
      parser.addBooleanOption( 'h', "help" ),
      "Print this help message" );
    CmdLineParser.Option uuidOption = parser.addHelp(  
      parser.addStringOption( 'u', "uuid" ),
      "Specify the series UUID you wish to download." );
    CmdLineParser.Option outputOption = parser.addHelp(  
      parser.addStringOption( 'o', "output" ),
      "Specify where to place the output." );
    
    Configurator configurator = new Configurator();
    String gridServiceUrl = configurator.getProps().getProperty( "gridServiceUrl" );
    String clientDownloadLocation = configurator.getProps().getProperty( "clientDownloadLocation" );
    nbiaClient = new NBIASimpleClient( gridServiceUrl, clientDownloadLocation );
    
    try
    {
      parser.parse( args );
    }
    catch( CmdLineParser.OptionException e)
    {
      System.err.println(e.getMessage());
      parser.printUsage();
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
      parser.printUsage();
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
      nbiaClient.fetchData( uuid, output );
    }
    
    
    // Save the configuration
    if( saveConfig != "" )
    {
      verbosePrint("Saving config: " + saveConfig);
      configurator.save( saveConfig );
    }
  }
  
  /**
   * Print things only if verbosity is turned on
   * @param out - the string to print
   */
  private static void verbosePrint( String out )
  {
    if( verbose )
    {
      System.out.println( out );
    }
  }
  
  private static void runTest() throws Exception
  {
    nbiaClient.fetchData( "1.3.6.1.4.1.9328.50.1.8862", "" );
  }
}
