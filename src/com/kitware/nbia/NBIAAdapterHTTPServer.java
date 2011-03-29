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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class NBIAAdapterHTTPServer implements HttpConstants {

  /* static class data/methods */

  /* print to stdout */
  protected static void p(String s) {
    System.out.println(s);
  }

  /* print to the log file */
  protected static void log(String s) {
    synchronized (log) {
      log.println(s);
      log.flush();
    }
  }

  protected String serverName = "NBIAAdapter";

  static PrintStream log = null;
  /*
   * our server's configuration information is stored in these properties
   */
  protected static Properties props = new Properties();

  /* Where worker threads stand idle */
  static Vector<Worker> threads = new Vector<Worker>();

  /* the web server's virtual root */
  static File root;

  /* timeout on client connections */
  static int timeout = 0;

  /* max # worker threads */
  static int workers = 5;

  /* load www-server.properties from java.home */
  static void loadProps() throws IOException {
    File f = new File(System.getProperty("java.home") + File.separator + "lib"
        + File.separator + "www-server.properties");
    if (f.exists()) {
      InputStream is = new BufferedInputStream(new FileInputStream(f));
      props.load(is);
      is.close();
      String r = props.getProperty("root");
      if (r != null) {
        root = new File(r);
        if (!root.exists()) {
          throw new Error(root + " doesn't exist as server root");
        }
      }
      r = props.getProperty("timeout");
      if (r != null) {
        timeout = Integer.parseInt(r);
      }
      r = props.getProperty("workers");
      if (r != null) {
        workers = Integer.parseInt(r);
      }
      r = props.getProperty("log");
      if (r != null) {
        p("opening log file: " + r);
        log = new PrintStream(new BufferedOutputStream(new FileOutputStream(r)));
      }
    }

    /* if no properties were specified, choose defaults */
    if (root == null) {
      root = new File(System.getProperty("user.dir"));
    }
    if (timeout <= 1000) {
      timeout = 5000;
    }
    if (workers < 25) {
      workers = 5;
    }
    if (log == null) {
      p("logging to stdout");
      log = System.out;
    }
  }

  static void printProps() {
    p("root=" + root);
    p("timeout=" + timeout);
    p("workers=" + workers);
  }

  public static void main(String[] a) throws Exception {
    int port = 8080;
    if (a.length > 0) {
      port = Integer.parseInt(a[0]);
    }
    loadProps();
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

      send404(ps, target);

    } finally {
      s.close();
    }
  }

  void send404(PrintStream ps, byte[] buf) throws IOException {
    log("From " + s.getInetAddress().getHostAddress());
    log("Asking for " + new String(buf));
    String notFound = "Not Found\n\n"
        + "The requested resource was not found.\n";
    ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
    ps.write(EOL);
    ps.print("Server: " + this.serverName);
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

interface HttpConstants {
  /** 2XX: generally "OK" */
  public static final int HTTP_OK = 200;
  public static final int HTTP_CREATED = 201;
  public static final int HTTP_ACCEPTED = 202;
  public static final int HTTP_NOT_AUTHORITATIVE = 203;
  public static final int HTTP_NO_CONTENT = 204;
  public static final int HTTP_RESET = 205;
  public static final int HTTP_PARTIAL = 206;

  /** 3XX: relocation/redirect */
  public static final int HTTP_MULT_CHOICE = 300;
  public static final int HTTP_MOVED_PERM = 301;
  public static final int HTTP_MOVED_TEMP = 302;
  public static final int HTTP_SEE_OTHER = 303;
  public static final int HTTP_NOT_MODIFIED = 304;
  public static final int HTTP_USE_PROXY = 305;

  /** 4XX: client error */
  public static final int HTTP_BAD_REQUEST = 400;
  public static final int HTTP_UNAUTHORIZED = 401;
  public static final int HTTP_PAYMENT_REQUIRED = 402;
  public static final int HTTP_FORBIDDEN = 403;
  public static final int HTTP_NOT_FOUND = 404;
  public static final int HTTP_BAD_METHOD = 405;
  public static final int HTTP_NOT_ACCEPTABLE = 406;
  public static final int HTTP_PROXY_AUTH = 407;
  public static final int HTTP_CLIENT_TIMEOUT = 408;
  public static final int HTTP_CONFLICT = 409;
  public static final int HTTP_GONE = 410;
  public static final int HTTP_LENGTH_REQUIRED = 411;
  public static final int HTTP_PRECON_FAILED = 412;
  public static final int HTTP_ENTITY_TOO_LARGE = 413;
  public static final int HTTP_REQ_TOO_LONG = 414;
  public static final int HTTP_UNSUPPORTED_TYPE = 415;

  /** 5XX: server error */
  public static final int HTTP_SERVER_ERROR = 500;
  public static final int HTTP_INTERNAL_ERROR = 501;
  public static final int HTTP_BAD_GATEWAY = 502;
  public static final int HTTP_UNAVAILABLE = 503;
  public static final int HTTP_GATEWAY_TIMEOUT = 504;
  public static final int HTTP_VERSION = 505;
}
