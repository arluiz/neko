package lse.neko.util;

import java.util.Random;

public class NativeTest {

    public static void main(String[] args) {

        final int iterations = 1000;
        // waiting time in microseconds
        final int waitingTime = 1000;

        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            long a = System.currentTimeMillis() * 1000;
            long b = MySystem.currentTimeMicros();
            long c = (System.currentTimeMillis() + 1) * 1000;
            if (!(a <= b && b <= c)) {
                System.out.println("Test failed\n"
                                   + "Three times a, b, c in microseconds:"
                                   + "\n  a = " + a
                                   + "\n  b = " + b
                                   + "\n  c = " + c
                                   + "\na <= b <= c does not hold");
                System.exit(1);
            }
            b += random.nextInt(waitingTime * 2);
            while (b >= MySystem.currentTimeMicros()) {
                // empty body
            }
        }
        System.out.println("Test successful");
    }

}
