class TestObject {
    private final int value;
    private final int[] array;

    TestObject(int value, int[] array) {
        this.value = value;
        this.array = array;
    }

    int getValue() {
        return value;
    }

    int[] getArray() {
        return array;
    }

    boolean compare(int i1, int i2) {
        int sum = value + i1 + i2;
        return sum > 5;
    }
}