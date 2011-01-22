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

import jargs.gnu.CmdLineParser;

/**
 * Main class for configuration loading and passing input from the user to the
 * NBIA simple client.
 * 
 * @author Patrick Reynolds
 */
public class NBIAAdapter {
  
  private static Boolean verbose;

  /**
   * Run when the class is executed
   * @param args
   * @throws Exception
   */
  public static void main(String args[]) throws Exception {
    AutoHelpParser parser = new AutoHelpParser();
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
    CmdLineParser.Option testOption = parser.addHelp(
        parser.addBooleanOption('t', "test"), "Run the standard test.");
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

    Configurator configurator = new Configurator();
    String gridServiceUrl = configurator.getProps().getProperty(
        "gridServiceUrl");
    String clientDownloadLocation = configurator.getProps().getProperty(
        "clientDownloadLocation");
    String testUUID = configurator.getProps().getProperty("testUUID");
    NBIASimpleClient nbiaClient = new NBIASimpleClient(gridServiceUrl, clientDownloadLocation);

    try {
      parser.parse(args);
    } catch (CmdLineParser.OptionException e) {
      System.err.println(e.getMessage());
      parser.printUsage();
      System.exit(2);
    }

    verbose = (Boolean) parser.getOptionValue(verboseOption, Boolean.FALSE);
    String saveConfig = (String) parser.getOptionValue(saveConfigOption, "");
    String loadConfig = (String) parser.getOptionValue(loadConfigOption, "");
    Boolean test = (Boolean) parser.getOptionValue(testOption, Boolean.FALSE);
    Boolean help = (Boolean) parser.getOptionValue(helpOption, Boolean.FALSE);
    String uuid = (String) parser.getOptionValue(uuidOption, "");
    String query = (String) parser.getOptionValue(queryOption, "");
    String output = (String) parser.getOptionValue(outputOption, "");

    // Print usage information
    if (help) {
      parser.printUsage();
      System.exit(0);
    }

    // Load the configuration file into the Configurator
    if (loadConfig != "") {
      verbosePrint("Loading Config: " + loadConfig);
      configurator.load(loadConfig);
    }
    
    // Run a query based on the CQL file provided
    if (query != "") {
      verbosePrint("Running Query: " + query);
      nbiaClient.query(query);
    }

    // Run the testing code from the NBIA wiki if asked to
    if (test) {
      verbosePrint("Running Test from NBIA Wiki.");
      nbiaClient.fetchData(testUUID, "");
    }

    // Run the application as intended
    if (uuid != "") {
      nbiaClient.fetchData(uuid, output);
    }

    // Save the configuration
    if (saveConfig != "") {
      verbosePrint("Saving Config: " + saveConfig);
      configurator.save(saveConfig);
    }
  }

  /**
   * Print things only if verbosity is turned on
   * 
   * @param out
   */
  private static void verbosePrint(String out) {
    if (verbose) {
      System.out.println(out);
    }
  }

}
