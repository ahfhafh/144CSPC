// JACK YANG 30062393
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
import java.net.Socket;

public class GzipClient {

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
		Socket socket;					// initialize socket
		InputStream inputstream;		// initialize inputstream
		OutputStream outputstream;		// initialize outputstream
		try {
			socket = new Socket(serverName, serverPort);		// establish a connection 
			inputstream = socket.getInputStream();				// get inputstream from socket connection
			outputstream = socket.getOutputStream();			// get outputstream from socket connection

			Thread wThread = new Thread(new writeTo(outputstream, inName));		// create write thread
			Thread rThread = new Thread(new readFrom(inputstream, outName));	// create read thread
			wThread.start(); // start write to socket thread
			rThread.start(); // start read from socket thread

			try {
				wThread.join();					// wait for write thread to join
				socket.shutdownOutput();		// shutdown socket output after writing to socket has finished
				rThread.join();					// wait for read thread to finish reading from socket
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
			socket.close();						// close the socket
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
	}

	// inner class to read from file and write to socket
	class writeTo implements Runnable {
		OutputStream outputstream;
		String inName;

		/**
		 * Constructor to initialize the class.
		 * 
		 * @param outputstream the socket outputstream
		 * @param inName file name to read from
		 * 
		 *
		 */
		public writeTo(OutputStream outputstream, String inName) {
			this.outputstream = outputstream;
			this.inName = inName;
		}

		@Override
		public void run() {
			FileInputStream inFile;
			try {
				// get the fileinputstream to read from file specified
				inFile = new FileInputStream(inName);	
	
				int readBytes;
				// create buff array of size bufferSize
				byte[] buff = new byte[bufferSize];		
	
				// Read the input file until EOF 
				while ((readBytes = inFile.read(buff)) != -1) {
					System.out.println("W" + readBytes);
					// write to socket output
					outputstream.write(buff, 0, readBytes);
					outputstream.flush();
				}
				inFile.close();
	
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}
	}

	// inner class to read from socket and write to file
	class readFrom implements Runnable {
		InputStream inputstream;
		String outName;

		/**
		 * Constructor to initialize the class.
		 * 
		 * @param inputstream the socket inputstream
		 * @param outName file name to write to
		 * 
		 *
		 */
		public readFrom(InputStream inputstream, String outName) {
			this.inputstream = inputstream;
			this.outName = outName;
		}

		@Override
		public void run() {
			FileOutputStream outFile;
			try {
				// write to file specified
				outFile = new FileOutputStream(outName);	

				int readBytes;
				// create buff array of size bufferSize
				byte[] buff = new byte[bufferSize];		

				// read from socket until server stops sending
				while ((readBytes = inputstream.read(buff)) != -1) {
					// Write to output file
					System.out.println("R" + readBytes);
					outFile.write(buff, 0, readBytes);
					outFile.flush();
				}
				outFile.close();

			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}
	}
}
