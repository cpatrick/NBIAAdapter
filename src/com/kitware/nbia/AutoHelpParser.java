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

import jargs.gnu.CmdLineParser;

public class AutoHelpParser extends CmdLineParser
{
	protected List<String> optionHelpStrings = new ArrayList<String>();
	protected String exeName;

	public Option addHelp(Option option, String helpString) {
		optionHelpStrings.add(" -" + option.shortForm() + "/--" + option.longForm() + ": " + helpString);
		return option;
	}
	
	public void setExeName( String exeName )
	{
      this.exeName = exeName;
	}

	public void printUsage() {
        System.err.println( "Usage: " + this.exeName + " [options]" );
        for (Iterator<String> i = optionHelpStrings.iterator(); i.hasNext(); ) {
        	System.err.println(i.next());
        }
    }
}