import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;

import javax.sound.sampled.*;
import java.util.*;

class Transmitter {

	public SourceDataLine sourceDataLine;
	static Thread receiverThread;
	/* Carrier for symbol 0 and 1 */
	private short[] carrier0;
	private short[] carrier1;

	/* shicheng */
	private short[] Preamble; /* calculate preamble */

	private byte[] hwbuffer = new byte[2 * Util.LINEBUFFER_SIZE];
	private int bufferlen;
	public int id;

	public Transmitter() {
		Preamble = Util.calPreamble();
		carrier0 = Util.calCarrier0();
		carrier1 = Util.calCarrier1();
		AudioFormat format = Util.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		try {
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
			sourceDataLine.open(format, sourceDataLine.getBufferSize());
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
    public int sendFrame(DataFrame df){
        //TODO: --Assign: shicheng
        //Check noise and wait, you can write the noise detection function in Util.java, for using in Receiver.
        return sendPackage(df.getData());
    }

    private int sendPackage (byte[] byteData){
        int sendPreamble = sendPreamble();
        int sendDataNumByte = sendData(byteData);
        return sendDataNumByte + sendPreamble;
    }

    private int sendData(byte[] bits){
		int sendsamplenum = 0;
        for(int i = 0;i < bits.length;i++){
			sendsamplenum += sendSymbol(bits[i]);
		}
		return sendsamplenum;
	}

	private int sendPreamble(){
        return sendWave(Preamble);
    }

	private int sendSymbol(int symbol){
        short[] wave = null;
        if (symbol == 0){
            wave = carrier0;
        }else if (symbol == 1){
            wave = carrier1;
        }
        return sendWave(wave);
	}
	
	public int sendWave (short[] wave) {
        int sentSampleCount = 0;
        for (int i=0; i<wave.length; i+=Util.LINEBUFFER_SIZE) {
            ByteBuffer.wrap(hwbuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(wave, i, hwbuffer.length/2);
            bufferlen = hwbuffer.length;
            int sendNum = sendBuffer();
            if (sendNum == -1) {
                System.err.println("SendBuffer Error");
                return -1;
            }
            sentSampleCount += sendNum;
        }
        return sentSampleCount;
	}
	private int sendBuffer(){
        return sourceDataLine.write(hwbuffer, 0, bufferlen);
    }
    public void startLine(){
        sourceDataLine.start();
	}
	public void stopLine(){
        sourceDataLine.flush();
        sourceDataLine.stop();
        sourceDataLine.close();
	}

}