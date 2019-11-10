package lse.neko.util;

public class MySystem {

    static {
        System.loadLibrary("Utils");
    }

    public static native long currentTimeMicros();

    public static void main(String[] args) {

        final int start = 10000;
        final int num = 20;
        long[] times = new long[num];
        for (int i = -start; i < num; i++) {
            if (i >= 0) {
                times[i] = currentTimeMicros();
            }
        }
        for (int i = 0; i < num; i++) {
            System.out.println("Time: " + times[i] + " microseconds");
        }

    }

}
