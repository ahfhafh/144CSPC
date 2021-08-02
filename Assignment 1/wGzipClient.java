
/**
 * GzipClient Class
 * 
 * CPSC 441
 * Assignment 1
 * 
 * java GzipDriver -i files/large.jpg -p 2575 -s csx.cs.ucalgary.ca -b 5000
 */

import java.io.*;
import java.util.logging.*;
import java.util.zip.GZIPInputStream;
import java.net.Socket;

public class wGzipClient {

	private static final Logger logger = Logger.getLogger("GzipClient"); // global logger

	private static String serverName;
	private static int serverPort, bufferSize;

	/**
	 * Constructor to initialize the class.
	 * 
	 * @param serverName remote server name
	 * @param serverPort remote server port number
	 * @param bufferSize buffer size used for read/write
	 * 
	 *
	 */
	public GzipClient(String serverName, int serverPort, int bufferSize) {
		GzipClient.serverName = serverName;
		GzipClient.serverPort = serverPort;
		GzipClient.bufferSize = bufferSize;
	}

	/**
	 * Compress the specified file via the remote server.
     *
	 * @param inName  name of the input file to be compressed
	 * @param outName name of the output compressed file
	 */
	public void gzip(String inName, String outName) {
        Socket socket;
        InputStream inputStream;
        OutputStream outputStream;
    
        FileInputStream inFile;
        FileOutputStream outFile;
    
		try {
			socket = new Socket(serverName, serverPort);
			outputStream = socket.getOutputStream();
			inFile = new FileInputStream(inName);
			inputStream = socket.getInputStream();
			outFile = new FileOutputStream(outName);

			// amount of bytes read reading from input file
			int readBytes;
			// amount of bytes read reading from socket
            int readBytes2;
			// buffer array of size bufferSize
			byte[] buff = new byte[bufferSize];

			int stopCounter = 0;

            // Read the input file & write to socket output
            while (stopCounter != 2) {
				if (stopCounter != 1) {
					if ((readBytes = inFile.read(buff)) != -1) {
						System.out.println("W" + readBytes);
						outputStream.write(buff, 0, readBytes);
						outputStream.flush();
					} else {
						socket.shutdownOutput();
						stopCounter ++;
					}
				}

				System.out.println("stopCounter: " + stopCounter);
                
                // read from socket and write to output file
				// must read up to readBytes of data
                int currentRead = 0;
                while (currentRead < bufferSize) {
					readBytes2 = inputStream.read(buff, currentRead, bufferSize - currentRead);
                    currentRead += readBytes2;
					System.out.println("readBytes2: " + readBytes2);
					System.out.println("currentRead: " + currentRead);
                }
                System.out.println("R" + currentRead);
                outFile.write(buff, 0, currentRead);
                outFile.flush();
				if (currentRead != bufferSize) {
					stopCounter ++;
				}
            }

            inFile.close();
            outFile.close();
			socket.close();

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.toString(), e);
			System.out.println("Error: " + e.getMessage());
		}
	}

}

