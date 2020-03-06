import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchTest {
    @Test
    void testSwitchRemoval() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(0, helper.switchRemovalTest(42));
    }


    // the multiple switch tests are used to simulate multiple execution paths
    @Test
    void testSwitch1() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(8, helper.switchTest(35));
    }

    @Test
    void testSwitch2() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(8, helper.switchTest(-1));
    }

    @Test
    void testSwitch3() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(5, helper.switchTest(92));
    }

    @Test
    void testSwitch4() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(3, helper.switchTest(23));
    }

    @Test
    void testSwitch5() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(0, helper.switchTest(42));
    }

    @Test
    void testSwitchString() {
        SwitchHelper helper = new SwitchHelper();
        assertEquals(-100, helper.switchStringTest("a", 42));
    }
}
