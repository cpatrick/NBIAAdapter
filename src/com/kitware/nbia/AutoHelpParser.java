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