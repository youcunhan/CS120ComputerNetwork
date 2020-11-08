import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.*;

import javax.sound.sampled.*;
import java.util.*;

class Transmitter {

	public SourceDataLine sourceDataLine;
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// has been moved to Util ///
	// ///
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/* Carrier for symbol 0 and 1 */
	private short[] carrier0;
	private short[] carrier1;

	/* shicheng */
	private short[] Preamble; /* calculate preamble */

	private byte[] hwbuffer = new byte[2 * Util.LINEBUFFER_SIZE];
	private int bufferlen;

	public Transmitter() {
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// has been moved to Util ///
		// ///
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Preamble = Util.calPreamble();
		carrier0 = Util.calCarrier0();
		carrier1 = Util.calCarrier1();
		// for (int i = 0; i < carrier0.length; i++) {
		// 	carrier0[i] = (short) (Util.CARAMP * Math.cos(2 * Math.PI * Util.CARFREQ * i / Util.sampleRate));
		// 	carrier1[i] = (short) (Util.CARAMP * Math.sin(2 * Math.PI * Util.CARFREQ * i / Util.sampleRate));
		// }
		AudioFormat format = Util.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		try {
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
			sourceDataLine.open(format, sourceDataLine.getBufferSize());
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	private static short[] generatePart2(double duration) {
        int numSamples = (int) (duration * Util.sampleRate);
        short[] buffer = new short[numSamples];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (Util.CARAMP * (float) (Math.sin(2 * Math.PI * 1000 *i / Util.sampleRate) + Math.sin(2 * Math.PI * 10000 *i / Util.sampleRate)));
        }
        return buffer;
	}
	private static short[] generatePart3(double duration) {
        int numSamples = (int) (duration * Util.sampleRate);
        short[] buffer = new short[numSamples];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (4*Util.CARAMP * (float) Math.sin(2 * Math.PI * 1000 *i / Util.sampleRate));
        }
        return buffer;
    }

	public void startTransmit(String filename) {
		byte[] bits = readBitFile(filename);
		byte[][] pacs = new byte[Util.packagenum][Util.samplesperpac];
		for(int i=0;i<bits.length;i++){
			pacs[i/Util.samplesperpac][i%Util.samplesperpac] = bits[i];
		}
		sourceDataLine.start();
		short[] prem = Util.calPreamble();
		for(int i=0;i<Util.packagenum;i++){
			sendWave(prem);
			sendSymbols(pacs[i]);
		}
		
	
		//
		// try {
		// 	Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// 	e.printStackTrace();
		// }
		// sendWave(prem);
		// try {
		// 	Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// 	e.printStackTrace();
		// }
		// sendWave(prem);
		// for(int i=0;i<prem.length;i++){
		// 	System.out.print(prem[i]);
		// 	System.out.print(',');

		// }
		//sendWave(generatePart3(10));
		//sendWave(generatePart3(1));
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopLine();
	}



	public static byte[] readBitFile(String filename) {
		try {
			InputStream is = new FileInputStream(filename);
			int size = is.available();
			byte[] bytes = new byte[size];
			is.read(bytes);
			is.close();
			for(int i = 0;i < bytes.length;i++){
				bytes[i] -= '0';
			}
			return bytes;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
    }

	/*
	 * Encapsulates the data in an ethernet frame and sends it one byte at a time
	 * 
	 * Note: bits within an octet (byte) are lsb first Data is most significant
	 * octet first
	 */
	public void sendFrame(byte[] data, int size) {
		
	}

	private void sendSymbols(byte[] bits){
        for(int i = 0;i < bits.length;i++){
			sendSymbol(bits[i]);
		}
	}

	private void sendSymbol(byte symbol){
        short[] wave = null;
        if (symbol == 0){
            wave = carrier0;
        }else if (symbol == 1){
            wave = carrier1;
        }
        sendWave(wave);
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
	
	
	private static void playAudio(byte[] audio) 
	{
		try 
		{
			InputStream input = new ByteArrayInputStream(audio);
			final AudioFormat format = Util.getFormat();
			final AudioInputStream ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();

			int bufferSize = (int) Util.sampleRate * format.getFrameSize();
			byte buffer[] = new byte[bufferSize];

			try {
				int count;
				while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
					if (count > 0) {
						line.write(buffer, 0, count);
					}
				}
				line.drain();
				line.close();
			} catch (IOException e) {
				System.err.println("I/O problems: " + e);
				System.exit(-3);
			}
		} catch (LineUnavailableException e) {
			System.err.println("Line unavailable: " + e);
			System.exit(-4);
		}
	}


	public void stopLine(){
        sourceDataLine.flush();
        sourceDataLine.stop();
        sourceDataLine.close();
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
// 				  		has been moved to Util 															  ///
//																										  ///
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public short[] calPreamble(){
		short[] prea = new short[Util.preSize * Util.sampleSizeInBytes];
    	int preaSize = prea.length;
    	double phase_delta = (2333)/preaSize/2-1;
    	for (int i=0;i<preaSize ;i++ ) {
    		double phase = ((double)i/Util.sampleRate)*(i*phase_delta+1000);
    		short signal = (short) (Util.CARAMP* (Math.sin(2*Math.PI*phase))); 
    		prea[i] = signal;
    		prea[preaSize-i-1] = prea[i];
    	}
    	return prea;
    }
}