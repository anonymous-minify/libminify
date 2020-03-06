import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonTest {


    @Test
    void testArrayCompare() {
        ComparisonHelper helper = new ComparisonHelper(false);
        int[] a1 = new int[]{5, 4, 3, 2, 1};
        int[] a2 = new int[]{5, 3, 9, 8, 6};

        assertFalse(helper.compareFirstArrayElements(a1, a2));
    }

    @Test
    void testIntCompare() {
        ComparisonHelper helper = new ComparisonHelper(false);
        int i1 = 4;
        int i2 = 5;

        assertFalse(helper.compareInts(i2, i1));
    }

    @Test
    void testLongCompare() {
        ComparisonHelper helper = new ComparisonHelper(false);
        long l1 = Long.MIN_VALUE;
        long l2 = Long.MAX_VALUE;

        assertTrue(helper.compareLongs(l1, l2));
    }

    @Test
    void testDoubleCompare() {
        ComparisonHelper helper = new ComparisonHelper(false);
        double d1 = 4.0;
        double d2 = 5.0;

        assertTrue(helper.compareDoubles(d1, d2));
    }

    @Test
    void testFloatCompare() {
        ComparisonHelper helper = new ComparisonHelper(false);
        float d1 = 4.4f;
        float d2 = 5.6f;

        assertTrue(helper.compareFloats(d1, d2));
    }
}
