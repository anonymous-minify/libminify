import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchingTest {
    @Test
    void ifBranchingTest() {
        BranchingHelper c = new BranchingHelper();
        assertTrue(c.ifTest(8));
    }

    @Test
    void ifNullTest() {
        BranchingHelper c = new BranchingHelper();
        c.ifNullTest("");
    }

    // the multiple tests are used to simulate multiple execution paths
    @Test
    void ifMultipleBranchesTest1() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(2, c.ifMultiplePathsTest(15));
    }

    @Test
    void ifMultipleBranchesTest2() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(4, c.ifMultiplePathsTest(11));
    }

    @Test
    void ifMultipleBranchesTest3() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(0, c.ifMultiplePathsTest(1));
    }

    @Test
    void ifMultipleBranchesTest4() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(1, c.ifMultiplePathsTest(21));
    }

    @Test
    void forLoopTest() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(4, c.forLoopTest(5));
    }

    @Test
    void whileLoopTest() {
        BranchingHelper c = new BranchingHelper();
        assertEquals(6, c.whileLoopTest(0, 10));
    }
}
