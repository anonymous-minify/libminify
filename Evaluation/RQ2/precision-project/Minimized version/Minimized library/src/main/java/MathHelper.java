class MathHelper {
    double divAbove1(int a, int b) {
        return (double) (a / b);
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
