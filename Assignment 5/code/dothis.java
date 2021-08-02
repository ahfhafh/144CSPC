import java.util.Timer;
import java.util.TimerTask;

public class dothis {

    static String a;

	static Timer timer;
    static TimerTask task;
   
    static class Thread1 implements Runnable {

        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
		 * @param pkt the packet being sent
         * 
         *
         */
        public Thread1() {
        }

		@Override
		public void run() {
            timer = new Timer();
            task = new ACKTimer();
			timer.scheduleAtFixedRate(task, 0, 1000);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    }

    static class ACKTimer extends TimerTask {


        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
		 * @param pkt the packet being sent
         * 
         *
         */
        public ACKTimer() {
        }

		@Override
		public void run() {
			try {
                System.out.println("hey");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

    static class Thread2 implements Runnable {

        String stuff;
        /**
         * Constructor to initialize the class.
         * 
         * @param clientSocket the socket
		 * @param pkt the packet being sent
         * 
         *
         */
        public Thread2() {
        }

		@Override
		public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            timer.cancel();
            timer = new Timer();
            task = new ACKTimer();
            timer.scheduleAtFixedRate(task, 0, 1000);
        }
    }

    public static void main(String args[]) {
        try {
            Thread Threada = new Thread(new Thread1());
            Thread Threadb = new Thread(new Thread2());
            Threada.start();
            Threadb.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}