import javax.sound.sampled.AudioFormat;

public class Node {


    private static Receiver receiver = new Receiver();
    private static Transmitter transmitter = new Transmitter();
    static Thread receiverThread;

    public Node() {

    }

    private static short[] generatePart2(double duration) {
        int numSamples = (int) (duration * Util.sampleRate);
        short[] buffer = new short[numSamples];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (Util.CARAMP * (float) Math.sin(2 * Math.PI * 1000 *i / Util.sampleRate));
        }
        return buffer;
    }

    public static void startReceive() {
        receiverThread = new Thread(receiver, "threadReceiverSignal");
        receiverThread.start();
    }

    public static void startTransmit(){
        // long stamp0 = System.currentTimeMillis();
        transmitter.startTransmit("INPUT.txt");


        // long stamp1 = System.currentTimeMillis();
        // double time = ((stamp1-stamp0) / 1000.0);
        //System.out.printf("Time used: %.04f s;" , time);
    }

    public static void main(String[] args) throws Exception {
        //Thread.sleep(2000);

        //System.out.println("start receive");
        
        //System.out.println("start receive");
        receiver.mod = args[0];
        startReceive();
        //ystem.out.println("start transmit");
        //Thread.sleep(2000);
        //startTransmit();
        System.in.read();
        receiver.stopLine();
        // System.out.println("];");
        // System.out.println("length(wave)");
        // System.out.println("stem(wave);");
    }
}
