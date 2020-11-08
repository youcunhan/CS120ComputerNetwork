import java.awt.*;
import java.util.Arrays;

// |Preamble| |dst|src|id|type|MACload(data)|crc|
// |  480   | | 8 | 8 |8 | 4  |     500     |32 |

// Type:
//    0: normal
//    1: ACK
//    2: filebegin
//    3: fileend
//    4: macping_req
//    5: macping_reply

public class DataFrame {
    private byte[] data = new byte[Util.paclen];

    public DataFrame(int dst, int src, int id, int type, byte[] bits) {
        byte[] dstbit = Util.byteToBit((byte) dst);
        byte[] srcbit = Util.byteToBit((byte) src);
        byte[] typebit = Util.byteToBit((byte) type);
        byte[] idbit = Util.byteToBit((byte) id);
        byte[] alldata = new byte[528];

        for (int i = 0; i < 8; i++) {
            this.data[i] = dstbit[i];
            alldata[i] = dstbit[i];
        }
        for (int i = 0; i < 8; i++) {
            this.data[i + 8] = srcbit[i];
            alldata[i + 8] = srcbit[i];
        }
        for (int i = 0; i < 8; i++) {
            this.data[i + 16] = idbit[i];
            alldata[i + 16] = idbit[i];
        }
        for (int i = 0; i < 4; i++) {
            this.data[i + 24] = typebit[i + 4];
            alldata[i + 24] = typebit[i + 4];
        }
        // for (int i=0;i<528;i++){
        // alldata[i + 28] = bits[i];
        // }
        if (bits != null) {
            for (int i = 0; i < Util.databitsperpac; i++) {
                this.data[i + 28] = bits[i];
                alldata[i + 28] = bits[i];
                // System.out.print(bits[i]);

            }
            // System.out.println("id:" + String.valueOf(id) + "crc:" +
            // String.valueOf(CRC.crc32(alldata, 528)));
            byte[] crcbit = Util.createcrc(alldata);
            int index = 0;
            for (int i = Util.paclen - 32; i < Util.paclen; i++) {
                this.data[i] = crcbit[index];
                index++;
            }
        } else
            for (int i = 0; i < Util.databitsperpac; i++) {
                this.data[i + 28] = 0;
            }
    }

    public DataFrame(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getdst() {
        String bitStr = "";
        for (int i = 0; i < 8; i++) {
            bitStr += String.valueOf(data[i]);
        }
        return (int) Util.BitToByte(bitStr);
    }

    public int getsrc() {
        String bitStr = "";
        for (int i = 8; i < 16; i++) {
            bitStr += String.valueOf(data[i]);
        }
        return (int) Util.BitToByte(bitStr);
    }

    public int getid() {
        String bitStr = "";
        for (int i = 16; i < 24; i++) {
            bitStr += String.valueOf(data[i]);
        }
        return (int) Util.BitToByte(bitStr);
    }

    public int gettype() {
        String bitStr = "0000";
        for (int i = 24; i < 28; i++) {
            bitStr += String.valueOf(data[i]);
        }
        return (int) Util.BitToByte(bitStr);
    }
}
