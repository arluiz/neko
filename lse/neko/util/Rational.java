package lse.neko.util;

/**
 * Beware: this class almost never checks for integer overflow.
 * Operations are only safe if the product of the denominators
 * involved does not overflow.
 */
public class Rational
    implements Comparable
{

    private int num;
    private int denom;

    public Rational(int num, int denom) {
        if (denom == 0
            || denom == Integer.MIN_VALUE
            || num == Integer.MIN_VALUE)
        {
            throw new ArithmeticException();
        }
        if (denom < 0) {
            this.num = -num;
            this.denom = -denom;
        } else {
            this.num = num;
            this.denom = denom;
        }
        int gcd = gcd(this.num, this.denom);
        this.num /= gcd;
        this.denom /= gcd;
    }

    public boolean equals(Object right) {
        // only comparison with Rationals is implemented
        return equals((Rational) right);
    }

    public int hashCode() {
        throw new RuntimeException("To use objects of this class "
                                   + "in hash-based collections, "
                                   + "implement a hashCode method "
                                   + "that is consistent with equals!");
    }

    public boolean equals(Rational right) {
        return num == right.num && denom == right.denom;
    }

    public int compareTo(Object right) {
        // only comparison with Rationals is implemented
        return compareTo((Rational) right);
    }

    public int compareTo(Rational right) {
        // comparison based on sign
        int s = (num < 0) ? -1 : +1;
        int rs = (num < 0) ? -1 : +1;
        if (s != rs) {
            return s;
        }

        // comparison based on integer part
        int i = num / denom;
        int ri = right.num / right.denom;
        int r = Util.compare(i, ri);
        if (r != 0) {
            return r;
        }

        // comparison based on fraction part
        int n = num % denom;
        int rn = right.num % right.denom;
        return Util.compare(n * right.denom, rn * denom);
    }

    public Rational minus(Rational right) {
        return new Rational(num * right.denom - denom * right.num,
                            denom * right.denom);
    }

    public Rational plus(Rational right) {
        return new Rational(num * right.denom + denom * right.num,
                            denom * right.denom);
    }

    public Rational plus(int right) {
        return new Rational(num + denom * right, denom);
    }

    public Rational times(int right) {
        return new Rational(num * right, denom);
    }

    private static int gcd(int aParam, int bParam) {
        int a = Math.abs(aParam);
        int b = bParam;
        assert b > 0;
        while (b != 0) {
            int r = a % b;
            a = b;
            b = r;
        }
        return a;
    }

    public double doubleValue() {
        return num / denom;
    }

    public String toString() {
        if (denom == 1) {
            return "" + num;
        } else {
            return "" + num + "/" + denom;
        }
    }

    public static final Rational MAX_VALUE =
        new Rational(Integer.MAX_VALUE, 1);
    public static final Rational MIN_VALUE =
            new Rational(-Integer.MAX_VALUE, 1);
}
