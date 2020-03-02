package testCode;

public class DeadCodeAnalysisTestCode {
    public void testDoubleCompare(double d1, double d2) {
        if (d1 < d2) {
            System.out.println(String.format("%d is smaller than %d", d1, d2));
        } else {
            System.out.println(String.format("%d is larger than %d or equal", d2, d1));
        }
    }

    public void testIntCompare(int i1, int i2) {
        if (i1 < i2) {
            System.out.println(String.format("%d is smaller than %d", i1, i2));
        } else {
            System.out.println(String.format("%d is larger than %d or equal", i2, i1));
        }
    }

    public void testLongCompare(long l1, long l2) {
        if (l1 < l2) {
            System.out.println(String.format("%d is smaller than %d", l1, l2));
        } else {
            System.out.println(String.format("%d is larger than %d or equal", l2, l1));
        }
    }

    public int testNestedIf(int i1, int i2) {
        while (i1 <= i2) {
            System.out.println(String.format("%d is smaller than %d", i1, i2));
            if (i1 == i2) {
                System.out.println(String.format("%d is equal to %d", i1, i2));
            } else {
                i1++;
            }
        }

        return 8;
    }

    public int testMultipleNestedIf(int i1, int i2) {
        while (i1 <= i2) {
            System.out.println(String.format("%d is smaller than %d", i1, i2));
            if (i1 == i2) {
                System.out.println(String.format("%d is equal to %d", i1, i2));
            } else {
                i1++;
            }

            if (i1 != i2) {
                i2++;
            } else {
                System.out.println(String.format("%d is equal to %d", i1, i2));
            }
        }

        return 8;
    }
}
