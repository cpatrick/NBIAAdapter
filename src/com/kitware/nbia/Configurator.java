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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The configurator uses Java's property-file construct to load and save a
 * configuration files with reasonable defaults.
 * 
 * @author Patrick Reynolds
 */
public class Configurator {

  /**
   * The property object for dealing with configuration parameters
   */
  protected Properties props;

  /**
   * The comment to place at the top of the config file
   */
  private static final String CONFIG_COMMENT = "NBIAAdapter Configuration";

  /**
   * Standard Constructor. Note the default values below.
   */
  public Configurator() {
    this.props = new Properties();
    props.setProperty("gridServiceUrl",
        "http://imaging.nci.nih.gov/wsrf/services/cagrid/NCIACoreService");
    props.setProperty("clientDownloadLocation", "NBIAGridClientDownload");
    props.setProperty("testUUID", "1.3.6.1.4.1.9328.50.1.8862");
  }

  /**
   * Loads configuration parameters from a file
   * 
   * @param name
   *          - the name of the file to load
   * @return 0 for success, 1 for file not found, and 2 for other IO exceptions.
   */
  public int load(String name) {
    try {
      this.props.load(new FileInputStream(name));
      return 0;
    } catch (FileNotFoundException e) {
      System.err.println("File not found on Config Load");
      return 1;
    } catch (IOException e) {
      System.err.println("Unknown IO Exception on Config Load");
      return 2;
    }
  }

  /**
   * Save the config file.
   * 
   * @param name
   *          - the name of the configuration file
   * @return 0 for success, 1 for file not found, and 2 for other IO exceptions.
   */
  public int save(String name) {
    try {
      this.props.store(new FileOutputStream(name), CONFIG_COMMENT);
      return 0;
    } catch (FileNotFoundException e) {
      System.err.println("File not found on Config Save");
      return 1;
    } catch (IOException e) {
      System.err.println("Unknown IO Exception on Config Save");
      return 2;
    }
  }

  /**
   * @return the standard Java Property object
   */
  public Properties getProps() {
    return props;
  }

}
