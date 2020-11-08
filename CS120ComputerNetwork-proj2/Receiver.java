import java.io.FileWriter;
import java.io.IOException;
import java.nio.*;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.*;

public class Receiver implements Runnable {
    private int id;
    Transmitter transmitter;
    public boolean runing = false;
    boolean isReceivingfile;
    private TargetDataLine targetDataLine;
    private byte[] hwbuffer = new byte[2 * Util.LINEBUFFER_SIZE];
    private int bufferlen;
    private short[] sampleBuffer = new short[Util.LINEBUFFER_SIZE];
    private byte[] receivedbitsbuffer = new byte[Util.paclen];
    public byte[] receiveddatabits = new byte[Util.dataLength * 2];
    public boolean[] receivedACK = new boolean[Util.packagenum];
    public long[] receivedACKTime = new long[Util.packagenum];
    private short[] receivedwave = new short[Util.sampleRate / Util.CARFREQ];
    private LinkedList<Short> syncFIFO = new LinkedList<>();
    public String mod;
    private int correctIndex = -999999999;
    private int noise_bottom_line = 500;
    private int sampledelay = 0;
    public boolean receiveoveer = false;

    private LinkedList<Short> buffer = new LinkedList<>();

    private int receivedwaveIndex;
    private int receivedbitIndex;
    public int receivedpacIndex;
    public int receiveddatabitsIndex;

    /* Carrier for symbol 0 and 1 */
    private short[] carrier0;
    private short[] carrier1;
    private long magicNum = Util.magicNum;

    enum State {
        readingPreamble, readingData
    }

    State state;

    public long max = -9999999;

    private short[] Preamble;

    public Receiver(int id) {
        this.id = id;
        state = State.readingPreamble;
        resetReceiver();
        final AudioFormat format = Util.getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format, targetDataLine.getBufferSize());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        Preamble = Util.calPreamble();
        carrier0 = Util.calCarrier0();
        carrier1 = Util.calCarrier1();

    }

    public void resetReceiver() {
        receivedpacIndex = 0;
        receivedwaveIndex = 0;
        receivedbitIndex = 0;
        receiveddatabitsIndex = 0;
        isReceivingfile = false;
        for (int i = 0; i < receivedACK.length; i++) {
            receivedACK[i] = false;
        }
    }

    public void stopLine() {
        targetDataLine.flush();
        targetDataLine.stop();
        targetDataLine.close();
        runing = false;
    }

    private void initFIFO(LinkedList<Short> l, int len) {
        while (!l.isEmpty()) {
            l.poll();
        }
        for (int i = 0; i < len; i++) {
            Short a = 0;
            l.offer(a);
        }
    }

    private void shiftSyncFIFO(Short sample) {
        syncFIFO.poll();
        syncFIFO.offer(sample);
    }

    @Override
    public void run() {
        runing = true;
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

    }

    private void deal() {
        ByteBuffer.wrap(hwbuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(sampleBuffer);
        for (short sample : sampleBuffer) {
            if (mod.equals("macping_receive")) {
                if (Math.abs(sample) > 200) {
                    DataFrame REPLYFrame = new DataFrame(1, 2, 0, 5, null);
                    transmitter.sendFrame(REPLYFrame);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            }
            if (mod.equals("macping")) {
                if (Math.abs(sample) > 200) {
                    receivedACK[0] = true;
                }
            }
            shiftSyncFIFO(sample);
            if (mod.equals("testall")) {
                System.out.print(sample);
                System.out.print(',');
            }
            if (state == State.readingPreamble) {
                if (checkPreamble()) {
                    initFIFO(syncFIFO, Util.preamlenth);
                    state = State.readingData;
                    // System.out.println("Preamble " + String.valueOf(receivedpacIndex) + " found,
                    // start to read frame!!!");
                    receivedpacIndex++;
                    receivedwaveIndex = 0;
                    receivedbitIndex = 0;

                }

            } else if (state == State.readingData) {
                receivedwave[receivedwaveIndex] = sample;
                receivedwaveIndex += 1;
                if (mod.equals("testbits")) {
                    System.out.print(sample);
                    System.out.print(',');
                }
                if (receivedwaveIndex == receivedwave.length) {
                    decode(receivedwave);
                    receivedbitIndex += 1;
                    if (receivedbitIndex == Util.paclen) {
                        unpackage();
                        state = State.readingPreamble;
                        correctIndex = 0;
                        continue;
                    }
                    receivedwaveIndex = 0;
                }
            }
            correctIndex++;
        }
    }

    private boolean checkPreamble() {
        long powerDebug = preambleCorr();
        if (max < powerDebug) {
            max = powerDebug;
            if (mod.equals("findmax")) {
                System.out.println(max);
            }
        }
        if (powerDebug > magicNum) {
            if (mod.equals("findprem")) {
                String content = "";
                FileWriter fileWritter;
                for (ListIterator<Short> iterator = syncFIFO.listIterator(); iterator.hasNext();) {
                    Short integer = iterator.next();
                    content += String.valueOf(integer) + ",";
                }
                content += "\n";

                for (int i = 0; i < Preamble.length; i++) {
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

    private long preambleCorr() {
        long sumPower = 0;
        int index = 0;
        for (ListIterator<Short> iterator = syncFIFO.listIterator(); iterator.hasNext();) {
            Short integer = iterator.next();
            sumPower += integer * Preamble[index];
            index++;
        }
        return sumPower;
    }

    public boolean isIdle() {
        for (short b : sampleBuffer) {
            if (Math.abs(b) >= noise_bottom_line) {
                return false;
            }
        }
        return true;
    }

    private void decode(short[] buffer) {
        long sum = 0;
        for (int i = 0; i < buffer.length; i++) {
            sum += (long) (buffer[i] * carrier1[i]);
            if (mod.equals("testeach")) {
                System.out.print(buffer[i]);
                System.out.print(',');
            }
        }
        if (mod.equals("testeach")) {
            System.out.print('\n');
        }
        assert (buffer.length == carrier1.length);
        byte bit;
        if (sum > 0) {
            bit = 1;
        } else {
            bit = 0;
        }
        receivedbitsbuffer[receivedbitIndex] = bit;

    }

    private void unpackage() {
        DataFrame df = new DataFrame(receivedbitsbuffer);
        int src = df.getsrc();
        int dst = df.getdst();
        int id = df.getid();
        int type = df.gettype();
        System.out.println("pck received! src:" + String.valueOf(src) + "|dst:" + String.valueOf(dst) + "|id:"
                + String.valueOf(id) + "|type:" + String.valueOf(type));
        if (dst != this.id || id<0 || id >100)
            return;
        switch (type) {
            case 0:
                if (isReceivingfile) {
                    boolean flag = false;
                    byte[] checkbits = new byte[528];
                    byte[] crcbits = new byte[32];
                    for (int i = 0; i < 528; i++) {
                        checkbits[i] = receivedbitsbuffer[i];
                    }
                    byte[] crc = new byte[32];
                    // System.out.println("id:" + String.valueOf(id) + "crc:" +
                    // String.valueOf(CRC.crc32(checkbits, 528)));
                    crc = Util.intToBit(CRC.crc32(checkbits, 528));
                    for (int i = 0; i < 32; i++) {
                        crcbits[i] = receivedbitsbuffer[i + 528];
                        if (crcbits[i] != crc[i]) {
                            System.out.println("Break by crc");
                            flag = true;
                            break;
                        }
                    }
                    if (flag == true) {
                        break;
                    }
                    for (int i = 28; i < receivedbitsbuffer.length - 32; i++) {
                        receiveddatabits[Util.databitsperpac * id + i - 28] = receivedbitsbuffer[i];
                    }
                    if (Util.databitsperpac * (id + 1) > receiveddatabitsIndex)
                        receiveddatabitsIndex = Util.databitsperpac * (id + 1);

                }

                DataFrame ACKFrame = new DataFrame(src, dst, id, 1, null);
                transmitter.sendFrame(ACKFrame);
                break;
            case 1:
                receivedACK[id] = true;
                receivedACKTime[id] = System.currentTimeMillis();
                break;
            case 2:
                isReceivingfile = true;
                break;
            case 3:
                if(isReceivingfile == false) return;
                isReceivingfile = false;
                byte[] receiveddatabitstmp = new byte[receiveddatabitsIndex];
                System.out.println("receive " + String.valueOf(receiveddatabitsIndex) + " bits");
                for (int i = 0; i < receiveddatabitsIndex; i++) {
                    receiveddatabitstmp[i] = receiveddatabits[i];
                }
                byte[] databytes = Util.BitsTobytes(receiveddatabitstmp);
                // for(int i=0;i<databytes.length;i++){
                // System.out.print(databytes[i]);
                // }
                Util.writeFileByBytes(databytes, "OUTPUT.bin");
                if(this.id==1){
                    receiveoveer = true;
                }
                else{
                    System.exit(0);
                }
                break;
            case 4:
                DataFrame REPLYFrame = new DataFrame(src, dst, 1, 5, null);
                transmitter.sendFrame(REPLYFrame);
                break;
            case 5:
                receivedACK[id] = true;
                receivedACKTime[id] = System.currentTimeMillis();
                break;
        }

    }

}
