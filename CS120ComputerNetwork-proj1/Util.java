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
	public static final int CARFREQ = 3000;
	public static final int CARFREQ0 = 9000;
	public static final int CARFREQ1 = 6000;
	public static final int SAMPLES_PER_SYMBOL = sampleRate/CARFREQ;
	

    public static final int LINEBUFFER_SIZE = 8;
    /*shicheng*/
    public static final int preSize = 4;/*bytes*/
	public static final int sampleSizeInBytes = sampleSizeInBits*8;
	public static final int preamlenth = 480;
	public static final int dataLength = 10000;
	public static final int packagenum = 50;
	public static final int samplesperpac = dataLength/packagenum;
	public static final long magicNum = 20050174070l;



    /* Carrier for symbol 0 and 1 */
	public short[] Preamble = new short[preamlenth]; /*calculate preamble*/




    public static AudioFormat getFormat()
	{
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	}

	/* for  receiver and  sender they need to know carrier and pream*/


	// public static short[] calPreamble() {
	// 	int preambleSize = preamlenth;
	// 	short[] preamble = new short[preambleSize];
	// 	double phaseIncre = 3000 / (preambleSize / 2 - 1);
	// 	for (int i = 0; i < preambleSize >> 1; i++) {
	// 		double phase = ((double) i / sampleRate) * (i * phaseIncre + 5000);
	// 		short signal = (short) (10*Util.CARAMP * (Math.sin(2 * Math.PI * phase)));
	// 		preamble[i] = signal;
	// 		preamble[preambleSize - i - 1] = preamble[i];
	// 	}
	// 	return preamble;
	// }
	// public static short[] calPreamble() {
	// 	int samplecount = preamlenth/2;
	// 	double samFrequency = sampleRate;
	// 	double t = (samplecount -1)*1 /samFrequency;
	// 	double a = (4000-2000)/ t;
	// 	short[] pream = new short[samplecount*2];
	// 	for (int i=0;i<samplecount ;i++ ) {
	// 		double phase = (i*1/samFrequency) *(i*1/samFrequency)*a*1*Math.PI +(i*1/samFrequency)*2000*Math.PI;
	// 		short anss =(short) (Math.cos(phase) * Short.MAX_VALUE/2 );/*soundvoice*/
	// 		pream[i] = (short)( anss);
	// 	}
	// 	for (int i= samplecount;i<2*samplecount ;i++ ) {
	// 		double phase = -((i- samplecount)*1/samFrequency) *((i- samplecount)*1/samFrequency)*a*1*Math.PI +((i- samplecount)*1/samFrequency)*4000*Math.PI;
	// 		short anss =(short) (Math.cos(phase)*  Short.MAX_VALUE/2);
	// 		pream[i] = (short)( anss);
	// 	}
	// 	return pream;
	// }
	public static void main(String[] args){
		short[] pream = calPreamble();
		for (int i=0;i<pream.length;i++){
			System.out.print(pream[i]);
			System.out.print(',');

		}
	}

	public static short[] calPreamble(){
		short[] pream = new short[480];
		double[] f_p = new double[480];
		double[] t = new double[480];
		t[0] = 0;
		for (int i=0;i<479 ;i++ ) {
			t[i+1] = t[i] +(1.0/48000);
			//System.out.print(t[i+1]);
		}
		for (int i=0;i<240 ;i++ ) {
			f_p[i] = 2000+i*(6000/240);
			f_p[479-i] = 2000+i*(6000/240);
		}
		double[] omega = cumtrapz(t,f_p);
		for (int i=0;i<480 ;i++ ) {
			pream[i] = (short) (Short.MAX_VALUE/2*Math.sin(2* Math.PI*omega[i]));
			//System.out.print(omega[i]);
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
		short[] carrier0 = new short[sampleRate/CARFREQ];
		for (int i = 0; i < carrier0.length; i++) {
			carrier0[i] = (short) ((-1)*Util.CARAMP * Math.sin(2 * Math.PI *  Util.CARFREQ * i / Util.sampleRate));
		}
		return carrier0;
	}
	// public static short[] calCarrier1() {
	// 	short[] carrier1 = new short[sampleRate/CARFREQ];
	// 	for (int i = 0; i < carrier1.length; i++) {
	// 		carrier1[i] = 0;
	// 	}
	// 	carrier1[0] = 1000;
	// 	return carrier1;
	// }

	public static short[] calCarrier1() {
		short[] carrier1 = new short[sampleRate/CARFREQ];
		for (int i = 0; i < carrier1.length; i++) {
			carrier1[i] = (short) (Util.CARAMP * Math.sin(2 * Math.PI *  Util.CARFREQ * i / Util.sampleRate));
		}
		return carrier1;
	}

	public static double Variance(double[] x) { 
	    int m=x.length;
	    double sum=0;
	    for(int i=0;i<m;i++){
	        sum+=x[i];
	    }
	    double dAve=sum/m;
	    double dVar=0;
		for(int i=0;i<m;i++){
			dVar+=(x[i]-dAve)*(x[i]-dAve);
		}
		return dVar/m;
	}
}
