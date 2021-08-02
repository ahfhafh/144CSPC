
/**
 * GzipClient Class
 * 
 * CPSC 441
 * Assignment 1
 * 
 *
 */

import java.io.*;
import java.util.logging.*;
import java.util.zip.GZIPInputStream;
import java.net.Socket;

// javac *.java
// java GzipDriver fileName -i .\files\small.txt -o smallhehe.txt -p 8888

public class pGzipClient {

	private static final Logger logger = Logger.getLogger("GzipClient"); // global logger

	private static String serverName;
	private static int serverPort, bufferSize;
	private String inName, outName;

	/**
	 * Constructor to initialize the class.
	 * 
	 * @param serverName remote server name
	 * @param serverPort remote server port number
	 * @param bufferSize buffer size used for read/write
	 * 
	 *                   csx.cs.ucalgary.ca
	 */
	public GzipClient(String serverName, int serverPort, int bufferSize) {
		GzipClient.serverName = serverName;
		GzipClient.serverPort = serverPort;
		GzipClient.bufferSize = bufferSize;
	}

	Socket socket;
	GZIPInputStream inputStream;
	OutputStream outputStream;

	FileInputStream inFile;
	FileOutputStream outFile;

	/**
	 * Compress the specified file via the remote server.
	 * 
	 * This is the main method that reads the local file and communicates with the
	 * remote server to create a GZIP compressed version of the file. The parameter
	 * inFile specifies the name of the input file, while the parameter outFile
	 * specifies the name of the compressed file to be created.
	 * 
	 * @param inName  name of the input file to be compressed
	 * @param outName name of the output compressed file
	 */
	public void gzip(String inName, String outName) {
		try {
			socket = new Socket(serverName, serverPort);
			outputStream = socket.getOutputStream();
			inFile = new FileInputStream("files/small.txt");
			inputStream = new GZIPInputStream(socket.getInputStream());
			outFile = new FileOutputStream("files/smallheh.txt");

			int readBytes = 0;
			byte[] buff = new byte[bufferSize];

			// Read the input file & write to socket output
			while ((readBytes = inFile.read(buff)) != -1) {
				System.out.println("W" + readBytes);
				outputStream.write(buff, 0, readBytes);
				outputStream.flush();	
			}
			inFile.close();
			socket.shutdownOutput();

			while ((readBytes = inputStream.read(buff)) != -1) {
				// Read from socket
				// Write to output file
				System.out.println("R" + readBytes);
				outFile.write(buff, 0, readBytes);
				outFile.flush();
			}

			inputStream.close();
			outputStream.close();
			outFile.close();
			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
			logger.log( Level.SEVERE, e.toString(), e );
			System.out.println("Error3: " + e.getMessage());
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		// FileInputStream FILE = new FileInputStream("files/large.jpg");
		// FileOutputStream FILEE = new FileOutputStream("files/smallheh.jpg");

		// int readBytes = 0;
		// byte[] buff = new byte[89080];

		// try {
		// 	while ((readBytes = FILE.read(buff)) != -1) {
		// 		System.out.println("W" + readBytes);
		// 		System.out.println("R" + readBytes);
		// 		FILEE.write(buff, 0, readBytes);
		// 		FILEE.flush();
		// 	}

		// 	FILE.close();
		// 	FILEE.close();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
	}
}

