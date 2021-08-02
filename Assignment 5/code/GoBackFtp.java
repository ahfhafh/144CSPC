
/**
 * GoBackFtp Class
 * 
 * GoBackFtp implements a basic FTP application based on UDP data transmission.
 * It implements a Go-Back-N protocol. The window size is an input parameter.
 * 
 * @author 	Majid Ghaderi
 * @version	2021
 *
 */

import java.io.*;
import java.net.*;
import java.sql.ClientInfoStatus;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;


public class GoBackFtp {
	
	private int windowSize;
	private int rtoTimer;

	private int seqNum;
	private int ServerUDPport;

	static Timer timer;	
	static TimerTask task;

	private volatile boolean doneSending = false;
	private volatile boolean firstSent = false;

	ConcurrentLinkedQueue<DatagramPacket> queue = new ConcurrentLinkedQueue<DatagramPacket>();
  
	
	// global logger	
	private static final Logger logger = Logger.getLogger("GoBackFtp");

	/**
	 * Constructor to initialize the program 
	 * 
	 * @param windowSize	Size of the window for Go-Back_N in units of segments
	 * @param rtoTimer		The time-out interval for the retransmission timer
	 */
	public GoBackFtp(int windowSize, int rtoTimer) {
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;
	}


	/**
	 * Send the specified file to the specified remote server
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 * @throws FtpException If unrecoverable errors happen during file transfer
	 */
	public void send(String serverName, int serverPort, String fileName) throws FtpException {
		try {
			// Handshake with server
			// Open TCP and UDP sockets 
			Socket socket = new Socket(serverName, serverPort);
			DataInputStream dataInput = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());

			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(serverName);
			// Complete the handshake over TCP
			// Send the name of the file as a UTF encoded string
			File file = new File(fileName);
			dataOutput.writeUTF(fileName);

			// Send the length (in bytes) of the file as a long value
			long contentLength = file.length(); // get file size
			dataOutput.writeLong(contentLength);

			// Send the local UDP port number used for file transfer as an int value
			dataOutput.writeInt(clientSocket.getLocalPort());

			dataOutput.flush();

			// Receive the server UDP port number used for file transfer as an int value
			ServerUDPport = dataInput.readInt();

			// Receive the initial sequence number used by the server as an int value
			seqNum = dataInput.readInt();

			// Create the timer
			GoBackFtp.timer = new Timer();
			
			// Start the ACK receiving thread
			// Start the data sending thread
			Thread sendThread = new Thread(new sendThread(fileName, IPAddress, clientSocket));						// create send thread
			Thread receiveThread = new Thread(new receiveThread(clientSocket));										// create receive thread
			receiveThread.start();
			sendThread.start();

			// Wait for data sending thread to finish
			// Wait for the ACK receiving thread to finish
			sendThread.join();					// wait for sending thread to finish
			receiveThread.join();				// wait for receiving thread to finish
			
			// Shutdown the timer
			timer.cancel();
			timer.purge();
			// Close sockets
			socket.close();
			clientSocket.close();
				
		} catch(Exception e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
	}

	/**
	 * 
	 * ACKTimer, timer.
	 * 
	 *
	 */
	public class ACKTimer extends TimerTask {

		DatagramSocket clientSocket;

        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
         * 
         *
         */
        public ACKTimer(DatagramSocket clientSocket) {
            this.clientSocket = clientSocket;
        }

		@Override
		public void run() {
			FtpSegment seg;
			int segNum;
			try {
				System.out.println("timeout");
				
				// resend every packet in queue
				for (DatagramPacket pkt : queue) {
					seg = new FtpSegment(pkt);
					segNum = seg.getSeqNum();
					// print the sequence number of the packet being retransmitted
					System.out.println("retx    " + segNum);
					clientSocket.send(pkt);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}

	}


	/**
	 * 
	 * Sending thread.
	 * 
	 *
	 */
	public class sendThread implements Runnable {

		String fileName;
		InetAddress IPAddress;
		DatagramSocket clientSocket;

        /**
         * Constructor to initialize the class.
         * 
		 * @param fileName the name of the file
		 * @param IPAddress the IPAddress
         * @param clientSocket the socket
		 * 
         *
         */
        public sendThread(String fileName, InetAddress IPAddress, DatagramSocket clientSocket) {
			this.fileName = fileName;
            this.IPAddress = IPAddress;
			this.clientSocket = clientSocket;
        }

		@Override
		public void run() {
			try {
				int readBytes;
				FileInputStream inFile = new FileInputStream(fileName);
				byte[] payload = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
				FtpSegment seg;
				DatagramPacket pkt;
				// while not end of file do
				while ((readBytes = inFile.read(payload)) != -1) {
					// Read from the file and create a segment
					seg = new FtpSegment(seqNum, payload, readBytes);
					pkt = FtpSegment.makePacket(seg, IPAddress, ServerUDPport);
					
					// Wait while transmission queue is full
					while (queue.size() == windowSize) {
						Thread.yield();
					}
					// Send the segment and add it to the transmission queue
					queue.add(pkt);
					clientSocket.send(pkt);
					System.out.println("send    " + seqNum);
					seqNum++;
					// Start timer if the segment is first in the transmission queue
					if (queue.size() == 1) {
						start_timer(clientSocket);
						firstSent = true;
					}
				// end while
				}
				inFile.close();
				doneSending = true;
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}

	}
	
	/**
	 * 
	 * Receiving thread.
	 * 
	 *
	 */
	public class receiveThread implements Runnable {

		DatagramSocket clientSocket;

        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
		 * @param pkt the packet being sent
         * 
         *
         */
        public receiveThread(DatagramSocket clientSocket) {
			this.clientSocket = clientSocket;
        }

		@Override
		public void run() {
			try {
				int reccSegNum = seqNum; // reccSegNum is received ACK
				FtpSegment reccSeg; // segment and received segment
				byte[] receiveData = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				DatagramPacket headPkt; // the packet of the head of the queue
				FtpSegment queueSeg; // the segment of the headPkt
				int queueSegNum = seqNum; // the ACK number/sequence number of the headPkt
				// while sending thread not finished or transmission queue not empty do
				while (doneSending == false | (!queue.isEmpty())) {
					// Receive ACK
					try {
						clientSocket.receive(receivePacket);

						// get ACK from received packet
						reccSeg = new FtpSegment(receivePacket);
						reccSegNum = reccSeg.getSeqNum();
						System.out.println("ack     " + reccSegNum);

						// If ACK is valid then
						// peek from queue until a packet is read
						headPkt = queue.peek();
						while (headPkt == null) {
							headPkt = queue.peek();
						}
						// get the ACK from head packet in queue
						queueSeg = new FtpSegment(headPkt);
						queueSegNum = queueSeg.getSeqNum();
						// wait for the the timer to start for the first packet sent
						while (firstSent == false) {
							Thread.yield();
						}
						// check if ACK is valid
						if (reccSegNum >= queueSegNum + 1) {
							// Stop timer
							stop_timer();
							// Update the transmission queue based on ACK
							queue.poll();
							// Start timer if transmission queue is not empty
							if (!queue.isEmpty()) {
								start_timer(clientSocket);
							}
						// end if
						}

						clientSocket.setSoTimeout(50);
					} catch (SocketTimeoutException s) {
						if (doneSending == true && queue.isEmpty()) break;
					} 
				// end while
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}
	}

	/**
	 * Starts the timer.
	 * 
	 */
	public synchronized void start_timer(DatagramSocket clientSocket) {
		task = new ACKTimer(clientSocket);
		timer.scheduleAtFixedRate(task, rtoTimer, rtoTimer);
	}

	/**
	 * Stops the timer.
	 * 
	 */
	public synchronized void stop_timer() {
		task.cancel();
	}
} // end of class