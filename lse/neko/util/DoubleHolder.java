package lse.neko.util;

public class DoubleHolder {

    public DoubleHolder(double value) {
        this.value = value;
    }

    public double value;

    public String toString() {
        return Double.toString(value);
    }
}

