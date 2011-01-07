/**
 * 
 */
package com.kitware.nbia;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * @author cpatrick
 *
 */
public class Configurator
{

  protected Properties props;
  
  private static final String CONFIG_COMMENT = "NBIAAdapter Configuration";
  
  /**
   * 
   */
  public Configurator()
  {
    this.props = new Properties();
    props.setProperty( "gridServiceUrl", 
                       "http://imaging.nci.nih.gov/wsrf/services/cagrid/NCIACoreService" );
    props.setProperty( "clientDownloadLocation", "NBIAGridClientDownload" );
  }
  
  /**
   * Loads configuration parameters from a file
   * @param name - the name of the file to load
   * @return An integer indicating return status. 0 for success, 1 for file
   * not found, and 2 for other IO exceptions.
   */
  public int load( String name )
  {  
    try
    {
      this.props.load( new FileInputStream(name) );
      return 0;
    }
    catch( FileNotFoundException e )
    {
      System.err.println( "File not found on Config Load" );
      return 1;
    }
    catch( IOException e )
    {
      System.err.println( "Unknown IO Exception on Config Load" );
      return 2;
    }
  }
  
  public int save( String name )
  {
    try
    {
      this.props.store( new FileOutputStream(name), CONFIG_COMMENT );
      return 0;
    }
    catch( FileNotFoundException e )
    {
      System.err.println( "File not found on Config Save" );
      return 1;
    }
    catch( IOException e )
    {
      System.err.println( "Unknown IO Exception on Config Save" );
      return 2;
    }
  }

  
  /**
   * @return the standard java property object
   */
  public Properties getProps()
  {
    return props;
  }
  

}
