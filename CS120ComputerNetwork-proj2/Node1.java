import javax.sound.sampled.AudioFormat;

public class Node1 {

    private static Receiver receiver = new Receiver(1);
    private static Transmitter transmitter = new Transmitter();
    static Thread receiverThread;
    static DataFrame[] frames = new DataFrame[Util.packagenum];
    static boolean noise = false;
    static long[] sendTime = new long[Util.packagenum];

    public Node1() {

    }

    public static void startReceive() {
        receiver.mod = "decode";
        receiverThread = new Thread(receiver, "threadReceiverSignal");
        receiverThread.start();
        receiver.transmitter = transmitter;
    }

    public static void transmit1to2() {
        System.out.println("Start Send");
        // DataFrame ACKFrame = new DataFrame(2, 1, 10, 1, null);
        // transmitter.sendFrame(ACKFrame);
        byte[] bytes = Util.readFileByBytes("INPUT1to2.bin",6250);
        byte[] bits = Util.bytesToBits(bytes);
        byte[][] pacs = new byte[Util.packagenum][Util.databitsperpac];
        for (int i = 0; i < bits.length; i++) {
            pacs[i / Util.databitsperpac][i % Util.databitsperpac] = bits[i];
        }
        DataFrame framebegin = new DataFrame(2, 1, 0, 2, null);
        DataFrame frameend = new DataFrame(2, 1, 0, 3, null);
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new DataFrame(2, 1, i, 0, pacs[i]);
        }
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();
        transmitter.startLine();
        lockIdle();
        transmitter.sendFrame(framebegin);
        transmitter.sendFrame(framebegin);
        transmitter.sendFrame(framebegin);
        for (int i = 0; i < frames.length; i++) {
            lockIdle();
            transmitter.sendFrame(frames[i]);
            sendTime[i] = System.currentTimeMillis();
            System.out.println("Send pkg " + String.valueOf(i));
            if (i % 10 == 9 && i >= 20) {
                checkACK(0, i - 10);
            }

        }

        while(!receivedallACK()) {
            checkACK(0, frames.length);
        }
        transmitter.sendFrame(frameend);
        endTime = System.currentTimeMillis();
        System.out.println("Send finish, time: " + String.valueOf(((double) (endTime - startTime)) / 1000));

    }

    private static boolean receivedallACK(){
        for(boolean b:receiver.receivedACK){
            if(b==false) return false;
        }
        return true;
    }

    private static void checkACK(int range1, int range2) {
        for (int i = range1; i < range2; i++) {
            if (receiver.receivedACK[i] == false) {
                // if (System.currentTimeMillis() - sendTime[i] > 5000) {
                //     System.out.println("Link Error!");
                //     System.exit(0);
                // }
                transmitter.sendFrame(frames[i]);
                System.out.println("Resend pkg " + String.valueOf(i));
            } else {
                long time = receiver.receivedACKTime[i] - sendTime[i];
                System.out.println("Ping: " + time);
            }
        }
    }

    private static void lockIdle() {
        if (noise) {
            while (!receiver.isIdle()) {
                System.out.println("noisy!");
            }
        }
    }

    private static void macperf() {
        byte[] bits = new byte[500];
        for (int i = 0; i < bits.length; i++) {
            int max = 2, min = 0;
            byte ran2 = (byte) (Math.random() * (max - min) + min);
            bits[i] = ran2;
        }
        DataFrame frame = new DataFrame(2, 1, 0, 0, bits);
        long startTime = System.currentTimeMillis();
        long bitlen = 0;
        transmitter.startLine();
        while(true){
            transmitter.sendFrame(frame);
            bitlen+=Util.paclen;
            long endTime = System.currentTimeMillis();
            double time = ((double) (endTime - startTime));
            System.out.println("TH: " + String.valueOf(bitlen/time)+" kbps");

            
        }
    }

    private static void macping() {
        byte[] bits = new byte[500];
        for (int i = 0; i < bits.length; i++) {
            int max = 2, min = 0;
            byte ran2 = (byte) (Math.random() * (max - min) + min);
            bits[i] = ran2;
        }
        DataFrame frame = new DataFrame(2, 1, 0, 4, bits);
        transmitter.startLine();
        transmitter.sendFrame(frame);
        try {
            Thread.sleep(300);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (receiver.receivedACK[0] == false) {
            System.out.println("TIME OUT!");
            System.exit(0);
        } else {
            long time = receiver.receivedACKTime[0] - startTime;
            System.out.println("Ping: " + time);
        }
        
    }

    public static void main(String[] args) throws Exception {
        receiver.mod=args[0];
        if(args[0].equals("macperf")){
            macperf();
        }
        else if(args[0].equals("macping")){
            startReceive();
            macping();
            receiver.mod=args[0];
        }
        else{
            transmitter.startLine();
            startReceive();
            // while(true){
            //     Thread.sleep(1000);
            //     if(receiver.receiveoveer==true) break;
            // }
            transmit1to2();
            Thread.sleep(2000);
            receiver.runing = false;
        }
        
        
    }
}
