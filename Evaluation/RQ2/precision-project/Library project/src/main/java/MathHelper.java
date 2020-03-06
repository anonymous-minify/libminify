class MathHelper {
    double divAbove1(int a, int b) {
        double diff = 0;
        if (a < b) {
            diff = b / a;
        } else {
            diff = a / b;
        }

        System.out.println("The result of the division is " + diff);
        return diff;
    }

    int addIfLower(int a, int b) {
        int result = 0;
        for (int i = 0; i < b; i++) {
            if (i < a) {
                result += 1;
            }
        }
        return result;
    }
}
