import java.io.*;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.sound.sampled.AudioFormat;

public class Util {
	public static final int sampleSizeInBits = 16;
	public static final int channels = 1;
	public static final boolean signed = true;
	public static final boolean bigEndian = true;

	public static final int sampleRate = 48000;
	public static final int sizePerframe = 1000;
	public static final int CARAMP = 3000;
	public static final int CARFREQ = 6000;
	public static final int CARFREQ0 = 9000;
	public static final int CARFREQ1 = 6000;
	public static final int SAMPLES_PER_SYMBOL = sampleRate / CARFREQ;

	public static final int LINEBUFFER_SIZE = 8;
	/* shicheng */
	public static final int preSize = 4;/* bytes */
	public static final int sampleSizeInBytes = sampleSizeInBits * 8;
	public static final int preamlenth = 480;
	public static final int dataLength = 50000;
	public static final int dataLength2to1 = 40000;
	public static final int databitsperpac = 500;
	public static final int packagenum = dataLength / databitsperpac;
	public static final int packagenum2to1 = dataLength2to1 / databitsperpac;
	public static final int paclen = databitsperpac + 8 + 8 + 8 + 4 + 32;
	public static final long magicNum = 131611357653l;

	/* Carrier for symbol 0 and 1 */
	public short[] Preamble = new short[preamlenth]; /* calculate preamble */

	public static AudioFormat getFormat() {
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	public static byte[] createcrc(byte[] bits) {
		int out = CRC.crc32(bits, 528);
		byte[] by = intToBit(out);
		return by;
	}
	/* for receiver and sender they need to know carrier and pream */

	// public static short[] calPreamble() {
	// int preambleSize = preamlenth;
	// short[] preamble = new short[preambleSize];
	// double phaseIncre = 3000 / (preambleSize / 2 - 1);
	// for (int i = 0; i < preambleSize >> 1; i++) {
	// double phase = ((double) i / sampleRate) * (i * phaseIncre + 5000);
	// short signal = (short) (10*Util.CARAMP * (Math.sin(2 * Math.PI * phase)));
	// preamble[i] = signal;
	// preamble[preambleSize - i - 1] = preamble[i];
	// }
	// return preamble;
	// }
	// public static short[] calPreamble() {
	// int samplecount = preamlenth/2;
	// double samFrequency = sampleRate;
	// double t = (samplecount -1)*1 /samFrequency;
	// double a = (4000-2000)/ t;
	// short[] pream = new short[samplecount*2];
	// for (int i=0;i<samplecount ;i++ ) {
	// double phase = (i*1/samFrequency) *(i*1/samFrequency)*a*1*Math.PI
	// +(i*1/samFrequency)*2000*Math.PI;
	// short anss =(short) (Math.cos(phase) * Short.MAX_VALUE/2 );/*soundvoice*/
	// pream[i] = (short)( anss);
	// }
	// for (int i= samplecount;i<2*samplecount ;i++ ) {
	// double phase = -((i- samplecount)*1/samFrequency) *((i-
	// samplecount)*1/samFrequency)*a*1*Math.PI +((i-
	// samplecount)*1/samFrequency)*4000*Math.PI;
	// short anss =(short) (Math.cos(phase)* Short.MAX_VALUE/2);
	// pream[i] = (short)( anss);
	// }
	// return pream;
	// }
	public static short[] calPreamble() {
		short[] pream = new short[480];
		double[] f_p = new double[480];
		double[] t = new double[480];
		t[0] = 0;
		for (int i = 0; i < 479; i++) {
			t[i + 1] = t[i] + (1.0 / 48000);
			// System.out.print(t[i+1]);
		}
		for (int i = 0; i < 240; i++) {
			f_p[i] = 2000 + i * (6000 / 240);
			f_p[479 - i] = 2000 + i * (6000 / 240);
		}
		double[] omega = cumtrapz(t, f_p);
		for (int i = 0; i < 480; i++) {
			pream[i] = (short) (Short.MAX_VALUE / 2 * Math.sin(2 * Math.PI * omega[i]));
			// System.out.print(omega[i]);
		}
		return pream;
	}

	public static double[] cumtrapz(double[] x, double[] y) {
		int n = y.length;
		double[] dt = diffHalf(x);
		double[] z = new double[n];
		z[0] = 0;
		IntStream.range(1, z.length).forEach(i -> z[i] = z[i - 1] + dt[i - 1] * (y[i - 1] + y[i]));
		return z;
	}

	private static double[] diffHalf(double[] x) {
		double[] dt = new double[x.length - 1];
		Arrays.setAll(dt, i -> (x[i + 1] - x[i]) / 2);
		return dt;
	}

	public static short[] calCarrier0() {
		short[] carrier0 = new short[sampleRate / CARFREQ];
		for (int i = 0; i < carrier0.length; i++) {
			carrier0[i] = (short) ((-1) * Util.CARAMP * Math.sin(2 * Math.PI * Util.CARFREQ * i / Util.sampleRate));
		}
		return carrier0;
	}

	public static short[] calCarrier1() {
		short[] carrier1 = new short[sampleRate / CARFREQ];
		for (int i = 0; i < carrier1.length; i++) {
			carrier1[i] = (short) (Util.CARAMP * Math.sin(2 * Math.PI * Util.CARFREQ * i / Util.sampleRate));
		}
		return carrier1;
	}

	public static byte[] readFileByBytes(String fileName,int datalen) {
		byte[] bytes = new byte[datalen];
		int index = 0;
		FileInputStream input = null;
		try {
			input = new FileInputStream(fileName);
			byte[] buffer = new byte[1024];
			while (true) {
				int len = input.read(buffer);
				if (len == -1) {
					break;
				}
				for (int i = 0; i < buffer.length && index < datalen; i++) {
					bytes[index] = buffer[i];
					index++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				input.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return bytes;
	}

	public static byte[] bytesToBits(byte[] bytes) {
		byte[] bits = new byte[8 * bytes.length];
		int index = 0;
		for (int i = 0; i < bytes.length; i++) {
			byte[] bit = byteToBit(bytes[i]);
			for (int j = 0; j < 8; j++) {
				bits[index] = bit[j];
				index++;
			}
		}
		return bits;
	}

	public static byte[] BitsTobytes(byte[] bits) {
		byte[] bytes = new byte[bits.length / 8];
		for (int i = 0; i < bits.length; i += 8) {
			String bitStr = "";
			for (int j = i; j < i + 8; j++) {
				bitStr += String.valueOf(bits[j]);
			}
			bytes[i / 8] = BitToByte(bitStr);
		}
		return bytes;
	}

	public static byte[] byteToBit(byte b) {
		byte[] bit = new byte[8];
		bit[0] = (byte) ((b >> 7) & 0x1);
		bit[1] = (byte) ((b >> 6) & 0x1);
		bit[2] = (byte) ((b >> 5) & 0x1);
		bit[3] = (byte) ((b >> 4) & 0x1);
		bit[4] = (byte) ((b >> 3) & 0x1);
		bit[5] = (byte) ((b >> 2) & 0x1);
		bit[6] = (byte) ((b >> 1) & 0x1);
		bit[7] = (byte) ((b >> 0) & 0x1);
		return bit;
	}

	public static byte[] intToBit(int b) {
		byte[] bit = new byte[32];
		for (int i = 0; i < 32; i++) {
			bit[i] = (byte) ((b >> (31 - i)) & 0x1);
		}
		return bit;
	}

	public static byte BitToByte(String byteStr) {
		int re, len;
		if (null == byteStr) {
			return 0;
		}
		len = byteStr.length();
		if (len != 4 && len != 8) {
			return 0;
		}
		if (len == 8) {
			if (byteStr.charAt(0) == '0') {
				re = Integer.parseInt(byteStr, 2);
			} else {
				re = Integer.parseInt(byteStr, 2) - 256;
			}
		} else {
			re = Integer.parseInt(byteStr, 2);
		}
		return (byte) re;
	}

	public static void writeFileByBytes(byte[] bytes, String filename) {
		try {
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
			os.write(bytes);
			os.flush();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void outputbits(byte[] receivedbits, int receivedbitsIndex) {
		byte[] bytes = new byte[receivedbitsIndex / 8];
		for (int i = 0; i < receivedbitsIndex / 8; i++) {
			String bitStr = "";
			for (int j = 0; j < 8; j++) {
				bitStr += String.valueOf(receivedbits[8 * i + j]);
			}
			// System.out.print(bitStr+"|");
			bytes[i] = Util.BitToByte(bitStr);
		}
		Util.writeFileByBytes(bytes, "OUTPUT.bin");
	}

	public static void main(String[] args) {
		byte[] bits = new byte[5000*8];
		for(int i=0;i<5000*8;i++){
			int max = 2, min = 0;
			byte ran2 = (byte) (Math.random() * (max - min) + min);
			bits[i] = ran2;
		}
		byte[] databytes = Util.BitsTobytes(bits);
		Util.writeFileByBytes(databytes, "INPUT2to1.bin");
		
	}

}
