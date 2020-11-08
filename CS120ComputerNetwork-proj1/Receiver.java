import java.io.FileWriter;
import java.io.IOException;
import java.nio.*;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.*;

public class Receiver implements Runnable {
    boolean runing = false;
    private TargetDataLine targetDataLine;
    private byte[] hwbuffer = new byte[2 * Util.LINEBUFFER_SIZE];
    private int bufferlen;
    private short[] sampleBuffer = new short[Util.LINEBUFFER_SIZE];
    private short[] receivedwave = new short[Util.sampleRate / Util.CARFREQ];
    public short[] preamblebuffer = new short[Util.preamlenth];
    private LinkedList<Short> syncFIFO = new LinkedList<>();
    public String mod;
    private int correctIndex = -999999999;

    private int sampledelay = 0;

    private LinkedList<Short> buffer = new LinkedList<>();

    private int receivedwaveIndex;
    private int receivedbitIndex;
    private int receivedpacIndex;

    /* Carrier for symbol 0 and 1 */
    private short[] carrier0;
    private short[] carrier1;
    private long magicNum = Util.magicNum;

    enum State {
        readingPreamble, readingData
    }

    State state;

    public long max = -9999999;

    /* shicheng */
    private short[] Preamble; /* calculate preamble */

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Same as Transmitter but hard to simplify ///
    // ///
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Receiver() {
        state = State.readingPreamble;
        receivedpacIndex = 0;
        receivedwaveIndex = 0;
        receivedbitIndex = 0;
        final AudioFormat format = Util.getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format, targetDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < preamblebuffer.length; i++) {
            preamblebuffer[i] = 0;
        }

        Preamble = Util.calPreamble();
        carrier0 = Util.calCarrier0();
        carrier1 = Util.calCarrier1();
        
    }
    // public short[] calPreamble(short[] prea){
    // int preaSize = prea.length;
    // double phase_delta = (2333)/preaSize/2-1;
    // for (int i=0;i<preaSize ;i++ ) {
    // double phase = ((double)i/Util.sampleRate)*(i*phase_delta+1000);
    // short signal = (short) (Util.CARAMP* (Math.sin(2*Math.PI*phase)));
    // prea[i] = signal;
    // prea[preaSize-i-1] = prea[i];
    // }
    // return prea;
    // }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Same as Transmitter but hard to simplify ///
    // ///
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void initLine() {
        final AudioFormat format = Util.getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format, targetDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopLine() {
        targetDataLine.flush();
        targetDataLine.stop();
        targetDataLine.close();
        runing = false;
    }

    private void initFIFO(LinkedList<Short> l , int len){
        while(!l.isEmpty()){
            l.poll();
        }
        for (int i = 0; i< len; i ++){
            Short a = 0;
            l.offer(a);
        }
    }
    private void shiftSyncFIFO(Short sample){
        syncFIFO.poll();
        syncFIFO.offer(sample);
    }

    @Override
    public void run() {
        runing = true;
        //System.out.println("Start Rx!");
        targetDataLine.start();
        initFIFO(syncFIFO, Util.preamlenth);
        int numByteRead;
        while (runing) {
            numByteRead = targetDataLine.read(hwbuffer, 0, hwbuffer.length);
            bufferlen = numByteRead;
            if (numByteRead == -1) {
                System.err.println("[ERROR Receiver.java]: cannot read something");
                break;
            }
            deal();
        }
        try {
            check();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deal() {
        ByteBuffer.wrap(hwbuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(sampleBuffer);
        for (short sample : sampleBuffer) {
            buffer.offer(sample);
            // System.out.print(sample);
            // System.out.print(',');
            //shiftSyncFIFO(sample);
            

            // if (state == State.readingPreamble) {
            //     if (checkPreamble()) {
            //         initFIFO(syncFIFO, Util.preamlenth);
            //         state = State.readingData;
            //         receivedwaveIndex = 0;
            //         //System.out.println("Preamble found, start to read data!!!");
            //     }

            // } else if (state == State.readingData) {
            //     receivedwave[receivedwaveIndex] = sample;
            //     receivedwaveIndex += 1;
            //     System.out.print(sample);
            //     System.out.print(',');

            //     if (receivedwaveIndex == receivedwave.length) {
            //         decode(receivedwave);
            //         receivedwaveIndex = 0;
            //     }
            // }
        }
    }

    private boolean checkPreamble() {
        long powerDebug = preambleCorr();
        if (max < powerDebug){
            max = powerDebug;
            if(mod.equals("findmax")){
                System.out.println(max);
            }
        }
        if(powerDebug > magicNum){
            if(mod.equals("findprem")){
                String content = "";
                FileWriter fileWritter;
                for (ListIterator<Short> iterator = syncFIFO.listIterator(); iterator.hasNext();){
                    Short integer = iterator.next();
                    content += String.valueOf(integer) + ",";
                }
                content += "\n";

                for(int i=0;i<Preamble.length;i++){
                    content += String.valueOf(Preamble[i]) + ",";
                }
                content += "\n";
                try {
                    fileWritter = new FileWriter("waves.txt");
                    fileWritter.write(content);
                    fileWritter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    private long preambleCorr(){
        long sumPower = 0;
        int index = 0;
        for (ListIterator<Short> iterator = syncFIFO.listIterator(); iterator.hasNext();){
            Short integer = iterator.next();
            sumPower += integer*Preamble[index];
            index ++;
        }
        return sumPower;
    }

    private void check() throws IOException {
        FileWriter fileWritter = new FileWriter("waves.txt",true);
        if(mod.equals("testbits")){
            for(int i=0;i<Util.dataLength;i++){
                for(int j=0;j<carrier1.length;j++){
                    System.out.print(carrier1[j]);
                    System.out.print(',');
                }
            }
            System.out.print('\n');
        }
        
        for (ListIterator<Short> iterator = buffer.listIterator(); iterator.hasNext();){
            Short sample = iterator.next();
            shiftSyncFIFO(sample);
            if(mod.equals("testall")){
                System.out.print(sample);
                System.out.print(',');
            }
            if (state == State.readingPreamble) {
                if (checkPreamble()) {
                    if(correctIndex > Util.preamlenth * 2){
                        for(int i = 0;i<Util.samplesperpac;i++){
                            System.out.print('0');
                        }
                    }
                    initFIFO(syncFIFO, Util.preamlenth);
                    // for(int i=0;i<sampledelay;i++){
                    //     iterator.next();
                    // }
                    state = State.readingData;
                    // System.out.print(receivedpacIndex);
                    receivedpacIndex++;
                    receivedwaveIndex = 0;
                    receivedbitIndex = 0;
                    //System.out.println("Preamble found, start to read data!!!");
                }

            }
            else if (state == State.readingData) {
                receivedwave[receivedwaveIndex] = sample;
                receivedwaveIndex += 1;
                if(mod.equals("testbits")){
                    System.out.print(sample);
                    System.out.print(',');
                }
                if(mod.equals("findprem")){
                    fileWritter.write(String.valueOf(sample));
                    fileWritter.write(',');
                }

                if (receivedwaveIndex == receivedwave.length) {
                    decode(receivedwave);
                    receivedbitIndex += 1;
                    if(receivedbitIndex == Util.samplesperpac){
                        if(receivedpacIndex == Util.packagenum){
                            break;
                        }
                        else{
                            state = State.readingPreamble;
                            correctIndex = 0;
                            continue;
                        }
                    }
                    receivedwaveIndex = 0;
                }
            }
            correctIndex++;
        }
        fileWritter.close();
            
    }


    private void decode(short[] buffer){
        long sum = 0;
        //System.out.print('[');
        for (int i=0;i<buffer.length ;i++ ) {
            sum += (long) (buffer[i] * carrier1[i]);
            if(mod.equals("testeach")){
                System.out.print(buffer[i]);
                System.out.print(',');
            }
        }
        if(mod.equals("testeach")){
            System.out.print('\n');
        }
        assert(buffer.length == carrier1.length);
        byte bit;
        if (sum > 0){
            bit = 1;
        }
        else{
            bit = 0;
        }
        if(mod.equals("decode")){
            System.out.print(bit);
            // System.out.print('|');
            // System.out.print(sum);
           //System.out.print(sum);
        }

    }


}
