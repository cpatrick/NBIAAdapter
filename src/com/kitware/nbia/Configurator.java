/**
 * 
 */
package com.kitware.nbia;

import java.util.Properties;


/**
 * @author cpatrick
 *
 */
public class Configurator
{

  protected Properties props;
  
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
  
  public void load( String name )
  {
    
  }

}
