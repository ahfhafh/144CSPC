import java.util.logging.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.net.*;

public class StopWaitFtp{
	
	private static final Logger logger = Logger.getLogger("StopWaitFtp"); // global logger	

	private int timeoutInt, localPort, UDPport, seqNum, ACK;

	private boolean receiveACK;

	public class MyTimerTask extends TimerTask {

		@Override
		public void run() {
			System.out.println("timeout");
			UDPSocket.send(sendPack);
			System.out.println("retx   " + seqNum);
		}
	}

	/**
	 * Constructor to initialize the program 
	 * 
	 * @param timeout		The time-out interval for the retransmission timer
	 */
	public StopWaitFtp(int timeout) {

		this.timeoutInt = timeout;
	}


	/**
	 * Send the specified file to the specified remote server
	 * 
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 * @throws FtpException If anything goes wrong while sending the file
	 */
	public void send(String serverName, int serverPort, String fileName) throws FtpException {

		try {
			
			Socket socket = new Socket(serverName, serverPort);

			Path file = Paths.get(fileName);
			long fileLength = Files.size(file);

			localPort = socket.getLocalPort();

			// Necessary stream for writing to socket
            DataOutputStream write = new DataOutputStream(socket.getOutputStream());

            // Necessary streams for reading from socket
			DataInputStream read = new DataInputStream(socket.getInputStream());

			write.writeUTF(fileName);
			write.writeLong(fileLength);
			write.writeInt(localPort);
			write.flush();

			UDPport = read.readInt();
			seqNum = read.readInt();

			write.close();
			read.close();

			socket.close();

			DatagramSocket UDPSocket = new DatagramSocket();

			InetAddress ipAd = InetAddress.getLoopbackAddress();

			InputStream readFile = new FileInputStream(new File(fileName));

			byte[] sendData = new byte[1080];
			byte[] receiveData = new byte[1080];
			int count = 1;

			boolean notEndOfFile = true;
			receiveACK = true;

			Timer timer = new Timer(true);

			while(notEndOfFile) {

				if(receiveACK == true) {

					if((count = readFile.read(sendData)) == -1) {

						notEndOfFile = false;
						break;
					}
				}

				FtpSegment filePack = new FtpSegment(seqNum, sendData, count);

				DatagramPacket sendPack = FtpSegment.makePacket(filePack, ipAd, UDPport);

				UDPSocket.send(sendPack);

				TimerTask timerTask = new MyTimerTask();

				timer.scheduleAtFixedRate(timerTask, timeoutInt, timeoutInt);

				if (receiveACK == true) {

					System.out.println("Send " + seqNum);
				} else {

					System.out.println("retx " + seqNum);
				}

				receiveACK = false;

				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				UDPSocket.receive(receivePacket);

				timerTask.cancel();

				FtpSegment pack = new FtpSegment(receivePacket);

				ACK = pack.getSeqNum();

				System.out.println("ack " + ACK);

				if(ACK == seqNum + 1) {

					receiveACK = true;
					seqNum += 1;
				} else {

					System.out.println("Timeout");
				}
			}

			timer.cancel();
			timer.purge();

			readFile.close();

			UDPSocket.close();
		} catch (Exception e) {
			
			System.out.println("Error: " + e.getMessage());
		}
	}

} // end of class