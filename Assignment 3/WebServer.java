
// Jack Yang UCID: 30062393
/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * 
 * @author 	Majid Ghaderi
 * @version	2021
 *
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class WebServer extends Thread {

    // global logger object, configures in the driver class
    private static final Logger logger = Logger.getLogger("WebServer");

    public int port;
    public boolean notshutdown = true;

    protected ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * Constructor to initialize the web server
     * 
     * @param port The server port at which the web server listens > 1024
     * 
     */
    public WebServer(int port) {
        this.port = port;
    }

    /**
     * Main web server method. The web server remains in listening mode and accepts
     * connection requests from clients until the shutdown method is called.
     *
     */
    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            // whilenot shutdown do--------------------------------------------------------
            while (true) {
                try {
                    // Listen for connection requests from clients-------------------------
                    // Accept a new connection request-------------------------------------
                    Socket socket = serverSocket.accept();

                    // print ip address and port
                    System.out.println(socket.getRemoteSocketAddress());
                    
                    // Spawn a worker thread to handle the new connection------------------
                    this.threadPool.execute(new workerThread(socket));
                    
                    serverSocket.setSoTimeout(500);
                } catch (SocketTimeoutException s) {
                    if (!notshutdown) break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.toString(), e);
                }
            }
            // end while-------------------------------------------------------------------
            // Wait for worker threads to finish-------------------------------------------
            this.threadPool.shutdown();
            this.threadPool.awaitTermination(60, TimeUnit.SECONDS);
            this.threadPool.shutdownNow();
            // Close the server socket and clean up----------------------------------------
            serverSocket.close();

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    /**
     * separate class worker thread to handle new connections
	 *
     */
    class workerThread implements Runnable {

        Socket socket;

        /**
         * Constructor to initialize the class.
         * 
         * @param socket the socket
         * 
         *
         */
        public workerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            InputStream input;
            OutputStream output;
            // Parse the HTTP request------------------------------------------------------
            // Read the server response status and header lines
            String readString;
            String urlPath = null;
            String requestString = null;
            String requestHost = null;
            // buffer to read one byte at a time
            byte[] buff = new byte[1];
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
                // Reads each line of the response
                while (true) {
                    int count = 0;
                    // buffer to read one line
                    byte[] buff1 = new byte[16000];

                    // read a line
                    while (true) {
                        // read one byte
                        input.read(buff);
                        // append to line buffer
                        System.arraycopy(buff, 0, buff1, count, 1);
                        count += 1;

                        // stop reading at end of line == "\r\n"
                        if ((readString = new String(buff, 0, 1, "US-ASCII")).charAt(0) == '\r') {
                            input.read(buff);
                            // append to line buffer
                            System.arraycopy(buff, 0, buff1, count, 1);
                            count += 1;
                            if ((readString = new String(buff, 0, 1, "US-ASCII")).charAt(0) == '\n')
                                break;
                        }
                    }

                    // convert line of bytes to line of string
                    readString = new String(buff1, 0, count, "US-ASCII");
                    // print the response line
                    System.out.print(readString);
                    // parse url Path
                    if (readString.contains("HTTP")) {
                        // get entire GET request string
                        requestString = readString;

                        // parse object path from request string
                        String[] urlstring = readString.split(" ");
                        urlPath = urlstring[1].substring(1);
                    }
                    // get Host: part of the request header
                    if (readString.contains("Host")) requestHost = readString;
                    
                    // stop reading at end of header
                    if (readString.equals("\r\n")) break;
                }


                // Send response
                // Instantiates a new PrintWriter
                PrintWriter wtr = new PrintWriter(output);
                File file = new File(urlPath);
                // if format error then----------------------------------------------------
                if (!requestString.matches("GET /(.*) HTTP/1.1\r\n") || !requestHost.matches("Host: (.*)\r\n")) {
                    // Send bad request error response-------------------------------------
                    wtr.print("HTTP/1.1 400 Bad Request\r\n");
                    System.out.println("HTTP/1.1 400 Bad Request");
                    // Prints the header to the output stream
                    // get current time
                    SimpleDateFormat date = new SimpleDateFormat("EE, dd MMM yyyy hh:mm:ss zzz");
                    Date now = new Date();
                    String currentTime = date.format(now.getTime()).replaceAll("\\.", "");
                    wtr.print("Date: " + currentTime + "\r\n");
                    System.out.println("Date: " + currentTime);
                    // send server name?
                    wtr.print("Server: server.server.jy\r\n");
                    System.out.println("Server: server.server.jy");
                    // connection close
                    wtr.print("Connection: close\r\n");
                    System.out.println("Connection: close");
                    wtr.print("\r\n");
                    System.out.println("");
                    wtr.flush();
                }
                // else if non-existence object then---------------------------------------
                else if (!file.exists() || file.isDirectory()) {
                    // Send not found error response---------------------------------------
                    wtr.print("HTTP/1.1 404 Not Found\r\n");
                    System.out.println("HTTP/1.1 404 Not Found");
                    // Prints the header to the output stream
                    // get current time
                    SimpleDateFormat date = new SimpleDateFormat("EE, dd MMM yyyy hh:mm:ss zzz");
                    Date now = new Date();
                    String currentTime = date.format(now.getTime()).replaceAll("\\.", "");
                    wtr.print("Date: " + currentTime + "\r\n");
                    System.out.println("Date: " + currentTime);
                    // send server name?
                    wtr.print("Server: server.server.jy\r\n");
                    System.out.println("Server: server.server.jy");
                    // connection close
                    wtr.print("Connection: close\r\n");
                    System.out.println("Connection: close");
                    wtr.print("\r\n");
                    System.out.println("");
                    wtr.flush();
                } else { // else-----------------------------------------------------------
                    // Send Ok response header lines---------------------------------------
                    wtr.print("HTTP/1.1 200 OK\r\n");
                    System.out.println("HTTP/1.1 200 OK");
                    // get current time
                    SimpleDateFormat date = new SimpleDateFormat("EE, dd MMM yyyy hh:mm:ss zzz");
                    Date now = new Date();
                    String currentTime = date.format(now.getTime()).replaceAll("\\.", "");
                    wtr.print("Date: " + currentTime + "\r\n");
                    System.out.println("Date: " + currentTime);
                    // send server name?
                    wtr.print("Server: server.server.jy\r\n");
                    System.out.println("Server: server.server.jy");
                    // get last modified date
                    String lastModified = date.format(file.lastModified()).replaceAll("\\.", "");
                    wtr.print("Last-Modified: " + lastModified + "\r\n");
                    System.out.println("Last-Modified: " + lastModified);
                    // get file size
                    long contentLength = file.length();
                    wtr.print("Content-Length: " + contentLength + "\r\n");
                    System.out.println("Content-Length: " + contentLength);
                    // get content type
                    Path filepath = Paths.get(urlPath);
                    String contentType = Files.probeContentType(filepath);
                    wtr.print("Content-Type: " + contentType + "\r\n");
                    System.out.println("Content-Type: " + contentType);
                    // connection close
                    wtr.print("Connection: close\r\n");
                    System.out.println("Connection: close");
                    wtr.print("\r\n");
                    System.out.println("");
                    wtr.flush();
                    // Send the object content---------------------------------------------
                    FileInputStream inFile;
                    try {
                        // get the fileinputstream to read from file specified
                        inFile = new FileInputStream(urlPath);

                        // create buff array of size 1000000000
                        byte[] buff3 = new byte[1000000000];
                        int readBytes;

                        // Read the input file until EOF
                        while ((readBytes = inFile.read(buff3)) != -1) {
                            // write to socket output
                            output.write(buff3, 0, readBytes);
                            output.flush();
                        }
                        inFile.close();
                        
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, e.toString(), e);
                    }
                    wtr.flush();
                }
                // end if------------------------------------------------------------------
                // Close the socket and clean up-------------------------------------------
                socket.close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        }
    }
	

    /**
     * Signals the web server to shutdown.
	 *
     */
	public void shutdown() {
        this.notshutdown = false;
    }
	
}
