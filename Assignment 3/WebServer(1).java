
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
import java.util.logging.*;

public class WebServer2 extends Thread {

    // global logger object, configures in the driver class
    private static final Logger logger = Logger.getLogger("WebServer");

    public int port;
    public boolean notshutdown = true;

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
            // whilenot shutdown do---------------------------------
            while (true) {
                // Listen for connection requests from clients---------------------------------
                // Accept a new connection request---------------------------------
                // Spawn a worker thread to handle the new connection---------------------------------
                try {
                    Socket socket = serverSocket.accept();

                    new Thread(new workerThread(socket)).start();
                    
                    serverSocket.setSoTimeout(500);
                } catch (SocketTimeoutException s) {
                    if (!notshutdown) {
                        System.out.println("Socket timed out!");
                        break;
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, e.toString(), e);
                }
            }
            // end while---------------------------------
            // Wait for worker threads to finish---------------------------------
            // Close the server socket and clean up---------------------------------
            serverSocket.close();

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

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
            // Parse the HTTP request---------------------------------
            // Read the server response status and header lines
            String readString;
            String urlPath = null;
            String requestString = null;
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
                        requestString = readString;
                        // System.out.println("requestString: " + requestString);

                        String[] urlstring = readString.split(" ");
                        urlPath = urlstring[1].substring(1);
                        // System.out.println("urlPath: " + urlPath);
                    }
                    // stop reading at end of header
                    if (readString.equals("\r\n"))
                        break;
                }

                // Send response
                // Instantiates a new PrintWriter
                PrintWriter wtr = new PrintWriter(output);
                File file = new File(urlPath);
                // if format error then---------------------------------
                if (!requestString.matches("GET /(.*) HTTP/1.1\r\n")) { System.out.println("400");
                    // Send bad request error response---------------------------------
                    wtr.println("HTTP/1.1 400 Bad Request");
                    // Prints the header to the output stream
                    wtr.println("date");
                    wtr.println("server");
                    wtr.println("Connection: close");
                    wtr.println("\r\n");
                    wtr.flush();
                }
                // else if non-existence object then---------------------------------
                else if (!file.exists() || file.isDirectory()) { System.out.println("404");
                    // Send not found error response---------------------------------
                    wtr.println("HTTP/1.1 404 Not Found");
                    // Prints the header to the output stream
                    wtr.println("date");
                    wtr.println("server");
                    wtr.println("Connection: close");
                    wtr.println("\r\n");
                    wtr.flush();
                } else { // else---------------------------------
                    // Send Ok response header lines---------------------------------
                    wtr.println("HTTP/1.1 200 OK");
                    System.out.println("HTTP/1.1 200 OK");
                    // get current time
                    SimpleDateFormat date = new SimpleDateFormat("EE, dd MMM yyyy hh:mm:ss zzz");
                    Date now = new Date();
                    String currentTime = date.format(now.getTime()).replaceAll("\\.", "");
                    wtr.println("Date: " + currentTime);
                    System.out.println("Date: " + currentTime);
                    wtr.println("Server: server.server.jy");
                    System.out.println("Server: server.server.jy");
                    // get last modified date
                    String lastModified = date.format(file.lastModified()).replaceAll("\\.", "");
                    wtr.println("Last-Modified: " + lastModified);
                    System.out.println("Last-Modified: " + lastModified);
                    // get file size
                    long contentLength = file.length();
                    wtr.println("Content-Length: " + contentLength);
                    System.out.println("Content-Length: " + contentLength);
                    // get content type
                    //Path filepath = Paths.get(urlPath);
                    //String sfdks = probeContentType(filepath);
                    wtr.println("Content-Type: ");
                    wtr.println("Connection: close");
                    System.out.println("Connection: close");
                    wtr.print("\r\n");
                    System.out.println("");
                    wtr.flush();
                    // Send the object content---------------------------------
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
                    wtr.print("\r\n");
                    wtr.flush();
                }
                // end if---------------------------------
                // Close the socket and clean up---------------------------------
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
