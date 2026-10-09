package keyserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Protocol {

    public static final int MAX_FRAME_LEN = 4 * 1024 * 1024;

    private Protocol() {}

    public static void writeFrame(DataOutputStream out, byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(data);
    }

    public static byte[] readFrame(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > MAX_FRAME_LEN) {
            throw new IOException("Invalid frame length: " + len);
        }
        byte[] data = new byte[len];
        in.readFully(data);
        return data;
    }

    public static byte[] frame(byte[] data) {
        byte[] out = new byte[4 + data.length];
        out[0] = (byte) (data.length >>> 24);
        out[1] = (byte) (data.length >>> 16);
        out[2] = (byte) (data.length >>> 8);
        out[3] = (byte) data.length;
        System.arraycopy(data, 0, out, 4, data.length);
        return out;
    }
}
