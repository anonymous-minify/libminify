import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class MathTest {
    @Test
    void testAddIfLower(){
        MathHelper helper = new MathHelper();
        int result = helper.addIfLower(3,5);
        assertEquals(3,result);
    }
}
