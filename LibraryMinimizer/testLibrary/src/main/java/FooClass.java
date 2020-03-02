public class FooClass {
    public int a(int i) {
        if (i < 5) {
            int b = i + 5;
            if (i < 3) {
                return i + 9;
            }

            return b;
        } else {
            return i + 8;
        }
    }
}
