import static java.lang.System.*;

public class ComparisonHelper {
    boolean debug;

    public ComparisonHelper(boolean debug) {
        this.debug = debug;
    }

    public boolean compareFirstArrayElements(int[] a1, int[] a2) {
        if (a1[0] < a2[0]) {
            if (debug) {
                out.println("First element of a1 is smaller than first element of a2");
            }

            return true;
        }

        return false;
    }

    public boolean compareInts(int i1, int i2) {
        if (i1 < i2) {
            if (debug) {
                out.println("i1 is smaller than i2");
            }
            return true;
        }

        return false;
    }

    public boolean compareLongs(long l1, long l2) {
        if (l1 < l2) {
            if (debug) {
                out.println("l1 is smaller than l2");
            }
            return true;
        }

        return false;
    }

    public boolean compareDoubles(double d1, double d2) {
        if (d1 < d2) {
            if (debug) {
                out.println("d1 is smaller than d2");
            }
            return true;
        }

        return false;
    }

    public boolean compareFloats(double f1, double f2) {
        if (f1 < f2) {
            if (debug) {
                out.println("f1 is smaller than f2");
            }
            return true;
        }

        return false;
    }


}
