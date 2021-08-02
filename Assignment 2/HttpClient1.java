// Jack Yang UCID: 30062393
/**
 * HttpClient Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 */

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class HttpClient {

	private static final Logger logger = Logger.getLogger("HttpClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public HttpClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void get(String url) {
        // Parse the input url to extract server address and object path-------------------
        String urlAddress;
        int urlPort;
        String urlPath;
        // if string contains ":", there is a port
        if (url.contains(":")) {
            String[] urlstring = url.split(":");
            urlAddress = urlstring[0];
            String portandpath = urlstring[1];
            if (!(portandpath.contains("/"))) {
                System.out.println("Wrong URL");
                System.exit(0);
            }
            urlstring = portandpath.split("/", 2);
            urlPort = Integer.parseInt(urlstring[0]);
            urlPath = urlstring[1];
        } else { // else default port to 80
            if (!(url.contains("/"))) {
                System.out.println("Wrong URL");
                System.exit(0);
            }
            String[] urlstring = url.split("/", 2);
            urlAddress = urlstring[0];
            urlPath = urlstring[1];
            urlPort = 80;
        }
        

        // Establish a TCP connection with the server--------------------------------------
        Socket socket;
        InputStream input;
        OutputStream output;
        try {
            socket = new Socket(urlAddress, urlPort);
            input = socket.getInputStream();
            output = socket.getOutputStream();

            // Send a GET request for the specified object---------------------------------
            // Instantiates a new PrintWriter
            PrintWriter wtr = new PrintWriter(output);

            // Prints the request string to the output stream
            wtr.println("GET /" + urlPath + " HTTP/1.1");
            System.out.println("\nGET /" + urlPath + " HTTP/1.1");
            wtr.println("Host: " + urlAddress);
            System.out.println("Host: " + urlAddress);
            wtr.println("Connection: close");
            System.out.println("Connection: close");
            wtr.println("");
            System.out.println("");
            wtr.flush();
            
            // Read the server response status and header lines----------------------------
            String readString;
            int responseCode = 0;
            int contentLength = 0;
            // buffer to read one byte at a time
            byte[] buff = new byte[1];

            // Reads each line of the response
            while (true) {
                int count = 0;
                // buffer to read one line
                byte[] buff1 = new byte[8000];

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
                // parse response code
                if (readString.contains("HTTP")) {
                    String[] response = readString.split(" ");
                    responseCode = Integer.parseInt(response[1]);
                }
                // parse content length
                if (readString.contains("Content-Length")) {
                    contentLength = Integer.parseInt(readString.replaceAll("[^0-9?!\\.]",""));
                }
                // stop reading at end of header
                if (readString.equals(System.getProperty("line.separator"))) break;
            }

            // if response status is OK then-----------------------------------------------
            if (200 <= responseCode && 300 > responseCode) {
                // Create the local file with the object name------------------------------
                String[] urlObject = urlPath.split("/");
                String urlFile = urlObject[urlObject.length - 1];
                FileOutputStream outFile = new FileOutputStream(urlFile);       
                byte[] contentBuff = new byte[contentLength];
                int readBytes;
                // while not end of input stream do----------------------------------------
                while ((readBytes = input.read(contentBuff)) != -1) {
                    // Read from the socket and write to the local file--------------------
                    outFile.write(contentBuff, 0, readBytes);
                    outFile.flush();
                } 
                outFile.close();
                // end while---------------------------------------------------------------
            // end if----------------------------------------------------------------------
            } else {
                System.out.println("Bad Response Code: " + responseCode);
                System.exit(0);
            }
            // Clean up (e.g., close the streams and socket)-------------------------------
            socket.close();

        } catch (Exception e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}

    }

}
