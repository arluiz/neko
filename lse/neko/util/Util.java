package lse.neko.util;

// java imports:
import java.net.DatagramPacket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

// lse.neko imports:
import lse.neko.NekoSystem;

// other imports:
import org.apache.java.util.Configurations;


/**
 * General purpose utility functions.
 */
public class Util {

    private Util() {
    }

    public static final String START = "[";
    public static final String SEPARATOR = ",";
    public static final String END = "]";

    /**
     * toString() that works for arrays.
     * It displays something like [1,2,3].
     * Object.toString() should work this way.
     */
    public static String toString(Object o) {

        if (o == null) {
            return "null";
        } else if (!o.getClass().isArray()) {
            return o.toString();
        }

        StringBuffer buf = new StringBuffer(START);
        // the code looks the same for arrays of Object and
        // all primitive types
        if (o instanceof Object[]) {
            Object[] array = (Object[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof boolean[]) {
            boolean[] array = (boolean[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof byte[]) {
            byte[] array = (byte[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof char[]) {
            char[] array = (char[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof short[]) {
            short[] array = (short[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof int[]) {
            int[] array = (int[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof long[]) {
            long[] array = (long[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof float[]) {
            float[] array = (float[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        } else if (o instanceof double[]) {
            double[] array = (double[]) o;
            for (int i = 0; i < array.length; i++) {
                buf.append(((i == 0) ? "" : SEPARATOR) + array[i]);
            }
        }
        buf.append(END);
        return buf.toString();
    }

    // inspired from LogWriter.
    public static final String DEFAULT_DATEFORMAT = "0.000";
    public static final String KEYWORD_DATEFORMAT = "timeFormat";
    /**
     * The timestamp formatter.
     */
    private static NumberFormat formatter = null;

    public static String timeToString(double time) {

        if (formatter == null) {
            Configurations config = NekoSystem.instance().getConfig();
            String dateFormat =
                config.getString(KEYWORD_DATEFORMAT, DEFAULT_DATEFORMAT);
            formatter = NumberFormat.getInstance(Locale.ENGLISH);
            ((DecimalFormat) formatter).applyLocalizedPattern(dateFormat);
        }

        return formatter.format(time);
    }

    public static String datagramPacketToString(DatagramPacket packet) {
        int length = packet.getLength();
        byte[] data = packet.getData();
        return "DatagramPacket " + bytesToString(data, length);
    }

    private static final int BYTES_PER_LINE = 16;
    private static final String HEX_DIGITS = "0123456789abcdef";

    public static String bytesToString(byte[] data, int length) {

        StringBuffer buf = new StringBuffer();
        StringBuffer charBuf = new StringBuffer();

        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(data, 0, length);
        buf.append("length " + length + " crc " + crc.getValue() + "\n");

        int bytesInLine = 0;
        for (int i = 0; i < length; i++) {
            int b = (int) data[i];
            char c;
            if (b < 0x20 || b >= 0x7f) {
                c = '.';
            } else {
                c = (char) b;
            }
            charBuf.append(c);
            if (b < 0) { b += 256; }
            if (bytesInLine != 0) {
                buf.append(' ');
            }
            buf.append(HEX_DIGITS.charAt(b >> 4));
            buf.append(HEX_DIGITS.charAt(b & 0xF));
            bytesInLine++;
            if (bytesInLine >= BYTES_PER_LINE) {
                buf.append(" >" + charBuf.toString() + "<\n");
                charBuf = new StringBuffer();
                bytesInLine = 0;
            }
        }
        if (bytesInLine > 0) {
            buf.append(" >" + charBuf.toString() + "<\n");
        }
        return buf.toString();
    }

    public static int compare(long a, long b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public static int compare(int a, int b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public static int compare(short a, short b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public static int compare(char a, char b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

}

