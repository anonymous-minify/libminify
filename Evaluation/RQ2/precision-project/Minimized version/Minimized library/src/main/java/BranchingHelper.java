public class BranchingHelper {
    public boolean ifTest(int i) {
        boolean result;
        System.out.println("i > 6");
        result = true;
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
        } else {
            result = 4;
        }

        return result;
    }

    public void ifNullTest(Object o) {
        System.out.println(o.toString());
    }

    public int forLoopTest(int x) {
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
