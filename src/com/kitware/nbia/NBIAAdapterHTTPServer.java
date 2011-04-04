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

/*
 * An example of a very simple, multi-threaded HTTP server.
 * Implementation notes are in WebServer.html, and also
 * as comments in the source code.
 */
import jargs.gnu.CmdLineParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.apache.axis.AxisFault;

import com.google.gson.Gson;

public class NBIAAdapterHTTPServer implements HttpConstants {

  private static AutoHelpParser parser;
  
  public static Boolean verbose;
  private static String saveConfig;
  private static String loadConfig;
  private static String logFile;
  private static Boolean help;
  
  private static Configurator configurator;
  private static String gridServiceUrl;
  private static String clientDownloadLocation;
  
  protected String serverName = "NBIAAdapter";

  static Vector<Worker> threads = new Vector<Worker>();
  
  static HashMap<String,Response> uuidStatus = new HashMap<String,Response>();

  static int timeout;
  private static int port;
  static int workers;
  
  protected static PrintStream log = null;

  public static String getStatus(String uuid)
  {
    Gson json = new Gson();
    synchronized(uuidStatus)
    {
      if( !uuidStatus.containsKey(uuid))
      {
        uuidStatus.put(uuid, new Response("Download Not Started.", false));
      }
      return json.toJson(uuidStatus.get(uuid));
    }
  }
  
  public static void setDone(String uuid)
  {
    synchronized(uuidStatus)
    {
      uuidStatus.put(uuid, new Response("Download Complete.",true));
    }
  }
  
  public static void setFailure(String uuid)
  {
    synchronized(uuidStatus)
    {
      uuidStatus.put(uuid, new Response("Download Failed.",false));      
    }
  }
  
  public static void setStatus(String uuid, Response r)
  {
    synchronized(uuidStatus)
    {
      uuidStatus.put(uuid, r);
    }
  }
  
  /* print to the log file */
  protected static void log(String s) {
    synchronized (log) {
      log.println(s);
      log.flush();
    }
  }
  

  static void printProps() {
    verbosePrint("Port = " + port);
    verbosePrint("Timeout = " + timeout);
    verbosePrint("Workers = " + workers);
  }
  
  /**
   * setup the CLI parser
   * @param args - the arguments from main
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
    CmdLineParser.Option portOption = parser.addHelp(
        parser.addIntegerOption('p', "port"), 
        "The port that the server will listen on.");
    CmdLineParser.Option workersOption = parser.addHelp(
        parser.addIntegerOption('w', "workers"), 
        "The number of worker threads the server will use to handle requests.");
    CmdLineParser.Option timeoutOption = parser.addHelp(
        parser.addIntegerOption('t', "timeout"), 
        "The amount of time to keep a connection open (0 for unlimited)");
    CmdLineParser.Option logOption = parser.addHelp(
        parser.addStringOption('o', "log"),
        "Log the server data to a specified file (stdout of not specified).");

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
    port = (Integer) parser.getOptionValue(portOption, 8080);
    workers = (Integer) parser.getOptionValue(workersOption, 5);
    timeout = (Integer) parser.getOptionValue(timeoutOption, 0);
    logFile = (String) parser.getOptionValue(logOption, "");
    
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
    
    // Load the configuration file into the Configurator
    if (loadConfig != "") {
      verbosePrint("Loading Config: " + loadConfig);
      configurator.load(loadConfig);
      
      port = Integer.parseInt(configurator.getProps().getProperty(
        "serverPort"));
      workers = Integer.parseInt(configurator.getProps().getProperty(
        "serverWorkers"));
      timeout = Integer.parseInt(configurator.getProps().getProperty(
        "serverTimeout"));
    }
    
    gridServiceUrl = configurator.getProps().getProperty(
      "gridServiceUrl");
    clientDownloadLocation = configurator.getProps().getProperty(
      "clientDownloadLocation");
    
  }
  
  public static NBIASimpleClient setupClient()
  {
    return new NBIASimpleClient(gridServiceUrl, clientDownloadLocation);
  }

  public static void main(String[] args) throws Exception {
    
    setupParser(args);
    
    loadConfiguration();

    if( logFile == "")
    {
      log = System.out;
    }
    else
    {
      File file = new File(logFile);
      log = new PrintStream(file);
    }
    
    // Save the configuration
    if (saveConfig != "") {
      verbosePrint("Saving Config: " + saveConfig);
      configurator.save(saveConfig);
    }
    
    printProps();
    
    /* start worker threads */
    for (int i = 0; i < workers; ++i) {
      Worker w = new Worker();
      (new Thread(w, "worker #" + i)).start();
      threads.addElement(w);
    }

    ServerSocket ss = new ServerSocket(port);
    while (true) {

      Socket s = ss.accept();

      Worker w = null;
      synchronized (threads) {
        if (threads.isEmpty()) {
          Worker ws = new Worker();
          ws.setSocket(s);
          (new Thread(ws, "additional worker")).start();
        } else {
          w = (Worker) threads.elementAt(0);
          threads.removeElementAt(0);
          w.setSocket(s);
        }
      }
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

class Response
{  
  public String message;
  public boolean ok;
  Response()
  {
  }
  Response(String m, boolean ok)
  {
    this.message = new String(m);
    this.ok = ok;
  }
}

class Worker extends NBIAAdapterHTTPServer implements HttpConstants, Runnable {
  final static int BUF_SIZE = 2048;

  static final byte[] EOL = { (byte) '\r', (byte) '\n' };

  /* buffer to use for requests */
  byte[] buf;
  /* Socket to client we're handling */
  private Socket s;

  Worker() {
    buf = new byte[BUF_SIZE];
    s = null;
  }

  synchronized void setSocket(Socket s) {
    this.s = s;
    notify();
  }

  public synchronized void run() {
    while (true) {
      if (s == null) {
        /* nothing to do */
        try {
          wait();
        } catch (InterruptedException e) {
          /* should not happen */
          continue;
        }
      }
      try {
        handleClient();
      } catch (Exception e) {
        e.printStackTrace();
      }
      /*
       * go back in wait queue if there's fewer than numHandler connections.
       */
      s = null;
      Vector<Worker> pool = NBIAAdapterHTTPServer.threads;
      synchronized (pool) {
        if (pool.size() >= NBIAAdapterHTTPServer.workers) {
          /* too many threads, exit this one */
          return;
        } else {
          pool.addElement(this);
        }
      }
    }
  }

  void handleClient() throws IOException {
    InputStream is = new BufferedInputStream(s.getInputStream());
    PrintStream ps = new PrintStream(s.getOutputStream());
    /*
     * we will only block in read for this many milliseconds before we fail with
     * java.io.InterruptedIOException, at which point we will abandon the
     * connection.
     */
    s.setSoTimeout(NBIAAdapterHTTPServer.timeout);
    s.setTcpNoDelay(true);
    /* zero out the buffer from last time */
    for (int i = 0; i < BUF_SIZE; i++) {
      buf[i] = 0;
    }
    try {
      /*
       * We only support HTTP GET/HEAD, and don't support any fancy HTTP
       * options, so we're only interested really in the first line.
       */
      int nread = 0, r = 0;

      while (nread < BUF_SIZE) {
        r = is.read(buf, nread, BUF_SIZE - nread);
        if (r == -1) {
          /* EOF */
          return;
        }
        boolean ready = false;
        int i = nread;
        nread += r;
        for (; i < nread; i++) {
          if (buf[i] == (byte) '\n' || buf[i] == (byte) '\r') {
            ready = true; // read one line
            break;
          }
        }
        if (ready) {
          break;
        }
      }

      /* beginning of file name */
      int index;
      if (buf[0] == (byte) 'G' && buf[1] == (byte) 'E' && buf[2] == (byte) 'T'
          && buf[3] == (byte) ' ') {
        index = 4;
      } else {
        /* we don't support this method */
        ps.print("HTTP/1.0 " + HTTP_BAD_METHOD + " unsupported method type: ");
        ps.write(buf, 0, 5);
        ps.write(EOL);
        ps.flush();
        s.close();
        return;
      }

      int bound = index;
      for (int i = index; i < nread; ++i) {
        if (buf[i] == (byte) ' ') {
          bound = i;
          break;
        }
      }
      byte[] target = new byte[(bound - index) + 1];
      int targetIndex = 0;
      for (int i = index; i < bound; ++i) {
        target[targetIndex++] = buf[i];
      }
      
      String strTarget = new String(target);
      String[] tokens = strTarget.split("/");
      if( tokens[1].equals("fetch") && tokens.length >= 3)
      {
        fetchUUID(ps,tokens[2]);
      }
      if( tokens[1].equals("status") && tokens.length >= 3)
      {
        statusUUID(ps,tokens[2]);
      }
      else
      {
        send404(ps, target);
      }
      log(new String(target));

    } finally {
      s.close();
    }
  }
  
  protected void fetchUUID(PrintStream ps, String uuid)
  {
    log("From " + s.getInetAddress().getHostAddress());
    log("Fetching " + uuid);
    NBIASimpleClient nbia = setupClient();
    try {
      setStatus(uuid, new Response("Download Started", false));
      sendStatus(ps,uuid);
      nbia.fetchData(uuid, ""); // THE LONG PROCESS
      setDone(uuid);
    } catch (AxisFault e) {
      String err = "Internal Server Error at NBIA Site.";
      log(err);
      setStatus(uuid, new Response(err,false));
      e.printStackTrace(log);
    } catch(IOException e) {
      String err = "IOException when writing to socket stream.";
      log(err);
      setStatus(uuid, new Response(err,false));
      e.printStackTrace(log);
    } catch (Exception e) {
      String err = "Unknown Server Error";
      log(err);
      setStatus(uuid, new Response(err,false));
      e.printStackTrace(log);
    }
  }
  
  protected void statusUUID(PrintStream ps, String uuid)
  {
    log("From " + s.getInetAddress().getHostAddress());
    log("Getting status for " + uuid);
    try {
      sendStatus(ps,uuid);
    } catch(IOException e) {
      log("IOException when writing to socket stream.");
      e.printStackTrace(log);
    }
  }
  
  /**
   * Helper function for sending the download status in a HTTP reply.
   * @param ps - the socket's stream object
   * @param uuid - the uuid of the dataset
   * @throws IOException
   */
  protected void sendStatus(PrintStream ps, String uuid) throws IOException
  {
    String response = getStatus(uuid);
    ps.print("HTTP/1.0 " + HTTP_OK + " OK");
    ps.write(EOL);
    ps.print("Server: " + serverName);
    ps.write(EOL);
    ps.print("Last Modified: " + new Date());
    ps.write(EOL);
    ps.print("Content-Type: application/json");
    ps.write(EOL);      
    ps.print("Content-length: " + response.length());
    ps.write(EOL);
    ps.write(EOL);
    ps.print(response);
    ps.print(EOL);
    ps.flush();
    s.close();
  }
  
  /**
   * Send a 404 to the server if we don't know how to handle the request.
   * @param ps
   * @param buf
   * @throws IOException
   */
  void send404(PrintStream ps, byte[] buf) throws IOException {
    log("From " + s.getInetAddress().getHostAddress());
    log("Asking for " + new String(buf));
    String notFound = "Not Found\n\n"
        + "The requested resource was not found.\n";
    ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
    ps.write(EOL);
    ps.print("Server: " + serverName);
    ps.write(EOL);
    ps.print("Last Modified: " + new Date());
    ps.write(EOL);
    ps.print("Content-length: " + notFound.length());
    ps.write(EOL);
    ps.write(EOL);
    ps.print(notFound);
    ps.print(EOL);
    ps.flush();
    s.close();
  }

}
