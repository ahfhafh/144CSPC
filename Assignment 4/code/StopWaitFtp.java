

import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.*;


public class StopWaitFtp {
	
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger	

	public int timeout;
	public int seqNum;
	public int ServerUDPport;

	/**
	 * Constructor to initialize the program 
	 * 
	 * @param timeout		The time-out interval for the retransmission timer
	 */
	public StopWaitFtp(int timeout) {
		this.timeout = timeout;
	}


	/**
	 * Send the specified file to the specified remote server
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the remote server
	 * @throws FtpException If anything goes wrong while sending the file
	 */
	public void send(String serverName, int serverPort, String fileName) throws FtpException {
		try {
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

			// while not end of file do 
			FileInputStream inFile = new FileInputStream(fileName);
			byte[] payload = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
			byte[] receiveData = new byte[FtpSegment.MAX_PAYLOAD_SIZE];
			Timer timer = new Timer();
			TimerTask task;
			int reccSegNum = seqNum; // reccSegNum is received ACK
			int readBytes;
			FtpSegment seg, reccSeg; // segment and received segment
			DatagramPacket pkt;
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			while ((readBytes = inFile.read(payload)) != -1) {
				// Read from the file and create a segment
				seg = new FtpSegment(seqNum, payload, readBytes);
				pkt = FtpSegment.makePacket(seg, IPAddress, ServerUDPport);
			
				// Send the segment and start the timer
				clientSocket.send(pkt);
				System.out.println("send   " + seqNum);
			
				task = new sendTimer(clientSocket, pkt);
				timer.scheduleAtFixedRate(task, timeout, timeout);
				
				while(true) {
					// Wait for ACK, when correct ACK arrives stop the timer 
					clientSocket.receive(receivePacket);

					// get ACK from recieved packet
					reccSeg = new FtpSegment(receivePacket);
					reccSegNum = reccSeg.getSeqNum();
					System.out.println("ack    " + reccSegNum);
					if (reccSegNum == seqNum + 1) {
						task.cancel();
						break;
					}
				}
				seqNum = reccSegNum;

			}
			// end while 			
			// Close sockets and clean up
			timer.cancel();
			timer.purge();
			inFile.close();
			socket.close();
			clientSocket.close();

		} catch(Exception e) {
			logger.log(Level.SEVERE, e.toString(), e);
		}
	}

	public class sendTimer extends TimerTask {

		DatagramSocket clientSocket;
		DatagramPacket pkt;

        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
		 * @param pkt the packet being sent
         * 
         *
         */
        public sendTimer(DatagramSocket clientSocket, DatagramPacket pkt) {
            this.clientSocket = clientSocket;
			this.pkt = pkt;
        }

		@Override
		public void run() {
			try {
				System.out.println("timeout");
				clientSocket.send(pkt);
				System.out.println("retx   " + seqNum);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.toString(), e);
			}
		}

	}


} // end of class
