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

import jargs.gnu.CmdLineParser;

/**
 * Basic subclass of CmdLineParser. This simply automates the creation of a
 * usage message on the command line.
 * 
 * @author Patrick Reynolds
 */
public class AutoHelpParser extends CmdLineParser {
  /**
   * Storage data structure for the help strings.
   */
  protected ArrayList<String> optionHelpStrings = new ArrayList<String>();

  /**
   * The name of the executable evoking the parser
   */
  protected String exeName;

  /**
   * Add a help string to the given command-line option
   * @param option
   * @param helpString
   * @return
   */
  public Option addHelp(Option option, String helpString) {
    optionHelpStrings.add(" -" + option.shortForm() + "/--" + option.longForm()
        + ": " + helpString);
    return option;
  }

  /**
   * Set the name of the executable to be used
   * @param exeName
   */
  public void setExeName(String exeName) {
    this.exeName = exeName;
  }

  /**
   * Print the usage information for each argument.
   */
  public void printUsage() {
    System.err.println("Usage: " + this.exeName + " [options]");
    for (Iterator<String> i = optionHelpStrings.iterator(); i.hasNext();) {
      System.err.println(i.next());
    }
  }
}