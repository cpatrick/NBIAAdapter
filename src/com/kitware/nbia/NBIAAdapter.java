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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axis.AxisFault;

import jargs.gnu.CmdLineParser;

/**
 * Main class for configuration loading and passing input from the user to the
 * NBIA simple client.
 * 
 * @author Patrick Reynolds
 */
public class NBIAAdapter {

  private static AutoHelpParser parser;
  
  private static Boolean verbose;
  private static String saveConfig;
  private static String loadConfig;
  private static Boolean help;
  private static String uuid;
  private static String query;
  private static String output;
  
  private static Configurator configurator;
  private static String gridServiceUrl;
  private static String clientDownloadLocation;
  private static NBIASimpleClient nbiaClient;
  
  /**
   * Run when the class is executed
   * @param args
   * @throws Exception
   */
  public static void main(String args[]) {
    
    setupParser(args);
    
    loadConfiguration();

    // Run a query based on the CQL file provided
    if (query != "") {
      verbosePrint("Running Query: " + query);
      performQuery();
    }

    // Run the application as intended
    if (uuid != "") {
      verbosePrint("Fetching Data base on UUID");
      fetchData();
    }

    // Save the configuration
    if (saveConfig != "") {
      verbosePrint("Saving Config: " + saveConfig);
      configurator.save(saveConfig);
    }
  }

  /**
   * setup the CLI parser
   * @param args - the args from main
   */
  private static void setupParser( String[] args ) {
    parser = new AutoHelpParser();
    parser.setExeName("NBIAAdapter");
    CmdLineParser.Option verboseOption = parser.addHelp(
        parser.addBooleanOption('v', "verbose"),
        "Print additional information at each step.");
    CmdLineParser.Option saveConfigOption = parser.addHelp(
        parser.addStringOption('s', "saveconfig"),
        "Save parameters to the specified config file.");
    CmdLineParser.Option loadConfigOption = parser.addHelp(
        parser.addStringOption('l', "loadconfig"),
        "Load parameters from the specified config file.");
    CmdLineParser.Option helpOption = parser.addHelp(
        parser.addBooleanOption('h', "help"), "Print this help message");
    CmdLineParser.Option uuidOption = parser.addHelp(
        parser.addStringOption('u', "uuid"),
        "Specify the series UUID you wish to download.");
    CmdLineParser.Option queryOption = parser.addHelp(
        parser.addStringOption('q', "query"),
        "The CQL file on which to query.");
    CmdLineParser.Option outputOption = parser.addHelp(
        parser.addStringOption('o', "output"),
        "Specify where to place the output.");

    try {
      parser.parse(args);
    } catch (CmdLineParser.OptionException e) {
      System.err.println(e.getMessage());
      parser.printUsage();
      System.exit(2);
    }

    verbose = (Boolean) parser.getOptionValue(verboseOption, Boolean.FALSE);
    saveConfig = (String) parser.getOptionValue(saveConfigOption, "");
    loadConfig = (String) parser.getOptionValue(loadConfigOption, "");
    help = (Boolean) parser.getOptionValue(helpOption, Boolean.FALSE);
    uuid = (String) parser.getOptionValue(uuidOption, "");
    query = (String) parser.getOptionValue(queryOption, "");
    output = (String) parser.getOptionValue(outputOption, "");
    
    // Print usage information
    if (help) {
      parser.printUsage();
      System.exit(0);
    }
  }
  
  /**
   * Use the configurator to load the salient options
   */
  private static void loadConfiguration() {
    configurator = new Configurator();
    gridServiceUrl = configurator.getProps().getProperty(
        "gridServiceUrl");
    clientDownloadLocation = configurator.getProps().getProperty(
        "clientDownloadLocation");
    nbiaClient = new NBIASimpleClient(gridServiceUrl, clientDownloadLocation);

    // Load the configuration file into the Configurator
    if (loadConfig != "") {
      verbosePrint("Loading Config: " + loadConfig);
      configurator.load(loadConfig);
    }
    
  }
  
  /**
   * Use the client to fetch the data with the given uuid.
   */
  private static void fetchData() {
    try {
      nbiaClient.fetchData(uuid, output);
    } catch (AxisFault e) {
      System.out.println("Internal Server Error at NBIA Site.");
      if( verbose ) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("Unknown Server Error");
      if( verbose ) {
        e.printStackTrace();
      }   
    }
  }
  
  /**
   * 
   */
  private static void performQuery()
  {
    List<String> results = new ArrayList<String>();
    try {
      results.addAll(nbiaClient.query(query));
    } catch (AxisFault e) {
      System.out.println("Internal Server Error at NBIA Site.");
      if( verbose ) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("Unknown Server Error");
      if( verbose ) {
        e.printStackTrace();
      }   
    }
    for( Iterator<String> itr = results.iterator(); itr.hasNext(); )
    {
      System.out.print( itr.next() + "\n" );
    }
  }
  
  /**
   * Print things only if verbosity is turned on
   * 
   * @param out - the string to print
   */
  private static void verbosePrint(String out) {
    if (verbose) {
      System.out.println(out);
    }
  }

}
