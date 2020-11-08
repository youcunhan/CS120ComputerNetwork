import javax.sound.sampled.AudioFormat;

public class Node2 {


    private static Receiver receiver = new Receiver(2);
    private static Transmitter transmitter = new Transmitter();
    static Thread receiverThread;
    static DataFrame[] frames = new DataFrame[Util.packagenum2to1];
    static boolean noise = false;
    static long[] sendTime = new long[Util.packagenum2to1];


    public Node2() {

    }
    public static void startReceive() {
        receiverThread = new Thread(receiver, "threadReceiverSignal");
        receiverThread.start();
        receiver.transmitter = transmitter;
    }


    private static void transmit2to1() {
        System.out.println("Start Send");
        // DataFrame ACKFrame = new DataFrame(2, 1, 10, 1, null);
        // transmitter.sendFrame(ACKFrame);
        byte[] bytes = Util.readFileByBytes("INPUT2to1.bin",5000);
        byte[] bits = Util.bytesToBits(bytes);
        byte[][] pacs = new byte[Util.packagenum2to1][Util.databitsperpac];
        for (int i = 0; i < bits.length; i++) {
            pacs[i / Util.databitsperpac][i % Util.databitsperpac] = bits[i];
        }
        DataFrame framebegin = new DataFrame(1, 2, 0, 2, null);
        DataFrame frameend = new DataFrame(1, 2, 0, 3, null);
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new DataFrame(1, 2, i, 0, pacs[i]);
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
        for(int i=0;i<Util.packagenum2to1;i++){
            if(receiver.receivedACK[i]==false) return false;
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
    public static void main(String[] args) throws Exception {
        receiver.mod=args[0];
        if(args[0].equals("macping_receive")){
            transmitter.startLine();
            receiver.mod = "macping_receive";
            startReceive();
        }
        else{
            transmitter.startLine();
            startReceive();
            transmit2to1();
        }
        
    }
}
