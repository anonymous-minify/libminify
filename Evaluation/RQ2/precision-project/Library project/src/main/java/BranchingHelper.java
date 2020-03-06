public class BranchingHelper {
    public boolean ifTest(int i) {
        boolean result;
        if (i > 6) {
            System.out.println("i > 6");
            result = true;
        } else {
            System.out.println("i <= 6");
            result = false;
        }

        return result;
    }

    public int ifMultiplePathsTest(int i) {
        int result;
        if (i < 5) {
            System.out.println("i < 5");
            result = 0;
        } else if (i > 20) {
            System.out.println("i > 20");
            result = 1;
        } else if (i == 15) {
            System.out.println("i == 15");
            result = 2;
        } else if (i == 12) {
            System.out.println("i == 12");
            result = 3;
        } else {
            result = 4;
        }

        return result;
    }

    public void ifNullTest(Object o) {
        if (o == null) {
            System.out.println("Object should not be null");
            throw new IllegalArgumentException();
        }

        System.out.println(o.toString());
    }

    public int forLoopTest(int x) {
        for (int i = 0; i < x; i++) {
            if (i > 10) {
                return 5;
            }
        }

        return 4;
    }

    public int whileLoopTest(int x, int y) {
        int i = x;
        int j = y;
        while (i < 10) {
            i++;
            if (i < 5) {
                j--;
            }
        }

        return j;
    }
}
